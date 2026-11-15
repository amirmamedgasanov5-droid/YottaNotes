package com.yotta.fc;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** Draws players, officials and the ball as animated procedural figures. */
final class Figures {
    final Renderer R;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final RectF rc = new RectF();
    final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final int[] BOOTS = {0xFF111111, 0xFFF8FAFC, 0xFFFB923C, 0xFF38BDF8, 0xFFA3E635, 0xFFF472B6};
    // actor buffers
    final float[] key = new float[40];
    final int[] kind = new int[40];  // 0 pawn,1 official,2 ball
    final int[] idx = new int[40];
    final int[] order = new int[40];
    Pawn marked, target;
    float markT;
    // sphere patch directions
    static final float[][] PATCH = {{1, 1, 1}, {-1, 1, 1}, {1, -1, 1}, {-1, -1, 1}, {1, 1, -1}, {-1, 1, -1}, {1, -1, -1}, {-1, -1, -1}, {0, 0, 1.6f}, {0, 0, -1.6f}, {1.6f, 0, 0}, {-1.6f, 0, 0}};
    // ball trail
    final float[] trx = new float[8], tryy = new float[8];
    int trN;

    Figures(Renderer r) {
        R = r;
        p.setStrokeCap(Paint.Cap.ROUND);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
    }

    void drawActors(Canvas c, Match m) {
        int n = 0;
        for (int i = 0; i < 22; i++) {
            Pawn pw = m.all[i];
            if (pw.st == Pawn.SENT) continue;
            key[n] = pw.y; kind[n] = 0; idx[n] = i; n++;
        }
        for (int i = 0; i < m.officials.size(); i++) { Official o = m.officials.get(i); key[n] = o.y; kind[n] = 1; idx[n] = i; n++; }
        key[n] = m.by + 0.05f; kind[n] = 2; idx[n] = 0; n++;
        for (int i = 0; i < n; i++) order[i] = i;
        for (int i = 1; i < n; i++) {
            int v = order[i]; float k = key[v]; int j = i - 1;
            while (j >= 0 && key[order[j]] > k) { order[j + 1] = order[j]; j--; }
            order[j + 1] = v;
        }
        // shadows first
        for (int i = 0; i < n; i++) {
            int a = order[i];
            if (kind[a] == 0) { Pawn pw = m.all[idx[a]]; shadow(c, pw.x, pw.y, pw.h * 0.28f, pw.st == Pawn.SLIDE || pw.st == Pawn.FALL || pw.st == Pawn.DOWN || pw.st == Pawn.DIVE ? 1.8f : 1f, pw.z); }
            else if (kind[a] == 1) { Official o = m.officials.get(idx[a]); shadow(c, o.x, o.y, 0.45f, 1f, 0); }
        }
        // ball shadow
        R.proj(m.bx, m.by, 0);
        sp.setStyle(Paint.Style.FILL);
        sp.setColor(U.alpha(0xFF000000, (int) (90 / (1f + m.bz * 0.5f))));
        float sr = R.ps * 0.22f / (1f + m.bz * 0.25f);
        rc.set(R.px - sr, R.py - sr * 0.45f, R.px + sr, R.py + sr * 0.45f);
        c.drawOval(rc, sp);
        for (int i = 0; i < n; i++) {
            int a = order[i];
            if (kind[a] == 0) {
                Pawn pw = m.all[idx[a]];
                if (pw == marked) drawRing(c, pw, m);
                drawPawn(c, m, pw);
            } else if (kind[a] == 1) drawOfficial(c, m.officials.get(idx[a]));
            else drawBall(c, m);
        }
        if (marked != null && marked.st != Pawn.SENT) drawMarker(c, marked, 0xFFFFD54A, true, m);
        if (target != null && target != marked) drawMarker(c, target, 0xFF4ADE80, false, m);
    }

    void shadow(Canvas c, float x, float y, float r, float stretch, float z) {
        R.proj(x, y, 0);
        float rx = R.ps * r * 1.15f * stretch, ry = R.ps * r * 0.5f;
        sp.setColor(U.alpha(0xFF000000, (int) (95 / (1f + z))));
        rc.set(R.px - rx, R.py - ry, R.px + rx, R.py + ry);
        c.drawOval(rc, sp);
    }

