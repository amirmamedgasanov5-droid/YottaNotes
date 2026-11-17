package app.privacyscreen;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getSharedPreferences("privacy", Context.MODE_PRIVATE);
    }

    boolean enabled() { return sp.getBoolean("enabled", false); }
    void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }

    /** Затемнение поверх экрана, 0..85 (%) */
    int dim() { return sp.getInt("dim", 30); }
    void setDim(int v) { sp.edit().putInt("dim", v).apply(); }

    boolean useBrightness() { return sp.getBoolean("useBr", false); }
    void setUseBrightness(boolean v) { sp.edit().putBoolean("useBr", v).apply(); }

    /** Яркость экрана, 1..100 (%). Чем ниже, тем хуже видно сбоку. */
    int brightness() { return sp.getInt("brightness", 40); }
    void setBrightness(int v) { sp.edit().putInt("brightness", v).apply(); }
}
