package app.privacyscreen;

import android.widget.SeekBar;

class Watcher implements SeekBar.OnSeekBarChangeListener {
    private final MainActivity a;
    private final boolean isDim;

    Watcher(MainActivity a, boolean isDim) { this.a = a; this.isDim = isDim; }

    @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
        if (!user) return;
        if (isDim) a.prefs.setDim(v); else a.prefs.setBrightness(Math.max(1, v));
        a.push();
    }
    @Override public void onStartTrackingTouch(SeekBar s) { }
    @Override public void onStopTrackingTouch(SeekBar s) { }
}