    void drawRing(Canvas c, Pawn pw, Match m) {
        R.proj(pw.x, pw.y, 0);
        float pulse = 1 + 0.08f * (float) Math.sin(R.time * 7);
        float rx = R.ps * 0.85f * pulse, ry = R.ps * 0.4f * pulse;
        R.stroke.setStrokeWidth(Math.max(2, R.ps * 0.08f));
        R.stroke.setColor(0xFFFFD54A);
        rc.set(R.px - rx, R.py - ry, R.px + rx, R.py + ry);
        c.drawOval(rc, R.stroke);
    }

    void drawMarker(Canvas c, Pawn pw, int col, boolean named, Match m) {
        R.proj(pw.x, pw.y, pw.z + pw.h + 0.55f);
        float bob = (float) Math.sin(R.time * 6) * R.ps * 0.06f;
        float s = R.ps * 0.3f;
        path.reset();
        path.moveTo(R.px, R.py + bob + s * 0.2f);
        path.lineTo(R.px - s, R.py + bob - s * 1.1f);
        path.lineTo(R.px + s, R.py + bob - s * 1.1f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawPath(path, p);
        if (named) {
            tp.setTextSize(Math.max(14, R.ps * 0.42f));
            tp.setColor(0xFFFFFFFF);
            tp.setShadowLayer(4, 0, 2, 0xFF000000);
            c.drawText(pw.pl.last.toUpperCase(), R.px, R.py + bob - s * 1.5f, tp);
            tp.clearShadowLayer();
        }
    }

    // =============================================================== ball
    void drawBall(Canvas c, Match m) {
        float rad = 0.30f;
        R.proj(m.bx, m.by, m.bz + 0.11f);
        float cx = R.px, cy = R.py, r = Math.max(5f, R.ps * rad);
        // trail
        float spd = m.ballSpeed();
        if (spd > 14) {
            R.stroke.setColor(0x55FFFFFF);
            for (int i = 1; i < 4; i++) {
                float k = i * 0.012f;
                R.proj(m.bx - m.bvx * k * 3, m.by - m.bvy * k * 3, m.bz + 0.11f - m.bvz * k * 3);
                R.stroke.setStrokeWidth(r * (1.5f - i * 0.35f));
                R.stroke.setColor(U.alpha(0xFFFFFFFF, 70 - i * 18));
                c.drawLine(cx, cy, R.px, R.py, R.stroke);
            }
        }
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.4f, r * 1.4f, 0xFFFFFFFF, 0xFFB8BEC8, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        // rolling patches
        float a = m.brx, b = m.bry;
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a), cb = (float) Math.cos(b), sb = (float) Math.sin(b);
        p.setColor(0xFF22262E);
        for (float[] v : PATCH) {
            float x = v[0], y = v[1], z = v[2];
            float n = (float) Math.sqrt(x * x + y * y + z * z);
            x /= n; y /= n; z /= n;
            // rotate about y by a, then x by b
            float x1 = x * ca + z * sa, z1 = -x * sa + z * ca;
            float y1 = y * cb - z1 * sb, z2 = y * sb + z1 * cb;
            if (z2 > 0.12f) {
                float rr = r * 0.34f * (0.5f + z2 * 0.5f);
                rc.set(cx + x1 * r * 0.72f - rr, cy + y1 * r * 0.72f - rr * z2, cx + x1 * r * 0.72f + rr, cy + y1 * r * 0.72f + rr * z2);
                c.drawOval(rc, p);
            }
        }
        R.stroke.setColor(0x55000000);
        R.stroke.setStrokeWidth(1.2f);
        c.drawCircle(cx, cy, r, R.stroke);
    }

