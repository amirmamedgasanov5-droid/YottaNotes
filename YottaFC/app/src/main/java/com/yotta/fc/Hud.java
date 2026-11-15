package com.yotta.fc;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** Match HUD and touch controls (virtual 1080-high coordinates). */
final class Hud {
    final Ui ui;
    int vw, vh;
    float scale = 1f, opacity = 1f;
    boolean showRadar = true;
    // stick
    int stickPtr = -1; float sbx, sby, scx, scy; static final float SR = 120f;
    // buttons: 0 shoot 1 pass 2 lob 3 through 4 sprint
    final float[] bx = new float[5], by = new float[5], br = new float[5];
    final int[] bptr = {-1, -1, -1, -1, -1};
    boolean pausePressed;
    float pbx, pby, pbr = 46;
    int pausePtr = -1;
    float[] btnPulse = new float[5];
    final RectF rc = new RectF();

    Hud(Ui ui) { this.ui = ui; }

    void layout(int w, int h) {
        vw = w; vh = h;
        float s = scale;
        float rx = vw - 120 * s, ry = vh - 150 * s;
        bx[0] = rx - 40 * s; by[0] = ry; br[0] = 92 * s;
        bx[1] = rx - 220 * s; by[1] = ry + 38 * s; br[1] = 76 * s;
        bx[2] = rx - 245 * s; by[2] = ry - 170 * s; br[2] = 70 * s;
        bx[3] = rx - 55 * s; by[3] = ry - 205 * s; br[3] = 70 * s;
        bx[4] = rx - 400 * s; by[4] = ry + 70 * s; br[4] = 68 * s;
        pbx = vw - 70; pby = 70;
        if (stickPtr < 0) { sbx = 240 * s; sby = vh - 220 * s; scx = sbx; scy = sby; }
    }

    boolean down(int ptr, float x, float y, Match m) {
        for (int i = 0; i < 5; i++) {
            if (bptr[i] == -1 && U.dist(x, y, bx[i], by[i]) < br[i] * 1.12f) {
                bptr[i] = ptr; btnPulse[i] = 1f;
                Input in = m.in;
                switch (i) {
                    case 0: in.shoot = true; in.shootP = true; break;
                    case 1: in.pass = true; in.passP = true; break;
                    case 2: in.lob = true; in.lobP = true; break;
                    case 3: in.through = true; in.throughP = true; break;
                    default: in.sprint = true;
                }
                return true;
            }
        }
        if (U.dist(x, y, pbx, pby) < pbr * 1.3f) { pausePtr = ptr; return true; }
        if (x < vw * 0.46f && stickPtr == -1) {
            stickPtr = ptr; sbx = x; sby = y; scx = x; scy = y; update(m); return true;
        }
        return false;
    }

    void move(int ptr, float x, float y, Match m) {
        if (ptr == stickPtr) { scx = x; scy = y; update(m); }
    }

    boolean up(int ptr, float x, float y, Match m) {
        for (int i = 0; i < 5; i++) {
            if (bptr[i] == ptr) {
                bptr[i] = -1;
                Input in = m.in;
                switch (i) {
                    case 0: in.shoot = false; in.shootR = true; break;
                    case 1: in.pass = false; in.passR = true; break;
                    case 2: in.lob = false; in.lobR = true; break;
                    case 3: in.through = false; in.throughR = true; break;
                    default: in.sprint = false;
                }
                return false;
            }
        }
        if (ptr == pausePtr) { pausePtr = -1; if (U.dist(x, y, pbx, pby) < pbr * 1.6f) return true; return false; }
        if (ptr == stickPtr) {
            stickPtr = -1; m.in.sx = 0; m.in.sy = 0;
            sbx = 240 * scale; sby = vh - 220 * scale; scx = sbx; scy = sby;
        }
        return false;
    }

    void releaseAll(Match m) {
        stickPtr = -1; pausePtr = -1;
        for (int i = 0; i < 5; i++) bptr[i] = -1;
        m.in.sx = m.in.sy = 0; m.in.sprint = m.in.shoot = m.in.pass = m.in.lob = m.in.through = false;
        sbx = 240 * scale; sby = vh - 220 * scale; scx = sbx; scy = sby;
    }

    void update(Match m) {
        float dx = scx - sbx, dy = scy - sby, d = U.len(dx, dy);
        float r = SR * scale;
        if (d > r) { dx = dx / d * r; dy = dy / d * r; scx = sbx + dx; scy = sby + dy; d = r; }
        m.in.sx = dx / r; m.in.sy = dy / r;
    }

