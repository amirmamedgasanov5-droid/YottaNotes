package com.yotta.fc;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.HashMap;

/** Tiny immediate-mode UI toolkit on top of Canvas. */
final class Ui {
    interface Listener { void onAction(int id, int arg); }

    static final int BG0 = 0xFF080C1A, BG1 = 0xFF1A1040, PANEL = 0xE61A2036, PANEL2 = 0xFF242C47, ACCENT = 0xFF8A3FFC, CYAN = 0xFF22D3EE,
            GOLD = 0xFFFFD54A, GREEN = 0xFF22C55E, RED = 0xFFEF4444, GRAY = 0xFF9AA4B8, WHITE = 0xFFFFFFFF, DARK = 0xFF10152A, ORANGE = 0xFFFB923C;

    static final class Hit { RectF r = new RectF(); int id, arg; }
    static final class Scroll { RectF r = new RectF(); float content; int key; }

    Canvas c;
    int vw, vh;
    Listener listener;
    float time;
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final RectF rc = new RectF();
    ArrayList<Hit> hits = new ArrayList<Hit>(), prevHits = new ArrayList<Hit>();
    ArrayList<Scroll> scrolls = new ArrayList<Scroll>(), prevScrolls = new ArrayList<Scroll>();
    HashMap<Integer, Float> offs = new HashMap<Integer, Float>();
    HashMap<Integer, Float> vels = new HashMap<Integer, Float>();
    // touch
    boolean down, dragged;
    float dX, dY, lY;
    Hit downHit; Scroll dragScroll;
    // current scroll
    float curOff; RectF curArea;
    boolean inScroll;
    boolean modal;           // when true only hits registered after modalBegin() count
    int modalStart;
    Typeface bold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    Typeface norm = Typeface.create("sans-serif-condensed", Typeface.NORMAL);

    Ui() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    void begin(Canvas canvas, int w, int h, float t, float dt) {
        c = canvas; vw = w; vh = h; time = t;
        ArrayList<Hit> tmp = prevHits; prevHits = hits; hits = tmp; hits.clear();
        ArrayList<Scroll> tm2 = prevScrolls; prevScrolls = scrolls; scrolls = tm2; scrolls.clear();
        // momentum
        if (!down) {
            for (java.util.Map.Entry<Integer, Float> e : vels.entrySet()) {
                float v = e.getValue();
                if (Math.abs(v) > 5) {
                    float o = offs.containsKey(e.getKey()) ? offs.get(e.getKey()) : 0;
                    offs.put(e.getKey(), o + v * dt);
                    e.setValue(v * (float) Math.exp(-4f * dt));
                } else e.setValue(0f);
            }
        }
        modal = false;
    }

    // ------------------------------------------------------------- touch
    void touch(int action, float x, float y) {
        if (action == 0) { // down
            down = true; dragged = false; dX = x; dY = y; lY = y;
            downHit = findHit(x, y);
            dragScroll = null;
            for (int i = prevScrolls.size() - 1; i >= 0; i--) { Scroll s = prevScrolls.get(i); if (s.r.contains(x, y)) { dragScroll = s; break; } }
            if (dragScroll != null) vels.put(dragScroll.key, 0f);
        } else if (action == 2) { // move
            if (!down) return;
            if (!dragged && Math.abs(y - dY) > 14 && dragScroll != null) dragged = true;
            if (dragged && dragScroll != null) {
                float o = offs.containsKey(dragScroll.key) ? offs.get(dragScroll.key) : 0;
                float dy = lY - y;
                offs.put(dragScroll.key, o + dy);
                vels.put(dragScroll.key, dy * 55f);
            }
            lY = y;
        } else { // up
            if (down && !dragged && downHit != null) {
                Hit h = findHit(x, y);
                if (h != null && h.id == downHit.id && h.arg == downHit.arg && listener != null) listener.onAction(h.id, h.arg);
            }
            down = false; downHit = null;
        }
    }

    Hit findHit(float x, float y) {
        for (int i = prevHits.size() - 1; i >= 0; i--) { Hit h = prevHits.get(i); if (h.r.contains(x, y)) return h; }
        return null;
    }

    boolean pressed(RectF r) { return down && downHit != null && !dragged && r.contains(dX, dY) && r.contains(dX, dY); }

    void hit(float l, float t, float r, float b, int id, int arg) {
        Hit h = new Hit();
        h.r.set(l, t - (inScroll ? curOff : 0) * 0, r, b);
        if (inScroll) { h.r.offset(0, -curOff); if (curArea != null) { h.r.top = Math.max(h.r.top, curArea.top); h.r.bottom = Math.min(h.r.bottom, curArea.bottom); if (h.r.height() <= 0) return; } }
        h.id = id; h.arg = arg;
        if (modal) { /* modal hits always come last, nothing special */ }
        hits.add(h);
    }

