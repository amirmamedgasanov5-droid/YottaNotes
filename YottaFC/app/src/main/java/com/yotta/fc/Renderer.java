package com.yotta.fc;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Draws the 3D-projected match: stadium, pitch, people, ball. */
final class Renderer {
    static final float L = Match.L, W = Match.W;
    // skin / hair palettes
    static final int[] SKIN = {0xFFF6D2B8, 0xFFE8B896, 0xFFD0976B, 0xFFB07048, 0xFF8A5230, 0xFF5E3A22};
    static final int[] HAIR = {0xFF17171B, 0xFF3A2314, 0xFF6B4423, 0xFFD8B25A, 0xFFA8431F, 0xFFBDBDBD};

    int vw, vh;
    // camera
    float camX = L / 2, camY = 34, zoom = 1f, camZoomMul = 1f;
    float phi = 0.42f, D = 200f, fl = 8000f, hy = 0.72f;
    float cosP, sinP;
    float px, py, ps; // last projection result
    float time;
    int tod;            // 0 day 1 evening 2 night
    boolean rain;
    float rainT;
    float excite;       // crowd excitement 0..1
    int homeCol, awayCol;
    float shakeX, shakeY;

    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final Path path2 = new Path();
    final RectF rect = new RectF();
    final Matrix mtx = new Matrix();

    Bitmap crowdBmp;
    BitmapShader crowdShader;
    Bitmap crowdBmp2;
    BitmapShader crowdShader2;
    LinearGradient skyGrad;
    int lastW = -1, lastH = -1;
    final float[] tmp = new float[4];
    // sorting buffer
    final float[] ents = new float[40 * 2];
    final int[] entIdx = new int[40];

    final Figures figs = new Figures(this);
    void drawActors(Canvas c, Match m) { figs.drawActors(c, m); }