    String[] labels(Match m) {
        Match.Side hs = m.S[m.humanSide];
        if (m.mode == Match.M_SET && m.spHuman) {
            switch (m.spType) {
                case Match.SP_PENALTY: return new String[]{"УДАР", "", "", ""};
                case Match.SP_FREEKICK: return new String[]{"УДАР", "ПАС", "НАВЕС", "ПРОРЕЗ"};
                case Match.SP_CORNER: return new String[]{"ПОДАЧА", "КОРОТК.", "НАВЕС", ""};
                case Match.SP_THROW: return new String[]{"ВБРОС", "ВБРОС", "ВБРОС", "ВБРОС"};
                default: return new String[]{"ДАЛЬНИЙ", "КОРОТК.", "ДАЛЬНИЙ", "ДАЛЬНИЙ"};
            }
        }
        if (m.mode == Match.M_KICKOFF) return new String[]{"СТАРТ", "СТАРТ", "СТАРТ", "СТАРТ"};
        boolean att = m.owner != null && m.owner.side == m.humanSide;
        if (att) {
            if (m.owner.role == Player.GK) return new String[]{"ДАЛЬНИЙ", "КОРОТК.", "ДАЛЬНИЙ", "ДАЛЬНИЙ"};
            return new String[]{"УДАР", "ПАС", "НАВЕС", "ПРОРЕЗ"};
        }
        if (m.owner != null) return new String[]{"ОТБОР", "СМЕНА", "ПОДКАТ", "ПРЕСС"};
        return new String[]{"СМЕНА", "СМЕНА", "ПОДКАТ", "ПРЕСС"};
    }

    // =============================================================== draw
    void draw(Canvas c, Match m, float dt, Pawn passPreview) {
        for (int i = 0; i < 5; i++) btnPulse[i] = Math.max(0, btnPulse[i] - dt * 4);
        scoreboard(c, m);
        pauseButton(c);
        sticks(c, m);
        buttons(c, m);
        if (showRadar) radar(c, m);
        banner(c, m);
        commentary(c, m);
        playerTag(c, m);
        hints(c, m);
    }

    void scoreboard(Canvas c, Match m) {
        float w = 780, h = 92, x = vw / 2f - w / 2, y = 18;
        ui.glass(x, y, w, h, 24);
        Match.Side a = m.S[0], b = m.S[1];
        // colour tabs
        ui.fill.setColor(a.c1); ui.rc.set(x + 14, y + 16, x + 24, y + h - 16); c.drawRoundRect(ui.rc, 5, 5, ui.fill);
        ui.fill.setColor(b.c1); ui.rc.set(x + w - 24, y + 16, x + w - 14, y + h - 16); c.drawRoundRect(ui.rc, 5, 5, ui.fill);
        ui.text(a.abbr, x + 130, y + 60, 42, Ui.WHITE, 1);
        ui.text(b.abbr, x + w - 130, y + 60, 42, Ui.WHITE, 1);
        ui.text(String.valueOf(a.score), x + 270, y + 68, 62, Ui.GOLD, 1);
        ui.text(String.valueOf(b.score), x + w - 270, y + 68, 62, Ui.GOLD, 1);
        ui.text(m.clockText(), x + w / 2, y + 48, 38, Ui.WHITE, 1);
        String per = m.mode == Match.M_PEN ? "ПЕНАЛЬТИ" : (m.period == 0 ? "1 ТАЙМ" : m.period == 1 ? "2 ТАЙМ" : m.period == 2 ? "ДОП 1" : "ДОП 2");
        ui.text(per, x + w / 2, y + 76, 20, Ui.GRAY, 1);
        if (m.mode == Match.M_PEN) {
            ui.text(a.penScore + " : " + b.penScore, x + w / 2, y + h + 34, 34, Ui.WHITE, 1);
            for (int s = 0; s < 2; s++) {
                Match.Side sd = m.S[s];
                for (int k = 0; k < Math.max(5, sd.penTaken); k++) {
                    float px = (s == 0 ? x + 20 : x + w - 20 - Math.max(5, sd.penTaken) * 30) + k * 30, py = y + h + 14;
                    ui.fill.setColor(k < sd.penTaken ? (sd.penRes[Math.min(11, k)] ? Ui.GREEN : Ui.RED) : 0x55FFFFFF);
                    c.drawCircle(px + 12, py + 12, 10, ui.fill);
                }
            }
        }
    }

    void pauseButton(Canvas c) {
        ui.fill.setColor(0x88000000);
        c.drawCircle(pbx, pby, pbr, ui.fill);
        ui.stroke.setColor(0x66FFFFFF); ui.stroke.setStrokeWidth(2); c.drawCircle(pbx, pby, pbr, ui.stroke);
        ui.fill.setColor(Ui.WHITE);
        c.drawRect(pbx - 13, pby - 16, pbx - 4, pby + 16, ui.fill);
        c.drawRect(pbx + 4, pby - 16, pbx + 13, pby + 16, ui.fill);
    }