    // =============================================================== pawn
    void drawPawn(Canvas c, Match m, Pawn pw) {
        Match.Side s = m.S[pw.side];
        boolean gk = pw.role == Player.GK;
        int c1 = gk ? s.gk1 : s.c1, c2 = gk ? s.gk2 : s.c2, c3 = gk ? 0xFF111111 : s.c3;
        R.proj(pw.x, pw.y, pw.z);
        float u = pw.h * R.ps;
        int shorts = gk ? U.shade(s.gk2, 0.8f) : (U.colorDist(s.c3, s.c1) > 70 ? s.c3 : U.shade(s.c1, 0.55f));
        if (!gk && s.pattern == 3 && U.colorDist(s.c2, s.c1) > 50) shorts = s.c2;
        if (!gk && U.colorDist(shorts, s.c1) < 40) shorts = U.colorDist(s.c1, 0xFFFFFFFF) < 80 ? 0xFF1B1B1B : 0xFFFFFFFF;
        int socks = gk ? c1 : (s.pattern == 0 || s.pattern == 3 ? s.c1 : s.c2);
        float speed = pw.speed();
        int stt = pw.st;
        int arms = 0;
        if (stt == Pawn.CELEB) arms = 2;
        else if (stt == Pawn.JUMP) arms = 1;
        else if (stt == Pawn.THROW) arms = 3;
        else if (stt == Pawn.DIVE) arms = 4;
        float rot = 0, drop = 0;
        float cf = pw.fx;
        float sgn = cf >= 0 ? 1 : -1;
        if (stt == Pawn.SLIDE) { rot = -sgn * 78; drop = 0.43f; speed = 0; }
        else if (stt == Pawn.FALL || stt == Pawn.DOWN) { float k = U.smooth(pw.stT / 0.25f); rot = -sgn * 88 * k; drop = 0.45f * k; speed = 0; }
        else if (stt == Pawn.GETUP) { float k = 1 - U.smooth(pw.stT / 0.5f); rot = -sgn * 88 * k; drop = 0.45f * k; speed = 0; }
        else if (stt == Pawn.DIVE) {
            float k = U.smooth(pw.stT / 0.25f);
            float dsgn = pw.fy >= 0 ? 1 : -1;
            rot = -(s.dir > 0 ? 1 : -1) * dsgn * 72 * k; drop = 0.22f * k - 0.08f * (float) Math.sin(U.clamp(pw.stT / 0.9f, 0, 1) * U.PI) * 0; speed = 0;
        }
        figure(c, R.px, R.py, u, pw.fx, pw.fy, speed, pw.phase, pw.kickT, stt, pw.stT, c1, c2, c3, s.pattern, shorts, socks,
                pw.pl.skin, pw.pl.hairStyle, pw.pl.hairColor, pw.pl.beard, pw.pl.number, gk, arms, rot, drop, BOOTS[pw.pl.id % BOOTS.length], pw.pl.height, 0, 0);
    }

    void drawOfficial(Canvas c, Official o) {
        R.proj(o.x, o.y, 0);
        float u = o.h * R.ps;
        int c1, c2, c3, shorts, socks;
        int hold = 0;
        if (o.type == Official.REF) { c1 = 0xFF121212; c2 = 0xFFE8D400; c3 = 0xFF121212; shorts = 0xFF121212; socks = 0xFF121212; if (o.card > 0) hold = o.card == 1 ? 2 : 3; else if (o.armT > 0) hold = 4; }
        else if (o.type == Official.COACH) { c1 = o.side == 0 ? 0xFF1F2937 : 0xFF374151; c2 = 0xFFFFFFFF; c3 = c1; shorts = c1; socks = 0xFF111111; }
        else { c1 = 0xFFF2D100; c2 = 0xFF111111; c3 = 0xFF111111; shorts = 0xFF111111; socks = 0xFF111111; hold = o.flagT > 0 ? 5 : 1; }
        float speed = U.len(o.vx, o.vy);
        figure(c, R.px, R.py, u, o.fx, o.fy, speed, o.phase, 0, 0, 0, c1, c2, c3, o.type == Official.COACH ? 5 : 0, shorts, socks,
                1, o.type == Official.REF ? 1 : 2, 1, o.type == Official.COACH ? 1 : 0, 0, false, hold == 5 ? 5 : (hold == 4 ? 6 : 0), 0, 0, 0xFF111111, 180, hold, o.type);
    }

    // ---------------------------------------------------------------- figure
    final float[] k1 = new float[2];

