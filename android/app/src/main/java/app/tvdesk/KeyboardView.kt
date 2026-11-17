package app.tvdesk

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Полная клавиатура как у ноутбука: F1–F12, цифры, Tab, Caps, Shift, Ctrl, Win, Alt, стрелки,
 * Home/End/PgUp/PgDn, переключение EN/RU. Shift/Ctrl/Alt/Win — «липкие» (нажал и отпустил = вкл
 * на одно следующее нажатие) и работают как зажатые, если держать пальцем и жать другую клавишу.
 */
class KeyboardView(
    ctx: Context,
    private val rowH: Int,
    private val prefs: Prefs,
    private val onShot: () -> Unit = {},
) : LinearLayout(ctx) {

    private val handler = Handler(Looper.getMainLooper())
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val held = HashSet<KType>()
    private val latched = HashSet<KType>()
    private val usedWhileHeld = HashSet<KType>()
    private var caps = false
    private var lang = prefs.lang

    private class Slot(val def: KeyDef, val view: TextView)
    private val slots = ArrayList<Slot>()

    private val dp = ctx.resources.displayMetrics.density
    private fun dp(v: Int) = (v * dp).toInt()

    companion object {
        const val ROWS = 6
        fun heightFor(rowH: Int, dp: Float) = rowH * ROWS + (6 * dp).toInt()
    }

    init {
        orientation = VERTICAL
        setPadding(dp(3), dp(3), dp(3), dp(3))
        for (row in KeyDefs.rows) {
            val rl = LinearLayout(ctx).apply { orientation = HORIZONTAL }
            for (def in row) {
                if (def.type == KType.UPDOWN) rl.addView(updownCell(), cellLp(def.weight))
                else rl.addView(makeKey(def, 0), cellLp(def.weight))
            }
            addView(rl, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
        }
        relabel()
    }

    private fun cellLp(w: Float) = LayoutParams(0, LayoutParams.MATCH_PARENT, w).apply {
        setMargins(dp(2), dp(2), dp(2), dp(2))
    }

    private fun updownCell(): LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
        addView(makeKey(KeyDef("↑", KeyEvent.KEYCODE_DPAD_UP), 0), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply { bottomMargin = dp(1) })
        addView(makeKey(KeyDef("↓", KeyEvent.KEYCODE_DPAD_DOWN), 0), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(1) })
    }

    private fun bg(color: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(6).toFloat()
    }

    private fun makeKey(def: KeyDef, unused: Int): TextView {
        val tv = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, rowH * if (def.label.length > 3 || def.type == KType.CAPS) 0.26f else 0.36f)
            setSingleLine()
            isClickable = false
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), bg(0xFF4C8DFF.toInt()))
                addState(intArrayOf(android.R.attr.state_selected), bg(0xFF2E6BD6.toInt()))
                addState(intArrayOf(), bg(if (def.type == KType.CHAR || def.type == KType.SPACE) 0xFF2B323C.toInt() else 0xFF1C2229.toInt()))
            }
            text = def.label
        }
        tv.setOnTouchListener(KeyTouch(def, tv))
        slots.add(Slot(def, tv))
        return tv
    }

    // ---------- состояние модификаторов ----------

    private fun eff(t: KType) = held.contains(t) || latched.contains(t)

    private fun mods(): Int {
        var m = 0
        if (eff(KType.SHIFT)) m = m or KeyOutput.SHIFT
        if (eff(KType.CTRL)) m = m or KeyOutput.CTRL
        if (eff(KType.ALT)) m = m or KeyOutput.ALT
        if (eff(KType.WIN)) m = m or KeyOutput.META
        return m
    }

    private fun relabel() {
        val shift = eff(KType.SHIFT)
        for (s in slots) {
            val d = s.def
            when (d.type) {
                KType.CHAR -> {
                    val base = if (lang == 0) d.en else d.ru
                    val sh = if (lang == 0) d.enS else d.ruS
                    s.view.text = if (d.letter) { if (shift != caps) sh else base } else { if (shift) sh else base }
                }
                KType.LANG -> s.view.text = if (lang == 0) "EN" else "RU"
                else -> {}
            }
            val active = when (d.type) {
                KType.CAPS -> caps
                KType.SHIFT, KType.CTRL, KType.ALT, KType.WIN -> eff(d.type)
                else -> false
            }
            s.view.isSelected = active
        }
    }

    private fun feedback(v: View) {
        if (prefs.haptics) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (prefs.keySound) KeyClick.play(audio)
    }

    // ---------- нажатие ----------

    private fun fire(d: KeyDef, repeat: Int) {
        val m = mods()
        val combo = m and (KeyOutput.CTRL or KeyOutput.ALT or KeyOutput.META) != 0
        when (d.type) {
            KType.CHAR -> {
                if (combo) KeyOutput.key(d.code, m, repeat)
                else {
                    val shift = m and KeyOutput.SHIFT != 0
                    val base = if (lang == 0) d.en else d.ru
                    val sh = if (lang == 0) d.enS else d.ruS
                    val txt = if (d.letter) { if (shift != caps) sh else base } else { if (shift) sh else base }
                    KeyOutput.text(txt)
                }
            }
            KType.SPACE -> if (combo) KeyOutput.key(KeyEvent.KEYCODE_SPACE, m, repeat) else KeyOutput.text(" ")
            KType.FN -> KeyOutput.key(d.code, m, repeat)
            KType.SHOT -> onShot()
            else -> {}
        }
    }

    private inner class KeyTouch(val d: KeyDef, val v: TextView) : OnTouchListener {
        private var count = 0
        private val repeater = object : Runnable {
            override fun run() {
                count++
                fire(d, count)
                handler.postDelayed(this, 45)
            }
        }

        override fun onTouch(view: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    feedback(v)
                    when {
                        d.isMod -> {
                            held.add(d.type); usedWhileHeld.remove(d.type)
                        }
                        d.type == KType.CAPS -> caps = !caps
                        d.type == KType.LANG -> {
                            lang = 1 - lang; prefs.lang = lang
                        }
                        else -> {
                            fire(d, 0)
                            // модификаторы «использованы»: отпустим липкие
                            usedWhileHeld.addAll(held)
                            latched.clear()
                            if (d.repeatable) { count = 0; handler.postDelayed(repeater, 420) }
                        }
                    }
                    relabel()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(repeater)
                    if (d.isMod) {
                        held.remove(d.type)
                        if (e.actionMasked == MotionEvent.ACTION_UP && !usedWhileHeld.contains(d.type)) {
                            if (!latched.add(d.type)) latched.remove(d.type)
                        }
                        usedWhileHeld.remove(d.type)
                    }
                    relabel()
                }
            }
            return true
        }
    }
}