    void sticks(Canvas c, Match m) {
        float r = SR * scale;
        int a = (int) (110 * opacity);
        ui.fill.setShader(new RadialGradient(sbx, sby, r * 1.2f, U.alpha(0xFFFFFFFF, (int) (40 * opacity)), U.alpha(0xFFFFFFFF, (int) (12 * opacity)), Shader.TileMode.CLAMP));
        c.drawCircle(sbx, sby, r * 1.15f, ui.fill);
        ui.fill.setShader(null);
        ui.stroke.setColor(U.alpha(0xFFFFFFFF, a)); ui.stroke.setStrokeWidth(3);
        c.drawCircle(sbx, sby, r, ui.stroke);
        ui.fill.setShader(new RadialGradient(scx - 12, scy - 14, r * 0.7f, U.alpha(0xFFFFFFFF, (int) (230 * opacity)), U.alpha(0xFF9AA4B8, (int) (180 * opacity)), Shader.TileMode.CLAMP));
        c.drawCircle(scx, scy, r * 0.46f, ui.fill);
        ui.fill.setShader(null);
        // stamina of the controlled player
        Pawn p = m.S[m.humanSide].ctl;
        if (p != null) {
            float sx = sbx - r, sy = sby + r + 26;
            ui.bar(sx, sy, r * 2, 10, p.stamina, p.stamina > 0.5f ? Ui.GREEN : (p.stamina > 0.3f ? Ui.ORANGE : Ui.RED));
        }
    }

    void buttons(Canvas c, Match m) {
        String[] lab = labels(m);
        int[] cols = {0xFFE5484D, 0xFF2AA3F0, 0xFFF5A524, 0xFF2AD16C};
        float held = m.in.shootHeld;
        for (int i = 0; i < 5; i++) {
            float r = br[i] * (1 + btnPulse[i] * 0.12f) * (bptr[i] != -1 ? 0.93f : 1f);
            int col = i < 4 ? cols[i] : 0xFF8A3FFC;
            String l = i < 4 ? lab[i] : "БЕГ";
            if (i < 4 && l.length() == 0) continue;
            ui.fill.setShader(new RadialGradient(bx[i] - r * 0.25f, by[i] - r * 0.3f, r * 1.3f, U.alpha(U.mix(col, 0xFFFFFFFF, 0.35f), (int) (235 * opacity)), U.alpha(U.shade(col, 0.55f), (int) (235 * opacity)), Shader.TileMode.CLAMP));
            c.drawCircle(bx[i], by[i], r, ui.fill);
            ui.fill.setShader(null);
            ui.stroke.setStrokeWidth(4); ui.stroke.setColor(U.alpha(0xFFFFFFFF, (int) (150 * opacity)));
            c.drawCircle(bx[i], by[i], r, ui.stroke);
            float ts = l.length() > 6 ? r * 0.34f : (l.length() > 4 ? r * 0.4f : r * 0.46f);
            ui.text(l, bx[i], by[i] + ts * 0.35f, ts, Ui.WHITE, 1);
            if (i == 0 && bptr[0] != -1 && held > 0.02f) {
                ui.stroke.setColor(Ui.GOLD); ui.stroke.setStrokeWidth(10);
                rc.set(bx[0] - r - 12, by[0] - r - 12, bx[0] + r + 12, by[0] + r + 12);
                c.drawArc(rc, -90, 360f * U.clamp(held / 0.7f, 0, 1), false, ui.stroke);
            }
        }
    }

    void radar(Canvas c, Match m) {
        float w = 250, h = 160, x = vw / 2f - w / 2, y = vh - h - 18;
        ui.fill.setColor(0x66000000);
        rc.set(x, y, x + w, y + h);
        c.drawRoundRect(rc, 14, 14, ui.fill);
        ui.stroke.setColor(0x44FFFFFF); ui.stroke.setStrokeWidth(1.5f);
        float px = x + 10, py = y + 10, pw = w - 20, ph = h - 20;
        rc.set(px, py, px + pw, py + ph);
        c.drawRect(rc, ui.stroke);
        c.drawLine(px + pw / 2, py, px + pw / 2, py + ph, ui.stroke);
        c.drawCircle(px + pw / 2, py + ph / 2, ph * 0.14f, ui.stroke);
        for (Pawn p : m.all) {
            if (p.st == Pawn.SENT) continue;
            ui.fill.setColor(m.S[p.side].c1);
            float dx = px + p.x / Match.L * pw, dy = py + p.y / Match.W * ph;
            c.drawCircle(dx, dy, p == m.S[m.humanSide].ctl ? 6.5f : 4.5f, ui.fill);
            if (p == m.S[m.humanSide].ctl) { ui.stroke.setColor(Ui.GOLD); ui.stroke.setStrokeWidth(2); c.drawCircle(dx, dy, 8, ui.stroke); ui.stroke.setColor(0x44FFFFFF); ui.stroke.setStrokeWidth(1.5f); }
        }
        ui.fill.setColor(0xFFFFFFFF);
        c.drawCircle(px + m.bx / Match.L * pw, py + m.by / Match.W * ph, 4f, ui.fill);
    }

