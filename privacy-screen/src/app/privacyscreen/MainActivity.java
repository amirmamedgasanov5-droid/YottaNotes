package app.privacyscreen;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    Prefs prefs;
    private TextView status;
    private Button permBtn, toggleBtn;
    private SeekBar dimBar, brightBar;
    private TextView dimLabel, brightLabel;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, p);

        TextView title = text("Privacy Screen", 26, true);
        root.addView(title);
        root.addView(text("Программный фильтр приватности: затемняет экран поверх всех "
                + "приложений и снижает яркость, чтобы текст было трудно прочитать "
                + "сбоку и сзади. Вам прямо перед экраном он остаётся читаемым.", 14, false));

        status = text("", 15, true);
        status.setPadding(0, dp(16), 0, dp(8));
        root.addView(status);

        permBtn = new Button(this);
        permBtn.setText("1. Разрешить «Поверх других приложений»");
        permBtn.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        root.addView(permBtn);

        toggleBtn = new Button(this);
        toggleBtn.setOnClickListener(v -> toggle());
        root.addView(toggleBtn);

        dimLabel = text("", 14, false);
        dimLabel.setPadding(0, dp(16), 0, 0);
        root.addView(dimLabel);
        dimBar = new SeekBar(this);
        dimBar.setMax(85);
        dimBar.setProgress(prefs.dim());
        dimBar.setOnSeekBarChangeListener(new Watcher(this, true));
        root.addView(dimBar);

        CheckBox useBr = new CheckBox(this);
        useBr.setText("Дополнительно снижать яркость экрана");
        useBr.setChecked(prefs.useBrightness());
        useBr.setOnCheckedChangeListener((v, on) -> { prefs.setUseBrightness(on); push(); });
        useBr.setPadding(0, dp(12), 0, 0);
        root.addView(useBr);

        brightLabel = text("", 14, false);
        brightLabel.setPadding(0, dp(12), 0, 0);
        root.addView(brightLabel);
        brightBar = new SeekBar(this);
        brightBar.setMax(100);
        brightBar.setProgress(prefs.brightness());
        brightBar.setOnSeekBarChangeListener(new Watcher(this, false));
        root.addView(brightBar);

        TextView note = text("Совет: добавьте плитку «Приватный экран» в шторку быстрых "
                + "настроек, чтобы включать фильтр одним касанием.\n\n"
                + "Честно: это не заменяет аппаратную защитную плёнку с узким углом "
                + "обзора, но заметно снижает читаемость для соседей.", 13, false);
        note.setPadding(0, dp(20), 0, 0);
        root.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void toggle() {
        if (!Settings.canDrawOverlays(this)) { refresh(); return; }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        if (prefs.enabled()) OverlayService.stop(this); else OverlayService.start(this);
        prefs.setEnabled(!prefs.enabled());
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        // Уведомление нужно только для foreground-сервиса; запускаем в любом случае.
        if (Settings.canDrawOverlays(this) && !prefs.enabled()) {
            OverlayService.start(this);
            prefs.setEnabled(true);
        }
        refresh();
    }

    /** Применить новые значения к уже включённому фильтру. */
    void push() {
        if (prefs.enabled()) OverlayService.start(this);
        refresh();
    }

    private void refresh() {
        boolean can = Settings.canDrawOverlays(this);
        boolean on = prefs.enabled() && can;
        permBtn.setVisibility(can ? View.GONE : View.VISIBLE);
        toggleBtn.setEnabled(can);
        toggleBtn.setText(on ? "Выключить приватный экран" : "2. Включить приватный экран");
        status.setText(!can ? "Нужно разрешение «Поверх других приложений»"
                : on ? "● Фильтр включён" : "○ Фильтр выключен");
        dimLabel.setText("Затемнение: " + prefs.dim() + "%");
        brightLabel.setText("Яркость экрана: " + prefs.brightness() + "%");
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.START);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