    Renderer() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
    }

    void setup(int w, int h, Match m, int timeOfDay, boolean rain) {
        vw = w; vh = h; tod = timeOfDay; this.rain = rain;
        cosP = (float) Math.cos(phi); sinP = (float) Math.sin(phi);
        fl = vh / 1080f * 8000f;
        homeCol = m.S[0].c1; awayCol = m.S[1].c1;
        int sky0, sky1;
        if (tod == 0) { sky0 = 0xFF4C9BE8; sky1 = 0xFFCDE8FF; }
        else if (tod == 1) { sky0 = 0xFF2A2A6A; sky1 = 0xFFF28A4B; }
        else { sky0 = 0xFF04060F; sky1 = 0xFF121C33; }
        skyGrad = new LinearGradient(0, 0, 0, vh * 0.55f, sky0, sky1, Shader.TileMode.CLAMP);
        makeCrowd(m);
        camX = L / 2; camY = 34;
    }

    void makeCrowd(Match m) {
        int bw = 512, bh = 64;
        for (int k = 0; k < 2; k++) {
            Bitmap b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.drawColor(0xFF1B2030);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            java.util.Random r = new java.util.Random(77 + k * 31);
            int[] cols = {m.S[0].c1, m.S[0].c1, m.S[0].c2, m.S[1].c1, m.S[1].c1, 0xFFE5E7EB, 0xFF9CA3AF, 0xFF374151, 0xFFFBBF24, 0xFFEF4444, 0xFF60A5FA};
            if (k == 1) cols = new int[]{m.S[1].c1, m.S[1].c1, m.S[1].c2, m.S[0].c1, 0xFFE5E7EB, 0xFF9CA3AF, 0xFF374151, 0xFFFBBF24, 0xFF22C55E};
            for (int row = 0; row < 4; row++) {
                int y0 = row * 16;
                for (int x = -8; x < bw; x += 8 + r.nextInt(4)) {
                    int col = cols[r.nextInt(cols.length)];
                    p.setColor(col);
                    c.drawRoundRect(new RectF(x, y0 + 7, x + 11, y0 + 19), 4, 4, p);
                    p.setColor(SKIN[r.nextInt(6)]);
                    c.drawCircle(x + 5.5f, y0 + 5 + r.nextInt(2), 3.6f, p);
                    if (r.nextInt(4) == 0) { p.setColor(HAIR[r.nextInt(6)]); c.drawArc(new RectF(x + 2, y0 + 1, x + 9, y0 + 8), 180, 180, true, p); }
                }
                p.setColor(0x66000000);
                c.drawRect(0, y0 + 14, bw, y0 + 16, p);
            }
            if (k == 0) { crowdBmp = b; crowdShader = new BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT); }
            else { crowdBmp2 = b; crowdShader2 = new BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT); }
        }
    }

    // ------------------------------------------------------------- projection
    void proj(float x, float y, float z) {
        float zc = D - (y - camY) * cosP - z * sinP;
        float yc = -(y - camY) * sinP + z * cosP;
        float s = fl * zoom / zc;
        px = vw * 0.5f + s * (x - camX) + shakeX;
        py = vh * hy - s * yc + shakeY;
        ps = s;
    }

    // ------------------------------------------------------------- camera
    void updateCamera(Match m, float dt, boolean replay) {
        float tx, ty;
        if (m.mode == Match.M_KICKOFF) { tx = L / 2; ty = 34; }
        else if (m.mode == Match.M_SET && m.spType != Match.SP_NONE) {
            tx = m.spX; ty = 34 * 0.5f + m.spY * 0.5f;
            if (m.spType == Match.SP_PENALTY || m.spType == Match.SP_CORNER || (m.spType == Match.SP_FREEKICK && Math.abs(m.goalX(m.S[m.spSide]) - m.spX) < 36)) {
                float gx = m.goalX(m.S[m.spSide]);
                tx = (m.spX + gx) * 0.5f;
            }
        }
        else if (m.mode == Match.M_PEN) { tx = L - 18; ty = 34; }
        else {
            float lead = 0.35f;
            tx = m.bx + m.bvx * lead; ty = 34 * 0.55f + m.by * 0.45f;
            if (m.owner != null) { tx = m.owner.x + m.owner.vx * 0.5f; }
            if (m.mode == Match.M_GOAL) { tx = m.bx; }
        }
        tx = U.clamp(tx, 20f / zoom - 6, L - 20f / zoom + 6);
        float k = Math.min(1f, dt * (m.mode == Match.M_PLAY ? 3.4f : 2.2f));
        camX += (tx - camX) * k;
        camY += (ty - camY) * k * 0.8f;
        float zt = camZoomMul;
        if (m.mode == Match.M_SET && m.spType == Match.SP_PENALTY || m.mode == Match.M_PEN) zt *= 1.12f;
        if (m.mode == Match.M_GOAL) zt *= 1.18f;
        zoom += (zt - zoom) * Math.min(1f, dt * 2f);
        if (m.shake > 0) { shakeX = (float) Math.sin(m.time * 90) * 6 * m.shake; shakeY = (float) Math.cos(m.time * 77) * 5 * m.shake; }
        else { shakeX = shakeY = 0; }
        // crowd excitement
        float target = m.mode == Match.M_GOAL ? 1f : (Math.abs(m.bx - L / 2) > 36 ? 0.45f : 0.2f);
        excite += (target - excite) * Math.min(1f, dt * 1.6f);
    }

    // ============================================================== SCENE
    void drawScene(Canvas c, Match m, float t) {
        time = t;
        drawSky(c);
        drawStands(c, m);
        drawPitch(c, m);
        drawBoards(c);
        drawGoal(c, 0, false); drawGoal(c, 1, false);
        drawActors(c, m);
        drawGoal(c, 0, true); drawGoal(c, 1, true);
        drawLights(c);
        drawAimOverlay(c, m);
        if (rain) drawRain(c, t);
        if (tod == 2) {
            fill.setShader(null);
            fill.setColor(0x33000a20);
            c.drawRect(0, 0, vw, vh, fill);
        } else if (tod == 1) {
            fill.setShader(null);
            fill.setColor(0x1AFF8A30);
            c.drawRect(0, 0, vw, vh, fill);
        }
        drawVignette(c);
    }

    void drawSky(Canvas c) {
        fill.setShader(skyGrad);
        c.drawRect(0, 0, vw, vh, fill);
        fill.setShader(null);
        if (tod == 2) {
            fill.setColor(0xCCFFFFFF);
            java.util.Random r = new java.util.Random(5);
            for (int i = 0; i < 40; i++) c.drawCircle(r.nextInt(vw), r.nextInt((int) (vh * 0.2f)), 1.2f + r.nextFloat() * 1.4f, fill);
        }
    }

    void drawVignette(Canvas c) {
        fill.setShader(new RadialGradient(vw / 2f, vh / 2f, vw * 0.62f, 0x00000000, 0x66000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, vw, vh, fill);
        fill.setShader(null);
    }

    // ---------------------------------------------------------------- stands
    void drawStands(Canvas c, Match m) {
        // far stand: rows rising behind the far touchline
        float xl = -60, xr = L + 60;
        int rows = 16;
        // back wall / structure
        proj(xl, -6, 0); float lx0 = px, ly0 = py;
        proj(xr, -6, 0); float rx0 = px;
        proj(xl, -44, 30); float ty = py;
        float topY = ty;
        // concrete backdrop
        fill.setShader(new LinearGradient(0, topY, 0, ly0, tod == 2 ? 0xFF0B1020 : 0xFF2A3042, tod == 2 ? 0xFF161C2E : 0xFF3A4156, Shader.TileMode.CLAMP));
        c.drawRect(0, Math.max(0, topY - 30), vw, ly0 + 2, fill);
        fill.setShader(null);
        for (int i = 0; i < rows; i++) {
            float y0 = -5.5f - i * 2.3f, z0 = 0.5f + i * 1.7f;
            float y1 = -5.5f - (i + 1) * 2.3f, z1 = 0.5f + (i + 1) * 1.7f;
            proj(xl, y0, z0); float sx0 = px, sy0 = py, sc0 = ps;
            proj(xr, y0, z0); float sx1 = px;
            proj(xl, y1, z1); float sy1 = py;
            if (sy0 < -10 && sy1 < -10) continue;
            // crowd band
            float bob = (float) Math.sin(time * (3f + i * 0.2f) + i) * (1.5f + 8f * excite) * sc0 * 0.02f * (0.3f + excite);
            BitmapShader sh = new BitmapShader((i % 2 == 0) ? crowdBmp : crowdBmp2, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            mtx.reset();
            float scl = sc0 * 0.62f / 11f * 1.0f;
            mtx.postScale(scl, scl);
            mtx.postTranslate(sx0 + (i * 53 % 200), sy1 + bob);
            sh.setLocalMatrix(mtx);
            fill.setShader(sh);
            c.drawRect(Math.min(sx0, sx1), sy1 + bob, Math.max(sx0, sx1), sy0, fill);
            fill.setShader(null);
            // shade for depth
            fill.setColor(U.alpha(0xFF000000, (int) (40 + i * 5 + (tod == 2 ? 40 : 0))));
            c.drawRect(0, sy1, vw, sy0, fill);
            // step edge
            fill.setColor(0xAA10131C);
            c.drawRect(0, sy0 - 3, vw, sy0, fill);
        }
        // roof
        proj(xl, -40, 29); float rfy = py;
        proj(xl, -14, 26); float rfy2 = py;
        fill.setColor(tod == 2 ? 0xFF05070C : 0xFF1B2030);
        c.drawRect(0, rfy - 60, vw, rfy, fill);
        fill.setColor(tod == 2 ? 0xFF0B1220 : 0xFF2E364D);
        c.drawRect(0, rfy, vw, Math.max(rfy + 8, rfy2 - 10), fill);
        // end stands (behind goals), simple banks
        for (int e = 0; e < 2; e++) {
            float sgn = e == 0 ? -1 : 1;
            float xe = e == 0 ? 0 : L;
            path.reset();
            proj(xe + sgn * 7, -3, 0); path.moveTo(px, py);
            proj(xe + sgn * 7, W + 3, 0); path.lineTo(px, py);
            proj(xe + sgn * 33, W + 12, 15); path.lineTo(px, py);
            proj(xe + sgn * 33, -32, 15); path.lineTo(px, py);
            path.close();
            mtx.reset();
            proj(xe + sgn * 10, 0, 0);
            float scl = ps * 0.6f / 11f;
            mtx.postScale(scl, scl);
            mtx.postTranslate(px, py - ps * 12);
            BitmapShader es = new BitmapShader(crowdBmp2, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            es.setLocalMatrix(mtx);
            fill.setShader(es);
            c.drawPath(path, fill);
            fill.setShader(null);
            fill.setColor(tod == 2 ? 0xAA000511 : 0x77151A28);
            c.drawPath(path, fill);
        }
    }

    // ---------------------------------------------------------------- pitch
    static final int[] GRASS_D = {0xFF2E8B3A, 0xFF1F6E2E, 0xFF1A5E28};
    void drawPitch(Canvas c, Match m) {
        // surrounding apron
        proj(-20, -8, 0); float ax0 = px, ay0 = py;
        proj(L + 20, W + 10, 0); float ax1 = px, ay1 = py;
        int apron = tod == 2 ? 0xFF0F3A1C : 0xFF1B6B2B;
        fill.setColor(apron);
        path.reset();
        proj(-18, -7, 0); path.moveTo(px, py);
        proj(L + 18, -7, 0); path.lineTo(px, py);
        proj(L + 30, W + 14, 0); path.lineTo(px, py);
        proj(-30, W + 14, 0); path.lineTo(px, py);
        path.close();
        c.drawPath(path, fill);
        // mowing stripes
        int n = 21;
        for (int i = 0; i < n; i++) {
            float x0 = -5 + i * (L + 10) / n, x1 = -5 + (i + 1) * (L + 10) / n;
            path.reset();
            proj(x0, -4, 0); path.moveTo(px, py);
            proj(x1, -4, 0); path.lineTo(px, py);
            proj(x1, W + 4, 0); path.lineTo(px, py);
            proj(x0, W + 4, 0); path.lineTo(px, py);
            path.close();
            int col = (i % 2 == 0) ? 0xFF2F9440 : 0xFF2A8739;
            if (tod == 2) col = (i % 2 == 0) ? 0xFF1C6B2C : 0xFF18602A;
            else if (tod == 1) col = (i % 2 == 0) ? 0xFF2A8A3A : 0xFF257D34;
            fill.setColor(col);
            c.drawPath(path, fill);
        }
        // depth shading
        proj(L / 2, 0, 0); float fy = py;
        proj(L / 2, W, 0); float ny = py;
        fill.setShader(new LinearGradient(0, fy, 0, ny, 0x44000000, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(0, Math.max(0, fy - 40), vw, ny + 40, fill);
        fill.setShader(null);
        if (rain) {
            fill.setShader(new LinearGradient(0, fy, 0, ny, 0x00FFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, Math.max(0, fy), vw, ny, fill);
            fill.setShader(null);
        }
        drawLines(c);
    }

    void line(Canvas c, float x0, float y0, float x1, float y1) {
        proj(x0, y0, 0); float a = px, b = py;
        proj(x1, y1, 0);
        c.drawLine(a, b, px, py, stroke);
    }

    void drawLines(Canvas c) {
        proj(L / 2, W / 2, 0);
        stroke.setStrokeWidth(Math.max(2f, ps * 0.2f));
        stroke.setColor(0xDDFFFFFF);
        stroke.setShader(null);
        line(c, 0, 0, L, 0); line(c, L, 0, L, W); line(c, L, W, 0, W); line(c, 0, W, 0, 0);
        line(c, L / 2, 0, L / 2, W);
        float[][] boxes = {{16.5f, 20.16f}, {5.5f, 9.16f}};
        for (int e = 0; e < 2; e++) {
            float s = e == 0 ? 1 : -1, x0 = e == 0 ? 0 : L;
            for (float[] bx : boxes) {
                float xx = x0 + s * bx[0];
                line(c, x0, 34 - bx[1], xx, 34 - bx[1]);
                line(c, xx, 34 - bx[1], xx, 34 + bx[1]);
                line(c, xx, 34 + bx[1], x0, 34 + bx[1]);
            }
            // penalty arc
            arc(c, x0 + s * 11, 34, 9.15f, e == 0 ? -0.93f : (float) Math.PI - 0.93f, 1.86f);
            dot(c, x0 + s * 11, 34, 0.2f);
        }
        arc(c, L / 2, 34, 9.15f, 0, (float) (Math.PI * 2));
        dot(c, L / 2, 34, 0.22f);
        // corner arcs
        arc(c, 0, 0, 1f, 0, (float) Math.PI / 2); arc(c, L, 0, 1f, (float) Math.PI / 2, (float) Math.PI / 2);
        arc(c, L, W, 1f, (float) Math.PI, (float) Math.PI / 2); arc(c, 0, W, 1f, (float) Math.PI * 1.5f, (float) Math.PI / 2);
    }

    void arc(Canvas c, float cx, float cy, float r, float a0, float sweep) {
        int n = Math.max(6, (int) (Math.abs(sweep) * 10));
        path2.reset();
        for (int i = 0; i <= n; i++) {
            float a = a0 + sweep * i / n;
            proj(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0);
            if (i == 0) path2.moveTo(px, py); else path2.lineTo(px, py);
        }
        c.drawPath(path2, stroke);
    }

    void dot(Canvas c, float x, float y, float r) {
        proj(x, y, 0);
        fill.setColor(0xDDFFFFFF);
        rect.set(px - ps * r * 1.5f, py - ps * r * 0.8f, px + ps * r * 1.5f, py + ps * r * 0.8f);
        c.drawOval(rect, fill);
    }

    // ---------------------------------------------------------------- boards
    static final String[] SP = {"YOTTA", "NOVA BANK", "AEROLINE", "VOLT", "TERRA", "PULSE", "ZENITH", "ORBIT"};
    void drawBoards(Canvas c) {
        float y = -1.6f, h = 0.95f;
        float x0 = -8, x1 = L + 8;
        int segs = 14;
        float segW = (x1 - x0) / segs;
        for (int i = 0; i < segs; i++) {
            float a = x0 + i * segW, b = a + segW;
            proj(a, y, 0); float ax = px, ay = py;
            proj(b, y, h); float bx = px, by = py;
            int[] cols = {0xFF0F172A, 0xFF1D4ED8, 0xFF0F172A, 0xFFB91C1C, 0xFF0F172A, 0xFF059669, 0xFF0F172A, 0xFF7C3AED};
            fill.setColor(cols[(i + (int) (time * 0.4f)) % cols.length]);
            c.drawRect(ax, by, bx, ay, fill);
            fill.setColor(0xFFFFFFFF);
            text.setTextSize(Math.max(8, (ay - by) * 0.55f));
            text.setColor(0xFFFFFFFF);
            c.drawText(SP[(i * 3 + (int) (time * 0.4f)) % SP.length], (ax + bx) / 2, by + (ay - by) * 0.72f, text);
            fill.setColor(0x55000000);
            c.drawRect(ax, ay - 3, bx, ay, fill);
        }
    }

    // ---------------------------------------------------------------- goals
    void drawGoal(Canvas c, int end, boolean front) {
        float lx = end == 0 ? 0 : L;
        float dir = end == 0 ? -1 : 1; // net extends outward
        float d = 2.2f;
        float y0 = Match.CY - Match.GH, y1 = Match.CY + Match.GH, h = Match.GOAL_H;
        if (!front) {
            // net
            stroke.setColor(0x66FFFFFF);
            stroke.setStrokeWidth(Math.max(1f, ps * 0.02f));
            // back plane grid
            for (int i = 0; i <= 12; i++) {
                float y = y0 + (y1 - y0) * i / 12f;
                proj(lx + dir * d, y, 0); float a = px, b = py;
                proj(lx + dir * d, y, h * 0.85f);
                c.drawLine(a, b, px, py, stroke);
            }
            for (int j = 0; j <= 6; j++) {
                float z = h * 0.85f * j / 6f;
                proj(lx + dir * d, y0, z); float a = px, b = py;
                proj(lx + dir * d, y1, z);
                c.drawLine(a, b, px, py, stroke);
            }
            // top net
            for (int i = 0; i <= 6; i++) {
                float y = y0 + (y1 - y0) * i / 6f;
                proj(lx, y, h); float a = px, b = py;
                proj(lx + dir * d, y, h * 0.85f);
                c.drawLine(a, b, px, py, stroke);
            }
            // side nets
            for (int s = 0; s < 2; s++) {
                float y = s == 0 ? y0 : y1;
                for (int i = 0; i <= 4; i++) {
                    float dd = d * i / 4f;
                    proj(lx + dir * dd, y, 0); float a = px, b = py;
                    proj(lx + dir * dd, y, h * (1 - 0.15f * i / 4f));
                    c.drawLine(a, b, px, py, stroke);
                }
            }
            // far post & back posts
            stroke.setColor(0xFFE5E7EB);
            stroke.setStrokeWidth(Math.max(2f, ps * 0.12f));
            proj(lx, y0, 0); float a = px, b = py;
            proj(lx, y0, h);
            c.drawLine(a, b, px, py, stroke);
            proj(lx + dir * d, y0, 0); a = px; b = py;
            proj(lx + dir * d, y0, h * 0.85f);
            c.drawLine(a, b, px, py, stroke);
            proj(lx + dir * d, y1, 0); a = px; b = py;
            proj(lx + dir * d, y1, h * 0.85f);
            c.drawLine(a, b, px, py, stroke);
            proj(lx + dir * d, y0, h * 0.85f); a = px; b = py;
            proj(lx + dir * d, y1, h * 0.85f);
            c.drawLine(a, b, px, py, stroke);
        } else {
            stroke.setColor(0xFFFFFFFF);
            stroke.setStrokeWidth(Math.max(2.5f, ps * 0.14f));
            proj(lx, y1, 0); float a = px, b = py;
            proj(lx, y1, h);
            c.drawLine(a, b, px, py, stroke);
            a = px; b = py;
            proj(lx, y0, h);
            c.drawLine(a, b, px, py, stroke);
        }
    }

    // ---------------------------------------------------------------- lights
    void drawLights(Canvas c) {
        if (tod == 0) return;
        float a = tod == 2 ? 1f : 0.5f;
        float[][] pos = {{-12, -26}, {L + 12, -26}};
        for (float[] p : pos) {
            proj(p[0], p[1], 22);
            fill.setShader(new RadialGradient(px, py, vw * 0.14f, U.alpha(0xFFFFFFFF, (int) (200 * a)), 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawCircle(px, py, vw * 0.14f, fill);
            fill.setShader(null);
            fill.setColor(0xFFFFFFFF);
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) c.drawCircle(px - 20 + i * 20, py - 20 + j * 14, 5, fill);
        }
    }

    void drawRain(Canvas c, float t) {
        stroke.setColor(0x66CFE8FF);
        stroke.setStrokeWidth(2f);
        java.util.Random r = new java.util.Random(3);
        for (int i = 0; i < 160; i++) {
            float x = r.nextInt(vw + 200) - 100, sp = 900 + r.nextInt(600), y0 = r.nextInt(vh);
            float y = (y0 + t * sp) % (vh + 80) - 40;
            float xx = x - (y * 0.15f);
            c.drawLine(xx, y, xx - 6, y + 24, stroke);
        }
    }

    // ---------------------------------------------------------------- aim overlay
    void drawAimOverlay(Canvas c, Match m) {
        if (m.mode != Match.M_SET || !m.spHuman || m.autoPlay) return;
        if (m.spType == Match.SP_FREEKICK || m.spType == Match.SP_PENALTY || m.spType == Match.SP_CORNER) {
            float ax = m.spAimX, ay = m.spAimY;
            proj(ax, ay, m.spType == Match.SP_CORNER ? 0 : 1.2f);
            float pulse = 1 + 0.15f * (float) Math.sin(time * 8);
            stroke.setColor(0xFFFFD54A);
            stroke.setStrokeWidth(4);
            float r = ps * (m.spType == Match.SP_CORNER ? 1.5f : 0.55f) * pulse;
            c.drawCircle(px, py, r, stroke);
            c.drawLine(px - r * 1.4f, py, px + r * 1.4f, py, stroke);
            c.drawLine(px, py - r * 1.4f, px, py + r * 1.4f, stroke);
        }
    }
}