    void figure(Canvas c, float x, float y, float u, float fx, float fy, float speed, float phase, float kickT, int st, float stT,
                int c1, int c2, int c3, int pattern, int shorts, int socks, int skinI, int hairStyle, int hairColI, int beard, int number,
                boolean gk, int arms, float rot, float drop, int boot, int heightCm, int hold, int otype) {
        int skin = Renderer.SKIN[U.clampi(skinI, 0, 5)];
        int hair = Renderer.HAIR[U.clampi(hairColI, 0, 5)];
        float sf = fy, cf = fx;
        float absF = Math.abs(sf);
        float sgn = cf >= 0 ? 1 : -1;
        float sp = U.clamp(speed / 7.5f, 0, 1.15f);
        float bobT = phase * 2f;
        float bob = sp > 0.05f ? Math.abs((float) Math.sin(phase)) * 0.018f * u * sp : (float) Math.sin(R.time * 2.2f + x) * 0.004f * u;
        c.save();
        c.translate(x, y + drop * u);
        if (rot != 0) c.rotate(rot, 0, -0.5f * u);
        float hipY = -0.50f * u - bob, shY = -0.80f * u - bob, headY = -0.915f * u - bob, headR = 0.072f * u;
        float shw = u * (0.058f + 0.055f * absF), hpw = u * (0.040f + 0.040f * absF);
        float lean = U.clamp(sp, 0, 1) * 0.035f * u * sgn * (1 - absF * 0.6f);
        p.setStyle(Paint.Style.FILL);
        // ---- feet
        float A = 0.30f * u * sp;
        float[] fxs = new float[2], fys = new float[2], lifts = new float[2];
        for (int i = 0; i < 2; i++) {
            float ph = phase + i * U.PI;
            float swing = (float) Math.sin(ph) * A;
            float lift = Math.max(0, (float) Math.cos(ph)) * 0.13f * u * sp;
            float lat = (i == 0 ? -1 : 1) * 0.045f * u;
            float lx = -sf * lat * 1.3f / 1f, ly = cf * lat * 0.5f;
            fxs[i] = (cf * swing + lx) + (i == 0 ? -1 : 1) * hpw * 0.6f * (absF);
            fys[i] = sf * swing * 0.5f + ly - lift;
            lifts[i] = lift;
            if (st == Pawn.SLIDE || st == Pawn.FALL || st == Pawn.DOWN || st == Pawn.GETUP || st == Pawn.DIVE) { fxs[i] = lx; fys[i] = ly; }
            if (st == Pawn.JUMP) { fys[i] = -0.12f * u; fxs[i] = lx + (i == 0 ? 1 : -1) * 0.02f * u; }
            if (st == Pawn.CELEB && (int) (stT * 3) % 2 == 0) { fys[i] = -0.1f * u * (i + 0.2f); }
        }
        if (kickT > 0) {
            float k = 1 - kickT / 0.3f;
            float ext = k < 0.4f ? -0.25f * (k / 0.4f) : (-0.25f + 0.85f * U.smooth((k - 0.4f) / 0.3f));
            fxs[1] = cf * ext * u; fys[1] = sf * ext * u * 0.5f - 0.05f * u * (float) Math.sin(k * U.PI);
        }
        if (st == Pawn.SLIDE) { fxs[1] = cf * 0.3f * u; fys[1] = -0.0f; }
        if (st == Pawn.DIVE) { }
        // draw order of legs: by foot y (farther first)
        int first = fys[0] < fys[1] ? 0 : 1;
        // arms data
        float[] hx = new float[2], hyv = new float[2];
        for (int i = 0; i < 2; i++) {
            float ph = phase + (i == 0 ? U.PI : 0);
            float swing = (float) Math.sin(ph) * 0.24f * u * sp;
            float lat = (i == 0 ? -1 : 1);
            float shx = lat * shw + lean;
            hx[i] = shx + cf * swing * 0.9f + lat * 0.025f * u + (-sf * lat * 0.01f * u);
            hyv[i] = shY + 0.30f * u - Math.abs(swing) * 0.15f + sf * swing * 0.25f;
            if (arms == 1 || arms == 2) { hx[i] = shx + lat * 0.08f * u; hyv[i] = shY - 0.22f * u - (arms == 2 ? 0.05f * u * (float) Math.sin(R.time * 14 + i) : 0); }
            else if (arms == 3) { hx[i] = lean + lat * 0.05f * u; hyv[i] = shY - 0.26f * u; }
            else if (arms == 4) { hx[i] = shx + sgn * 0.3f * u; hyv[i] = shY - 0.28f * u + i * 0.05f * u; }
            else if (gk && st == Pawn.N && speed < 1.5f) { hx[i] = shx + lat * 0.08f * u; hyv[i] = shY + 0.22f * u; }
            if (st == Pawn.SLIDE) { hx[i] = shx - sgn * 0.12f * u; hyv[i] = shY + 0.2f * u; }
        }
        // hold items
        // ---- far leg
        drawLeg(c, first, hpw, hipY, fxs, fys, shorts, socks, skin, boot, u, sf, cf, gk);
        // far arm drawn before torso if fy>=0 side ordering: use arm index 0 as far
        drawArm(c, 0, shw, shY, lean, hx[0], hyv[0], c1, c2, skin, u, gk, pattern, arms);
        // ---- torso
        path.reset();
        float tl = -shw + lean, tr = shw + lean;
        path.moveTo(tl, shY + 0.02f * u);
        path.quadTo(tl - 0.012f * u, shY - 0.01f * u, tl + 0.02f * u, shY - 0.012f * u);
        path.lineTo(tr - 0.02f * u, shY - 0.012f * u);
        path.quadTo(tr + 0.012f * u, shY - 0.01f * u, tr, shY + 0.02f * u);
        path.lineTo(hpw * 1.05f, hipY + 0.03f * u);
        path.lineTo(-hpw * 1.05f, hipY + 0.03f * u);
        path.close();
        p.setColor(c1);
        c.drawPath(path, p);
        c.save();
        c.clipPath(path);
        switch (pattern) {
            case 1: { // vertical stripes
                p.setColor(c2);
                float w = (tr - tl) / 5f;
                for (int i = 0; i < 5; i += 2) c.drawRect(tl + i * w, shY - 0.02f * u, tl + (i + 1) * w, hipY + 0.04f * u, p);
                break;
            }
            case 2: { // hoops
                p.setColor(c2);
                for (float yy = shY + 0.04f * u; yy < hipY; yy += 0.10f * u) c.drawRect(tl, yy, tr, yy + 0.05f * u, p);
                break;
            }
            case 3: { // halves
                p.setColor(c2);
                c.drawRect(lean, shY - 0.02f * u, tr + 0.02f * u, hipY + 0.04f * u, p);
                break;
            }
            case 4: { // sash
                p.setColor(c2);
                path2(tl, shY, tl + (tr - tl) * 0.45f, shY, tr, hipY, tr - (tr - tl) * 0.45f, hipY);
                c.drawPath(path, p);
                break;
            }
            case 5: { // chest band / suit shirt
                p.setColor(c2);
                if (otype == 3) { c.drawRect(lean - 0.02f * u, shY, lean + 0.02f * u, hipY, p); }
                else c.drawRect(tl, shY + 0.07f * u, tr, shY + 0.14f * u, p);
                break;
            }
            default: break;
        }
        // shading
        p.setColor(0x22000000);
        c.drawRect(sgn > 0 ? tl : lean, shY, sgn > 0 ? lean : tr, hipY + 0.04f * u, p);
        c.restore();
        // sleeves/collar & number
        if (sf < -0.25f && number > 0 && otype == 0) {
            tp.setTextSize(Math.max(8, u * 0.17f));
            int nc = U.colorDist(c1, 0xFFFFFFFF) > 140 ? 0xFFFFFFFF : 0xFF111111;
            if (pattern != 0 && U.colorDist(c2, c1) > 120) nc = c2;
            tp.setColor(nc);
            c.drawText(String.valueOf(number), lean * 0.8f, shY + 0.19f * u, tp);
        } else if (sf > 0.3f) {
            // collar V + crest
            p.setColor(skin);
            path.reset(); path.moveTo(lean - 0.03f * u, shY - 0.012f * u); path.lineTo(lean + 0.03f * u, shY - 0.012f * u); path.lineTo(lean, shY + 0.04f * u); path.close();
            c.drawPath(path, p);
            p.setColor(c2); rc.set(lean + 0.03f * u, shY + 0.045f * u, lean + 0.055f * u, shY + 0.07f * u); c.drawOval(rc, p);
        } else if (otype == 3) {
            p.setColor(0xFFFFFFFF);
            path.reset(); path.moveTo(lean - 0.025f * u, shY - 0.012f * u); path.lineTo(lean + 0.025f * u, shY - 0.012f * u); path.lineTo(lean, shY + 0.05f * u); path.close();
            c.drawPath(path, p);
        }
        // shorts
        p.setColor(shorts);
        path.reset();
        path.moveTo(-hpw * 1.12f, hipY - 0.02f * u);
        path.lineTo(hpw * 1.12f, hipY - 0.02f * u);
        path.lineTo(hpw * 1.25f, hipY + 0.11f * u);
        path.lineTo(-hpw * 1.25f, hipY + 0.11f * u);
        path.close();
        c.drawPath(path, p);
        // near leg
        drawLeg(c, 1 - first, hpw, hipY, fxs, fys, shorts, socks, skin, boot, u, sf, cf, gk);
        // neck + head
        p.setColor(U.shade(skin, 0.9f));
        c.drawRect(lean * 1.1f - 0.025f * u, shY - 0.05f * u, lean * 1.1f + 0.025f * u, shY + 0.01f * u, p);
        float hxc = lean * 1.25f + cf * 0.008f * u;
        drawHead(c, hxc, headY, headR, skin, hair, hairStyle, beard, cf, sf, u, otype);
        // near arm
        drawArm(c, 1, shw, shY, lean, hx[1], hyv[1], c1, c2, skin, u, gk, pattern, arms);
        // held items
        if (hold != 0) drawHold(c, hold, hx[1], hyv[1], u, sgn);
        c.restore();
    }

