package app.privacyscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

public class OverlayService extends Service {
    static final String ACTION_STOP = "app.privacyscreen.STOP";
    private static final String CHANNEL = "privacy";

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    static void start(Context c) {
        Intent i = new Intent(c, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, OverlayService.class));
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Prefs prefs = new Prefs(this);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            prefs.setEnabled(false);
            PrivacyTileService.refresh(this);
            stopSelf();
            return START_NOT_STICKY;
        }

        goForeground();

        if (!Settings.canDrawOverlays(this)) {
            prefs.setEnabled(false);
            stopSelf();
            return START_NOT_STICKY;
        }

        prefs.setEnabled(true);
        showOrUpdate(prefs);
        PrivacyTileService.refresh(this);
        return START_STICKY;
    }

    private void showOrUpdate(Prefs prefs) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int alpha = Math.round(255f * prefs.dim() / 100f);
        float brightness = Math.max(0.01f, prefs.brightness() / 100f);

        if (overlay == null) {
            overlay = new View(this);
            lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            if (Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            overlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
            lp.screenBrightness = brightness;
            wm.addView(overlay, lp);
        } else {
            overlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
            lp.screenBrightness = brightness;
            wm.updateViewLayout(overlay, lp);
        }
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Privacy Screen", NotificationManager.IMPORTANCE_LOW));
        }
        int piFlags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), piFlags);
        Intent stopIntent = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 1, stopIntent, piFlags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Приватный экран включён")
                .setContentText("Нажмите, чтобы настроить")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Выключить", stop).build());

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, b.build(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, b.build());
        }
    }

    @Override
    public void onDestroy() {
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) { }
            overlay = null;
        }
        new Prefs(this).setEnabled(false);
        PrivacyTileService.refresh(this);
        super.onDestroy();
    }
}
