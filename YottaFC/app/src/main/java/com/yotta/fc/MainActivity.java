package com.yotta.fc;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends Activity implements App.Host {
    App app;
    GameView view;
    Vibrator vib;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try { vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); } catch (Throwable t) { }
        app = new App(this);
        app.sfx.init(this);
        view = new GameView(this, app);
        setContentView(view);
        immersive();
    }

    void immersive() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.getClass().getField("layoutInDisplayCutoutMode").setInt(lp, 1);
                getWindow().setAttributes(lp);
            }
        } catch (Throwable t) { }
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) immersive(); }
    @Override protected void onPause() { super.onPause(); app.saveCareer(); app.sfx.stopAmbient(); if (app.screen == App.S_MATCH && !app.paused && app.match != null && !app.match.halfPending) { app.paused = true; app.pauseTab = 0; app.hud.releaseAll(app.match); } }
    @Override protected void onResume() { super.onResume(); if (app.screen == App.S_MATCH && app.match != null && !app.paused) app.sfx.startAmbient(); }
    @Override protected void onDestroy() { super.onDestroy(); app.saveCareer(); app.sfx.release(); }
    @Override public void onBackPressed() { app.onBack(); }

    // ---- Host
    @Override public void vibrate(int ms) { try { if (vib != null) vib.vibrate(ms); } catch (Throwable t) { } }
    @Override public void save(String name, String data) {
        try {
            File f = new File(getFilesDir(), name + ".json");
            File tmp = new File(getFilesDir(), name + ".tmp");
            FileOutputStream o = new FileOutputStream(tmp);
            o.write(data.getBytes("UTF-8"));
            o.close();
            tmp.renameTo(f);
        } catch (Throwable t) { }
    }
    @Override public String load(String name) {
        try {
            File f = new File(getFilesDir(), name + ".json");
            if (!f.exists()) return null;
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return bo.toString("UTF-8");
        } catch (Throwable t) { return null; }
    }
    @Override public void quit() { finish(); }
}