    void banner(Canvas c, Match m) {
        if (m.bannerT <= 0 || m.banner.length() == 0) return;
        float k = Math.min(1f, m.bannerT * 3f) * Math.min(1f, (4f - Math.min(4f, m.bannerT * 0 + 0f)));
        float alpha = U.clamp(m.bannerT * 2.5f, 0, 1);
        int col = m.bannerKind == 1 ? Ui.GOLD : (m.bannerKind == 2 ? 0xFFFFD400 : (m.bannerKind == 3 ? Ui.RED : (m.bannerKind == 4 ? Ui.CYAN : Ui.WHITE)));
        float size = m.bannerKind == 1 ? 96 : 56;
        float y = 270;
        if (m.bannerKind == 1) {
            float w = Math.min(vw - 60, ui.textW(m.banner, size) + 160);
            ui.fill.setShader(new LinearGradient(vw / 2f - w / 2, 0, vw / 2f + w / 2, 0, 0x00000000, 0xAA000000, Shader.TileMode.CLAMP));
            ui.fill.setShader(null);
            ui.fill.setColor(U.alpha(0xFF000000, (int) (150 * alpha)));
            rc.set(vw / 2f - w / 2, y - size * 0.95f, vw / 2f + w / 2, y + size * 0.35f);
            c.drawRoundRect(rc, 30, 30, ui.fill);
        } else {
            float w = ui.textW(m.banner, size) + 80;
            ui.fill.setColor(U.alpha(0xFF000000, (int) (140 * alpha)));
            rc.set(vw / 2f - w / 2, y - size * 0.95f, vw / 2f + w / 2, y + size * 0.3f);
            c.drawRoundRect(rc, 22, 22, ui.fill);
        }
        ui.text(m.banner, vw / 2f, y, size, U.alpha(col, (int) (255 * alpha)), 1);
    }

    void commentary(Canvas c, Match m) {
        if (m.commT <= 0 || m.commentary.length() == 0) return;
        float a = U.clamp(m.commT, 0, 1);
        float w = Math.min(vw * 0.6f, ui.textW(m.commentary, 30) + 70);
        float x = vw / 2f - w / 2, y = vh - 210;
        ui.fill.setColor(U.alpha(0xFF000000, (int) (140 * a)));
        rc.set(x, y, x + w, y + 46);
        c.drawRoundRect(rc, 23, 23, ui.fill);
        ui.text(m.commentary, vw / 2f, y + 33, 28, U.alpha(0xFFFFFFFF, (int) (255 * a)), 1, false);
    }

    void playerTag(Canvas c, Match m) {
        Pawn p = m.S[m.humanSide].ctl;
        if (p == null) return;
        float x = 24, y = 24;
        ui.glass(x, y, 380, 76, 18);
        ui.fill.setColor(m.S[m.humanSide].c1);
        rc.set(x + 12, y + 12, x + 24, y + 64);
        c.drawRoundRect(rc, 5, 5, ui.fill);
        ui.text(String.valueOf(p.pl.number), x + 60, y + 52, 38, Ui.GOLD, 1);
        ui.textFit(p.pl.last.toUpperCase(), x + 92, y + 36, 270, 28, Ui.WHITE, 0);
        ui.text(Player.POS[p.role] + " • " + p.pl.ovr(), x + 92, y + 62, 20, Ui.GRAY, 0);
    }

    void hints(Canvas c, Match m) {
        if (m.mode != Match.M_SET || !m.spHuman || m.autoPlay) return;
        String s;
        switch (m.spType) {
            case Match.SP_PENALTY: s = "ПЕНАЛЬТИ: стик — угол, УДАР (держи) — сила"; break;
            case Match.SP_FREEKICK: s = "ШТРАФНОЙ: стик — прицел, УДАР — по воротам, ПАС/НАВЕС — передача"; break;
            case Match.SP_CORNER: s = "УГЛОВОЙ: стик — куда подавать, ПОДАЧА — навес"; break;
            case Match.SP_THROW: s = "ВБРАСЫВАНИЕ: стик — кому, любая кнопка"; break;
            case Match.SP_GOALKICK: s = "АУТ: КОРОТК. — пас, ДАЛЬНИЙ — вынос вперёд"; break;
            default: return;
        }
        float w = ui.textW(s, 28) + 60;
        ui.fill.setColor(0xAA000000);
        rc.set(vw / 2f - w / 2, 126, vw / 2f + w / 2, 176);
        c.drawRoundRect(rc, 25, 25, ui.fill);
        ui.text(s, vw / 2f, 160, 28, Ui.GOLD, 1);
    }
}