    void path2(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        path.reset(); path.moveTo(x0, y0); path.lineTo(x1, y1); path.lineTo(x2, y2); path.lineTo(x3, y3); path.close();
    }

    void drawHold(Canvas c, int hold, float hx, float hy, float u, float sgn) {
        switch (hold) {
            case 2: p.setColor(0xFFFFD400); rc.set(hx - 0.03f * u, hy - 0.11f * u, hx + 0.03f * u, hy - 0.03f * u); c.drawRect(rc, p); break;
            case 3: p.setColor(0xFFE11D2E); rc.set(hx - 0.03f * u, hy - 0.11f * u, hx + 0.03f * u, hy - 0.03f * u); c.drawRect(rc, p); break;
            case 5: case 1: {
                R.stroke.setColor(0xFF222222); R.stroke.setStrokeWidth(Math.max(1.5f, u * 0.012f));
                float fx0 = hx, fy0 = hy;
                if (hold == 5) { fy0 = hy - 0.3f * u; }
                c.drawLine(hx, hy, fx0 + 0.02f * u, fy0 - 0.17f * u, R.stroke);
                p.setColor(hold == 5 ? 0xFFFFD400 : 0xFFE11D2E);
                rc.set(fx0 + 0.02f * u, fy0 - 0.2f * u, fx0 + 0.02f * u + sgn * 0.11f * u + (sgn < 0 ? 0 : 0), fy0 - 0.12f * u);
                if (sgn < 0) { rc.left = fx0 + 0.02f * u - 0.11f * u; rc.right = fx0 + 0.02f * u; }
                c.drawRect(rc, p);
                p.setColor(0xFF111111); c.drawRect(rc.left, rc.top, rc.left + (rc.right - rc.left) * 0.5f, rc.bottom, p);
                break;
            }
            default: break;
        }
    }

