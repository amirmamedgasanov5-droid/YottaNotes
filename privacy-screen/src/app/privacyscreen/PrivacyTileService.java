package app.privacyscreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Плитка в шторке быстрых настроек: вкл/выкл одним нажатием. */
public class PrivacyTileService extends TileService {

    static void refresh(Context c) {
        try {
            TileService.requestListeningState(c, new ComponentName(c, PrivacyTileService.class));
        } catch (Exception ignored) { }
    }

    @Override public void onStartListening() { update(); }

    @Override
    public void onClick() {
        Prefs prefs = new Prefs(this);
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(i);
            return;
        }
        if (prefs.enabled()) OverlayService.stop(this); else OverlayService.start(this);
        prefs.setEnabled(!prefs.enabled());
        update();
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        t.setLabel("Приватный экран");
        t.setState(new Prefs(this).enabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }
}
