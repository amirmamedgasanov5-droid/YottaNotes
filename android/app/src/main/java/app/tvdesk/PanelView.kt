package app.tvdesk

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Нижняя (или верхняя) панель: полная клавиатура слева, тачпад с кнопками справа. */
class PanelView(
    ctx: Context,
    private val host: Host,
    private val pad: TouchpadView.Listener,
    private val prefs: Prefs,
    screenW: Int,
) : LinearLayout(ctx) {

    interface Host {
        fun back(); fun home(); fun recents()
        fun minimize(); fun close(); fun toggleDock()
        fun openSettings(); fun screenshot(); fun pickKeyboard()
    }

    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * dm.density).toInt()
    private val handler = Handler(Looper.getMainLooper())

    private val status = TextView(ctx)
    private val refresh = object : Runnable {
        override fun run() { updateStatus(); handler.postDelayed(this, 1500) }
    }
    private val listener: () -> Unit = { updateStatus() }

    init {
        orientation = VERTICAL
        setBackgroundColor(0xF2101418.toInt())

        val kbW = (screenW - dp(16)) * 0.70f
        val unit = kbW / 16f
        val rowH = (unit * 0.92f * prefs.panelScale).coerceIn(dp(32).toFloat(), dp(72).toFloat()).toInt()
        val bodyH = KeyboardView.heightFor(rowH, dm.density)

        // верхняя полоска
        val bar = LinearLayout(ctx).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(6), 0)
        }
        status.apply {
            setTextColor(0xFFB6C2D0.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setSingleLine()
            setOnClickListener { if (!imeSelected()) host.pickKeyboard() }
        }
        bar.addView(status, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(barButton(if (prefs.dockTop) "Вниз" else "Вверх") { host.toggleDock() })
        bar.addView(barButton("Настройки") { host.openSettings() })
        bar.addView(barButton("Свернуть") { host.minimize() })
        bar.addView(barButton("Закрыть") { host.close() })
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, dp(36)))

        val body = LinearLayout(ctx).apply { orientation = HORIZONTAL }
        val kb = KeyboardView(ctx, rowH, prefs) { host.screenshot() }
        body.addView(kb, LayoutParams(0, bodyH, 7f))

        val right = LinearLayout(ctx).apply {
            orientation = VERTICAL
            setPadding(dp(6), dp(5), dp(8), dp(5))
        }
        val touch = TouchpadView(ctx, pad)
        right.addView(touch, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val btnRow = LinearLayout(ctx).apply { orientation = HORIZONTAL }
        val left = padButton("ЛКМ").apply {
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { v.isPressed = true; pad.onDragStart() }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; pad.onDragEnd() }
                }
                true
            }
        }
        val rightBtn = padButton("ПКМ").apply {
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.isPressed = true
                    MotionEvent.ACTION_UP -> { v.isPressed = false; pad.onLongPress() }
                    MotionEvent.ACTION_CANCEL -> v.isPressed = false
                }
                true
            }
        }
        btnRow.addView(left, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(0, 0, dp(3), 0) })
        btnRow.addView(rightBtn, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(3), 0, 0, 0) })
        right.addView(btnRow, LayoutParams(LayoutParams.MATCH_PARENT, (rowH * 1.0f).toInt()).apply { topMargin = dp(6) })

        val nav = LinearLayout(ctx).apply { orientation = HORIZONTAL }
        nav.addView(navButton("Назад") { host.back() }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(0, 0, dp(3), 0) })
        nav.addView(navButton("Домой") { host.home() }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        nav.addView(navButton("Задачи") { host.recents() }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(3), 0, 0, 0) })
        right.addView(nav, LayoutParams(LayoutParams.MATCH_PARENT, (rowH * 0.8f).toInt()).apply { topMargin = dp(6) })

        body.addView(right, LayoutParams(0, bodyH, 3f))
        addView(body, LayoutParams(LayoutParams.MATCH_PARENT, bodyH))
        updateStatus()
    }

    private fun roundBg(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(8).toFloat() }

    private fun btnDrawable(normal: Int) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), roundBg(0xFF4C8DFF.toInt()))
        addState(intArrayOf(), roundBg(normal))
    }

    private fun barButton(t: String, onClick: () -> Unit) = TextView(context).apply {
        text = t; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), 0)
        background = btnDrawable(0x00000000)
        setOnClickListener { onClick() }
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    }

    private fun padButton(t: String) = TextView(context).apply {
        text = t; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER; background = btnDrawable(0xFF2B323C.toInt())
    }

    private fun navButton(t: String, onClick: () -> Unit) = TextView(context).apply {
        text = t; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER; background = btnDrawable(0xFF1C2229.toInt())
        setOnClickListener { onClick() }
    }

    private fun imeSelected(): Boolean {
        val cur = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        return cur.startsWith(context.packageName + "/")
    }

    private fun updateStatus() {
        when {
            !imeSelected() -> { status.text = "Клавиатура TV Desk не выбрана — нажмите здесь"; status.setTextColor(0xFFFFB454.toInt()) }
            AppState.imeHasInput -> { status.text = "● Поле ввода активно — печатайте"; status.setTextColor(0xFF6EE7A0.toInt()) }
            else -> { status.text = "○ Нет поля ввода — наведите курсор на поле и нажмите"; status.setTextColor(0xFFB6C2D0.toInt()) }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        AppState.addListener(listener)
        handler.post(refresh)
    }

    override fun onDetachedFromWindow() {
        AppState.removeListener(listener)
        handler.removeCallbacks(refresh)
        super.onDetachedFromWindow()
    }
}