    void drawLeg(Canvas c, int i, float hpw, float hipY, float[] fxs, float[] fys, int shorts, int socks, int skin, int boot, float u, float sf, float cf, boolean gk) {
        float hx = (i == 0 ? -1 : 1) * hpw * 0.65f;
        float fx = hx + fxs[i], fy = fys[i] - 0.015f * u;
        float dx = fx - hx, dy = fy - hipY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        float seg = 0.255f * u;
        if (d > seg * 1.98f) { float k = seg * 1.98f / d; fx = hx + dx * k; fy = hipY + dy * k; dx *= k; dy *= k; d = seg * 1.98f; }
        float mx = hx + dx / 2, my = hipY + dy / 2;
        float h = (float) Math.sqrt(Math.max(0, seg * seg - d * d / 4f));
        float nx = -dy / Math.max(d, 0.001f), ny = dx / Math.max(d, 0.001f);
        float kx1 = mx + nx * h, ky1 = my + ny * h, kx2 = mx - nx * h, ky2 = my - ny * h;
        boolean pick1 = Math.abs(cf) > 0.3f ? (kx1 - mx) * cf > (kx2 - mx) * cf : ky1 > ky2;
        float kx = pick1 ? kx1 : kx2, ky = pick1 ? ky1 : ky2;
        R.stroke.setStrokeCap(Paint.Cap.ROUND);
        // thigh
        R.stroke.setStrokeWidth(0.105f * u);
        R.stroke.setColor(skin);
        c.drawLine(hx, hipY, kx, ky, R.stroke);
        // shorts overlay
        R.stroke.setColor(shorts);
        R.stroke.setStrokeWidth(0.115f * u);
        c.drawLine(hx, hipY + 0.01f * u, hx + (kx - hx) * 0.5f, hipY + (ky - hipY) * 0.5f, R.stroke);
        // shin sock
        R.stroke.setStrokeWidth(0.075f * u);
        R.stroke.setColor(skin);
        c.drawLine(kx, ky, fx, fy - 0.035f * u, R.stroke);
        R.stroke.setColor(socks);
        c.drawLine(kx + (fx - kx) * 0.28f, ky + (fy - ky) * 0.28f, fx, fy - 0.04f * u, R.stroke);
        // boot
        p.setColor(boot);
        float bl = 0.075f * u, bw = 0.04f * u;
        float bx = fx + (cf >= 0 ? 1 : -1) * 0.035f * u * (1 - Math.abs(sf) * 0.7f);
        rc.set(bx - bl, fy - bw, bx + bl, fy + 0.02f * u);
        c.drawOval(rc, p);
    }