    void modalBegin() {
        // block everything underneath: add a full-screen dummy hit
        Hit h = new Hit(); h.r.set(0, 0, vw, vh); h.id = -1; h.arg = 0; hits.add(h);
        scrolls.clear();
    }

    // ------------------------------------------------------------- scroll
    float scrollBegin(int key, float x, float y, float w, float h, float contentH) {
        Scroll s = new Scroll(); s.r.set(x, y, x + w, y + h); s.content = contentH; s.key = key; scrolls.add(s);
        float max = Math.max(0, contentH - h);
        float o = offs.containsKey(key) ? offs.get(key) : 0;
        if (o < 0) { o = 0; vels.put(key, 0f); }
        if (o > max) { o = max; vels.put(key, 0f); }
        offs.put(key, o);
        c.save();
        c.clipRect(x, y, x + w, y + h);
        c.translate(0, -o);
        inScroll = true; curOff = o; curArea = s.r;
        return o;
    }
    void scrollEnd() { c.restore(); inScroll = false; curOff = 0; curArea = null; }
    void scrollReset(int key) { offs.put(key, 0f); vels.put(key, 0f); }

    // ------------------------------------------------------------- primitives
    void bg() {
        fill.setShader(new LinearGradient(0, 0, vw * 0.4f, vh, BG0, BG1, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, vw, vh, fill);
        fill.setShader(new RadialGradient(vw * 0.85f, vh * 0.1f, vw * 0.5f, 0x338A3FFC, 0x008A3FFC, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, vw, vh, fill);
        fill.setShader(new RadialGradient(vw * 0.1f, vh * 0.95f, vw * 0.4f, 0x2222D3EE, 0x0022D3EE, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, vw, vh, fill);
        fill.setShader(null);
    }

    void panel(float x, float y, float w, float h, int color, float rad) {
        rc.set(x, y, x + w, y + h);
        fill.setShader(null);
        fill.setColor(color);
        c.drawRoundRect(rc, rad, rad, fill);
        stroke.setStrokeWidth(1.5f);
        stroke.setColor(0x22FFFFFF);
        c.drawRoundRect(rc, rad, rad, stroke);
    }

    void glass(float x, float y, float w, float h, float rad) {
        rc.set(x, y, x + w, y + h);
        fill.setShader(new LinearGradient(x, y, x, y + h, 0xEE222B48, 0xEE141A30, Shader.TileMode.CLAMP));
        c.drawRoundRect(rc, rad, rad, fill);
        fill.setShader(null);
        stroke.setStrokeWidth(1.5f);
        stroke.setColor(0x33FFFFFF);
        c.drawRoundRect(rc, rad, rad, stroke);
    }

    void text(String s, float x, float y, float size, int color, int align) { text(s, x, y, size, color, align, true); }
    void text(String s, float x, float y, float size, int color, int align, boolean b) {
        txt.setTypeface(b ? bold : norm);
        txt.setTextSize(size);
        txt.setColor(color);
        txt.setTextAlign(align == 0 ? Paint.Align.LEFT : (align == 1 ? Paint.Align.CENTER : Paint.Align.RIGHT));
        c.drawText(s, x, y, txt);
    }
    void textFit(String s, float x, float y, float maxW, float size, int color, int align) {
        txt.setTypeface(bold);
        txt.setTextSize(size);
        float w = txt.measureText(s);
        if (w > maxW) size = size * maxW / w;
        text(s, x, y, size, color, align, true);
    }
    float textW(String s, float size) { txt.setTypeface(bold); txt.setTextSize(size); return txt.measureText(s); }

    static final int ST_PRIMARY = 0, ST_SECOND = 1, ST_DANGER = 2, ST_GOOD = 3, ST_TAB = 4, ST_TABON = 5, ST_GHOST = 6;

    void button(float x, float y, float w, float h, String label, int id, int arg, int style) { button(x, y, w, h, label, id, arg, style, h * 0.42f); }
    void button(float x, float y, float w, float h, String label, int id, int arg, int style, float size) {
        rc.set(x, y, x + w, y + h);
        float yy = inScroll ? y - curOff : y;
        boolean pr = false;
        if (down && downHit != null && downHit.id == id && downHit.arg == arg && !dragged) { RectF r2 = downHit.r; pr = r2.contains(dX, dY) && Math.abs(r2.left - x) < 2 && Math.abs(r2.top - yy) < 2; }
        float shrink = pr ? 3 : 0;
        rc.set(x + shrink, y + shrink, x + w - shrink, y + h - shrink);
        int c0, c1; int tc = WHITE;
        switch (style) {
            case ST_SECOND: c0 = 0xFF343E62; c1 = 0xFF262E4C; break;
            case ST_DANGER: c0 = 0xFFE5484D; c1 = 0xFFB02A3A; break;
            case ST_GOOD: c0 = 0xFF2AD16C; c1 = 0xFF15964A; break;
            case ST_TAB: c0 = 0x00000000; c1 = 0x00000000; tc = GRAY; break;
            case ST_TABON: c0 = 0xFF8A3FFC; c1 = 0xFF5B21D6; break;
            case ST_GHOST: c0 = 0x33FFFFFF; c1 = 0x14FFFFFF; break;
            default: c0 = 0xFF9B5BFF; c1 = 0xFF6D2BE0;
        }
        if (style != ST_TAB) {
            fill.setShader(new LinearGradient(0, rc.top, 0, rc.bottom, pr ? U.shade(c0, 0.8f) : c0, pr ? U.shade(c1, 0.8f) : c1, Shader.TileMode.CLAMP));
            c.drawRoundRect(rc, h * 0.22f, h * 0.22f, fill);
            fill.setShader(null);
            fill.setColor(0x22FFFFFF);
            RectF r2 = new RectF(rc.left + 2, rc.top + 2, rc.right - 2, rc.top + h * 0.45f);
            c.drawRoundRect(r2, h * 0.2f, h * 0.2f, fill);
        } else if (pr) { fill.setColor(0x22FFFFFF); c.drawRoundRect(rc, h * 0.2f, h * 0.2f, fill); }
        float s = size;
        txt.setTypeface(bold); txt.setTextSize(s);
        float tw = txt.measureText(label);
        if (tw > w - 16) s = s * (w - 16) / tw;
        text(label, x + w / 2, y + h / 2 + s * 0.35f, s, tc, 1);
        hit(x, y, x + w, y + h, id, arg);
    }

    void bar(float x, float y, float w, float h, float v, int color) {
        rc.set(x, y, x + w, y + h);
        fill.setColor(0x44000000);
        c.drawRoundRect(rc, h / 2, h / 2, fill);
        rc.set(x, y, x + Math.max(h, w * U.clamp(v, 0, 1)), y + h);
        fill.setColor(color);
        c.drawRoundRect(rc, h / 2, h / 2, fill);
    }

    int ovrColor(int o) {
        if (o >= 85) return 0xFFFFD54A; if (o >= 78) return 0xFF22D3A0; if (o >= 70) return 0xFF84CC16; if (o >= 62) return 0xFFFBBF24; return 0xFFFB923C;
    }

    void ovrBadge(float cx, float cy, float r, int ovr) {
        int col = ovrColor(ovr);
        fill.setColor(col);
        rc.set(cx - r, cy - r * 0.8f, cx + r, cy + r * 0.8f);
        c.drawRoundRect(rc, r * 0.3f, r * 0.3f, fill);
        text(String.valueOf(ovr), cx, cy + r * 0.38f, r * 1.05f, 0xFF10152A, 1);
    }

    // ------------------------------------------------------------- crest / flag / face
    void crest(Team t, float cx, float cy, float r) { crest(t.c1, t.c2, t.c3, t.crestShape, t.crestSym, cx, cy, r); }
    void crest(int c1, int c2, int c3, int shape, int sym, float cx, float cy, float r) {
        path.reset();
        switch (shape) {
            case 0: path.moveTo(cx - r * 0.85f, cy - r); path.lineTo(cx + r * 0.85f, cy - r); path.lineTo(cx + r * 0.85f, cy + r * 0.1f);
                path.quadTo(cx + r * 0.8f, cy + r * 0.75f, cx, cy + r * 1.1f); path.quadTo(cx - r * 0.8f, cy + r * 0.75f, cx - r * 0.85f, cy + r * 0.1f); path.close(); break;
            case 1: path.addCircle(cx, cy, r, Path.Direction.CW); break;
            case 2: for (int i = 0; i < 6; i++) { float a = (float) (Math.PI / 3 * i - Math.PI / 2); float x = cx + (float) Math.cos(a) * r, y = cy + (float) Math.sin(a) * r; if (i == 0) path.moveTo(x, y); else path.lineTo(x, y); } path.close(); break;
            default: path.moveTo(cx - r * 0.9f, cy - r); path.lineTo(cx + r * 0.9f, cy - r); path.lineTo(cx + r * 0.9f, cy + r * 0.3f); path.lineTo(cx, cy + r * 1.1f); path.lineTo(cx - r * 0.9f, cy + r * 0.3f); path.close();
        }
        fill.setColor(0xFF0B0F1E);
        c.save(); c.translate(cx, cy); c.scale(1.07f, 1.07f); c.translate(-cx, -cy);
        c.drawPath(path, fill); c.restore();
        fill.setColor(c2);
        c.save(); c.translate(cx, cy); c.scale(1.0f, 1.0f); c.translate(-cx, -cy); c.drawPath(path, fill); c.restore();
        c.save(); c.translate(cx, cy); c.scale(0.86f, 0.86f); c.translate(-cx, -cy);
        fill.setColor(c1); c.drawPath(path, fill);
        c.clipPath(path);
        fill.setColor(c3);
        switch (sym) {
            case 0: star(cx, cy - r * 0.05f, r * 0.55f, c3); break;
            case 1: for (int i = -1; i <= 1; i++) c.drawRect(cx + i * r * 0.5f - r * 0.12f, cy - r, cx + i * r * 0.5f + r * 0.12f, cy + r * 1.2f, fill); break;
            case 2: path.reset(); path.moveTo(cx - r, cy + r * 0.1f); path.lineTo(cx, cy - r * 0.55f); path.lineTo(cx + r, cy + r * 0.1f); path.lineTo(cx + r, cy + r * 0.45f); path.lineTo(cx, cy - r * 0.2f); path.lineTo(cx - r, cy + r * 0.45f); path.close(); c.drawPath(path, fill); break;
            case 3: path.reset(); path.moveTo(cx - r, cy - r * 0.2f); path.lineTo(cx - r, cy + r * 0.3f); path.lineTo(cx + r, cy - r * 0.6f); path.lineTo(cx + r, cy - r * 1.1f); path.close(); c.drawPath(path, fill); break;
            case 4: c.drawRect(cx - r * 0.14f, cy - r, cx + r * 0.14f, cy + r, fill); c.drawRect(cx - r, cy - r * 0.15f, cx + r, cy + r * 0.13f, fill); break;
            default: path.reset(); path.moveTo(cx + r * 0.15f, cy - r * 0.8f); path.lineTo(cx - r * 0.45f, cy + r * 0.1f); path.lineTo(cx - r * 0.02f, cy + r * 0.1f); path.lineTo(cx - r * 0.2f, cy + r * 0.85f); path.lineTo(cx + r * 0.5f, cy - r * 0.15f); path.lineTo(cx + r * 0.05f, cy - r * 0.15f); path.close(); c.drawPath(path, fill);
        }
        c.restore();
    }

    void star(float cx, float cy, float r, int col) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            float a = (float) (Math.PI / 5 * i - Math.PI / 2), rr = i % 2 == 0 ? r : r * 0.42f;
            float x = cx + (float) Math.cos(a) * rr, y = cy + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        fill.setColor(col);
        c.drawPath(path, fill);
    }

    void flag(int nat, float x, float y, float w) {
        int[] f = Data.NAT_FLAG[nat];
        float h = w * 0.66f;
        c.save();
        rc.set(x, y, x + w, y + h);
        path.reset(); path.addRoundRect(rc, 3, 3, Path.Direction.CW);
        c.clipPath(path);
        switch (f[0]) {
            case 0: for (int i = 0; i < 3; i++) { fill.setColor(f[1 + i]); c.drawRect(x, y + h * i / 3, x + w, y + h * (i + 1) / 3 + 1, fill); } break;
            case 1: for (int i = 0; i < 3; i++) { fill.setColor(f[1 + i]); c.drawRect(x + w * i / 3, y, x + w * (i + 1) / 3 + 1, y + h, fill); } break;
            case 2: fill.setColor(f[1]); c.drawRect(x, y, x + w, y + h, fill); fill.setColor(f[2]); c.drawRect(x + w * 0.4f, y, x + w * 0.58f, y + h, fill); c.drawRect(x, y + h * 0.4f, x + w, y + h * 0.6f, fill); break;
            case 3: fill.setColor(f[1]); c.drawRect(x, y, x + w, y + h, fill); fill.setColor(f[2]); c.drawCircle(x + w / 2, y + h / 2, h * 0.3f, fill); break;
            default: fill.setColor(f[1]); c.drawRect(x, y, x + w, y + h, fill); fill.setColor(f[2]);
                path.reset(); path.moveTo(x + w / 2, y + h * 0.1f); path.lineTo(x + w * 0.9f, y + h / 2); path.lineTo(x + w / 2, y + h * 0.9f); path.lineTo(x + w * 0.1f, y + h / 2); path.close(); c.drawPath(path, fill);
                fill.setColor(f[3]); c.drawCircle(x + w / 2, y + h / 2, h * 0.22f, fill);
        }
        c.restore();
    }

    /** Bust portrait of a player. */
    void face(Player p, float cx, float cy, float r, int shirt) {
        int skin = Scene3.SKIN[p.skin], hair = Scene3.HAIR[p.hairColor];
        c.save();
        rc.set(cx - r, cy - r, cx + r, cy + r);
        path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW);
        c.clipPath(path);
        fill.setShader(new LinearGradient(cx, cy - r, cx, cy + r, 0xFF3A4570, 0xFF1B2140, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(null);
        // shoulders
        fill.setColor(shirt);
        rc.set(cx - r * 0.9f, cy + r * 0.45f, cx + r * 0.9f, cy + r * 1.5f);
        c.drawRoundRect(rc, r * 0.45f, r * 0.45f, fill);
        fill.setColor(U.shade(skin, 0.92f));
        c.drawRect(cx - r * 0.17f, cy + r * 0.25f, cx + r * 0.17f, cy + r * 0.55f, fill);
        float hr = r * 0.42f, hcy = cy - r * 0.08f;
        fill.setColor(hair);
        if (p.hairStyle == 3) { rc.set(cx - hr * 1.12f, hcy - hr, cx + hr * 1.12f, hcy + hr * 1.9f); c.drawOval(rc, fill); }
        if (p.hairStyle == 5) c.drawCircle(cx, hcy - hr * 0.1f, hr * 1.5f, fill);
        fill.setColor(skin);
        rc.set(cx - hr * 0.95f, hcy - hr, cx + hr * 0.95f, hcy + hr * 1.12f);
        c.drawOval(rc, fill);
        fill.setColor(U.shade(skin, 0.88f));
        c.drawCircle(cx - hr * 0.97f, hcy + hr * 0.1f, hr * 0.2f, fill); c.drawCircle(cx + hr * 0.97f, hcy + hr * 0.1f, hr * 0.2f, fill);
        fill.setColor(hair);
        rc.set(cx - hr * 1.0f, hcy - hr * 1.06f, cx + hr * 1.0f, hcy + hr * 0.9f);
        switch (p.hairStyle) {
            case 1: fill.setAlpha(120); c.drawArc(rc, 180, 180, true, fill); fill.setAlpha(255); break;
            case 2: case 3: case 7: c.drawArc(rc, 180, 180, true, fill); break;
            case 4: rc.set(cx - hr * 0.22f, hcy - hr * 1.5f, cx + hr * 0.22f, hcy - hr * 0.3f); c.drawOval(rc, fill); break;
            case 5: rc.set(cx - hr * 1.3f, hcy - hr * 1.5f, cx + hr * 1.3f, hcy - hr * 0.15f); c.drawArc(rc, 180, 180, true, fill); break;
            case 6: for (int i = 0; i < 6; i++) c.drawCircle(cx - hr * 0.85f + i * hr * 0.34f, hcy - hr * (0.8f + (i % 2) * 0.12f), hr * 0.33f, fill); break;
            default: break;
        }
        fill.setColor(0xFF1A1A1A);
        c.drawCircle(cx - hr * 0.38f, hcy + hr * 0.1f, hr * 0.1f, fill); c.drawCircle(cx + hr * 0.38f, hcy + hr * 0.1f, hr * 0.1f, fill);
        stroke.setStrokeWidth(Math.max(1.5f, hr * 0.09f)); stroke.setColor(U.shade(skin, 0.6f));
        c.drawLine(cx - hr * 0.2f, hcy + hr * 0.62f, cx + hr * 0.2f, hcy + hr * 0.62f, stroke);
        if (p.beard == 1) { fill.setColor(hair); fill.setAlpha(210); rc.set(cx - hr * 0.9f, hcy - hr * 0.05f, cx + hr * 0.9f, hcy + hr * 1.1f); c.drawArc(rc, 15, 150, true, fill); fill.setAlpha(255); }
        c.restore();
        stroke.setStrokeWidth(2); stroke.setColor(0x44FFFFFF);
        c.drawCircle(cx, cy, r, stroke);
    }

    void shield(float cx, float cy, float r, int col) { }

    void chip(float x, float y, String s, int color, float size) {
        float w = textW(s, size) + size * 1.2f;
        rc.set(x, y, x + w, y + size * 1.7f);
        fill.setColor(color);
        c.drawRoundRect(rc, size * 0.5f, size * 0.5f, fill);
        text(s, x + w / 2, y + size * 1.22f, size, WHITE, 1);
    }
}
