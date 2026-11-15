package com.yotta.fc;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Camera + 2D overlay (markers, aim, rain) on top of the OpenGL scene. */
final class Renderer {
    int vw, vh;
    final Cam3 cam = new Cam3();
    float camZoomMul = 1f;
    float excite, time;
    int tod;
    boolean rain;
    long matchId;
    Pawn marked, target;
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final RectF rc = new RectF();
    final float[] o1 = new float[3], o2 = new float[3];

    Renderer() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
    }

    void setup(int w, int h, Match m, int timeOfDay, boolean rain) {
        vw = w; vh = h; tod = timeOfDay; this.rain = rain;
        matchId = System.nanoTime() & 0xFFFFFFFFL;
        cam.aspect = w / (float) h;
        cam.tx = Match.L / 2; cam.tz = Match.CY;
        cam.zoom = camZoomMul;
        cam.compute();
    }

    void updateCamera(Match m, float dt, boolean replay) {
        cam.aspect = vw / (float) vh;
        cam.update(m, dt, camZoomMul);
        float target = m.mode == Match.M_GOAL ? 1f : (Math.abs(m.bx - Match.L / 2) > 36 ? 0.45f : 0.2f);
        excite += (target - excite) * Math.min(1f, dt * 1.6f);
    }

    SceneData snapshot(Match m, float t) {
        SceneData d = new SceneData();
        d.capture(m, matchId);
        cam.fill(d);
        d.tod = tod; d.rain = rain; d.time = t; d.excite = excite;
        time = t;
        return d;
    }

    boolean proj(float x, float y, float z) { return M4.project(cam.vp, x, y, z, vw, vh, o1); }

    void drawOverlay(Canvas c, Match m) {
        // ring + name for the controlled player
        if (marked != null && marked.st != Pawn.SENT) {
            Pawn p = marked;
            if (proj(p.x, 0.03f, p.y)) {
                float cx = o1[0], cy = o1[1];
                float rx = 0, ry = 0;
                if (M4.project(cam.vp, p.x + 0.9f, 0.03f, p.y, vw, vh, o2)) rx = Math.abs(o2[0] - cx);
                if (M4.project(cam.vp, p.x, 0.03f, p.y + 0.9f, vw, vh, o2)) ry = Math.abs(o2[1] - cy);
                float pulse = 1 + 0.07f * (float) Math.sin(time * 7);
                stroke.setStrokeWidth(Math.max(3, rx * 0.12f));
                stroke.setColor(0xFFFFD54A);
                rc.set(cx - rx * pulse, cy - ry * pulse, cx + rx * pulse, cy + ry * pulse);
                c.drawOval(rc, stroke);
            }
            drawMarker(c, p, 0xFFFFD54A, true);
        }
        if (target != null && target != marked && target.st != Pawn.SENT) drawMarker(c, target, 0xFF4ADE80, false);
        // set-piece aim
        if (m.mode == Match.M_SET && m.spHuman && !m.autoPlay && (m.spType == Match.SP_FREEKICK || m.spType == Match.SP_PENALTY || m.spType == Match.SP_CORNER)) {
            float h = m.spType == Match.SP_CORNER ? 0.05f : 1.2f;
            if (proj(m.spAimX, h, m.spAimY)) {
                float pulse = 1 + 0.15f * (float) Math.sin(time * 8);
                float r = (m.spType == Match.SP_CORNER ? 44 : 26) * pulse * (30f / Math.max(10, o1[2])) * 3f;
                r = Math.max(16, Math.min(r, 90));
                stroke.setColor(0xFFFFD54A); stroke.setStrokeWidth(5);
                c.drawCircle(o1[0], o1[1], r, stroke);
                c.drawLine(o1[0] - r * 1.4f, o1[1], o1[0] + r * 1.4f, o1[1], stroke);
                c.drawLine(o1[0], o1[1] - r * 1.4f, o1[0], o1[1] + r * 1.4f, stroke);
            }
        }
        if (rain) {
            stroke.setColor(0x66CFE8FF); stroke.setStrokeWidth(2f);
            java.util.Random r = new java.util.Random(3);
            for (int i = 0; i < 170; i++) {
                float x = r.nextInt(vw + 200) - 100, sp = 1100 + r.nextInt(700), y0 = r.nextInt(vh);
                float y = (y0 + time * sp) % (vh + 80) - 40;
                float xx = x - y * 0.12f;
                c.drawLine(xx, y, xx - 6, y + 26, stroke);
            }
        }
    }

    void drawMarker(Canvas c, Pawn p, int col, boolean named) {
        if (!proj(p.x, p.pl.height / 100f * 1.0f + 0.55f + p.z, p.y)) return;
        float bob = (float) Math.sin(time * 6) * 6;
        float s = Math.max(14, Math.min(34, 900f / o1[2]));
        path.reset();
        path.moveTo(o1[0], o1[1] + bob + s * 0.2f);
        path.lineTo(o1[0] - s, o1[1] + bob - s * 1.1f);
        path.lineTo(o1[0] + s, o1[1] + bob - s * 1.1f);
        path.close();
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(col);
        c.drawPath(path, fill);
        if (named) {
            tp.setTextSize(Math.max(22, s * 1.1f));
            tp.setColor(0xFFFFFFFF);
            tp.setShadowLayer(4, 0, 2, 0xFF000000);
            c.drawText(p.pl.last.toUpperCase(), o1[0], o1[1] + bob - s * 1.5f, tp);
            tp.clearShadowLayer();
        }
    }
}