    void drawArm(Canvas c, int i, float shw, float shY, float lean, float hx, float hy, int c1, int c2, int skin, float u, boolean gk, int pattern, int arms) {
        float sx = (i == 0 ? -1 : 1) * shw + lean;
        float sy = shY + 0.015f * u;
        float dx = hx - sx, dy = hy - sy;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        float seg = 0.150f * u;
        if (d > seg * 1.97f) { float k = seg * 1.97f / d; hx = sx + dx * k; hy = sy + dy * k; dx *= k; dy *= k; d = seg * 1.97f; }
        float mx = sx + dx / 2, my = sy + dy / 2;
        float h = (float) Math.sqrt(Math.max(0, seg * seg - d * d / 4f));
        float nx = -dy / Math.max(d, 0.001f), ny = dx / Math.max(d, 0.001f);
        float ex = mx + nx * h * (i == 0 ? 1 : -1) * 0.5f, ey = my + ny * h * (i == 0 ? 1 : -1) * 0.5f;
        // choose elbow outward-down
        if (ey < my - 0.01f * u && arms == 0) { ex = mx - nx * h * 0.5f * (i == 0 ? 1 : -1); ey = my - ny * h * 0.5f * (i == 0 ? 1 : -1); }
        R.stroke.setStrokeCap(Paint.Cap.ROUND);
        R.stroke.setStrokeWidth(0.062f * u);
        R.stroke.setColor(skin);
        c.drawLine(ex, ey, hx, hy, R.stroke);
        R.stroke.setColor(c1);
        R.stroke.setStrokeWidth(0.07f * u);
        c.drawLine(sx, sy, ex, ey, R.stroke);
        if (pattern == 2 || pattern == 1 || pattern == 4) { R.stroke.setColor(c2); R.stroke.setStrokeWidth(0.04f * u); c.drawLine(sx + (ex - sx) * 0.6f, sy + (ey - sy) * 0.6f, ex, ey, R.stroke); }
        p.setColor(gk ? 0xFFFDE047 : skin);
        c.drawCircle(hx, hy, gk ? 0.04f * u : 0.03f * u, p);
    }

    void drawHead(Canvas c, float cx, float cy, float r, int skin, int hair, int style, int beard, float cf, float sf, float u, int otype) {
        boolean back = sf < -0.3f, front = sf > 0.3f;
        float sgn = cf >= 0 ? 1 : -1;
        // hair behind head
        p.setColor(hair);
        if (style == 3) { rc.set(cx - r * 1.08f, cy - r * 0.9f, cx + r * 1.08f, cy + r * 1.7f); c.drawOval(rc, p); }
        if (style == 5) c.drawCircle(cx, cy - r * 0.15f, r * 1.55f, p);
        if (style == 7) { rc.set(cx - sgn * r * 1.7f, cy - r * 0.2f, cx - sgn * r * 0.6f, cy + r * 0.9f); if (!front) c.drawOval(rc, p); }
        // head
        p.setColor(skin);
        c.drawCircle(cx, cy, r, p);
        if (!back) { // ears
            p.setColor(U.shade(skin, 0.88f));
            if (front) { c.drawCircle(cx - r * 0.98f, cy + r * 0.1f, r * 0.2f, p); c.drawCircle(cx + r * 0.98f, cy + r * 0.1f, r * 0.2f, p); }
            else c.drawCircle(cx - sgn * r * 0.2f, cy + r * 0.1f, r * 0.22f, p);
        }
        // hair cap
        p.setColor(hair);
        if (otype == 3 && style == 2) style = 7;
        switch (style) {
            case 0: break;
            case 1: p.setAlpha(120); rc.set(cx - r, cy - r, cx + r, cy + r * 0.2f); c.drawArc(rc, 180, 180, true, p); p.setAlpha(255); break;
            case 2: case 3: case 7: {
                float k = back ? 1.05f : 0.35f;
                rc.set(cx - r * 1.04f, cy - r * 1.05f, cx + r * 1.04f, cy + r * 0.95f);
                if (back) c.drawCircle(cx, cy, r * 1.03f, p);
                else c.drawArc(rc, 180, 180, true, p);
                if (!back && !front) { rc.set(cx - sgn * r * 1.05f - r * 0.35f, cy - r * 0.6f, cx - sgn * r * 1.05f + r * 0.35f, cy + r * 0.4f); c.drawOval(rc, p); }
                break;
            }
            case 4: rc.set(cx - r * 0.25f, cy - r * 1.45f, cx + r * 0.25f, cy - r * 0.2f); c.drawOval(rc, p); if (back) c.drawCircle(cx, cy, r * 1.0f, p); break;
            case 5: if (back) c.drawCircle(cx, cy, r * 1.2f, p); else { rc.set(cx - r * 1.35f, cy - r * 1.5f, cx + r * 1.35f, cy - r * 0.1f); c.drawArc(rc, 180, 180, true, p); } break;
            case 6: for (int i = 0; i < 6; i++) c.drawCircle(cx - r * 0.9f + i * r * 0.36f, cy - r * (0.75f + (i % 2) * 0.12f), r * 0.34f, p); if (back) c.drawCircle(cx, cy, r * 1.0f, p); break;
            default: break;
        }
        if (otype == 1) { // ref cap-less; nothing
        }
        // face
        if (front) {
            p.setColor(0xFF1A1A1A);
            c.drawCircle(cx - r * 0.38f, cy + r * 0.05f, r * 0.12f, p);
            c.drawCircle(cx + r * 0.38f, cy + r * 0.05f, r * 0.12f, p);
            R.stroke.setStrokeWidth(Math.max(1f, r * 0.1f)); R.stroke.setColor(U.shade(skin, 0.6f));
            c.drawLine(cx - r * 0.2f, cy + r * 0.5f, cx + r * 0.2f, cy + r * 0.5f, R.stroke);
        } else if (!back) {
            p.setColor(0xFF1A1A1A);
            c.drawCircle(cx + sgn * r * 0.42f, cy + r * 0.02f, r * 0.12f, p);
            p.setColor(U.shade(skin, 0.92f));
            c.drawCircle(cx + sgn * r * 1.0f, cy + r * 0.25f, r * 0.14f, p);
        }
        if (beard == 1 && !back) {
            p.setColor(hair); p.setAlpha(210);
            rc.set(cx - r * 0.95f, cy - r * 0.1f, cx + r * 0.95f, cy + r * 1.0f);
            c.drawArc(rc, 10, 160, true, p);
            p.setAlpha(255);
        }
    }
}
