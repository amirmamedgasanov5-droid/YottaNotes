package com.yotta.fc;

import java.util.ArrayList;
import java.util.Random;

/** Builds and draws the 3D stadium, pitch, players and ball through a Gfx backend. */
final class Scene3 {
    static final float L = Match.L, W = Match.W, CY = Match.CY, GH = Match.GH, GOAL_H = Match.GOAL_H;
    final Gfx g;
    // shared unit meshes
    int hBox, hCyl, hCylCap, hSphere, hBall, hDisc;
    // world
    int hGround, hStripeA, hStripeB, hLines, hGoalFrame, hGoalNet, hStandStatic, hBoards, hTowers, hLamps;
    int[] hSky = new int[3];
    int[][] crowd = new int[3][];       // far, left, right
    long builtFor = -1;
    final float[] vp = new float[16];
    final float[] ident = new float[16];
    final float[] root = new float[16], a = new float[16], b = new float[16], c = new float[16], d = new float[16], e = new float[16], t1 = new float[16];
    final float[] mm = new float[16];
    static final int[] SKIN = {0xFFF6D2B8, 0xFFE8B896, 0xFFD0976B, 0xFFB07048, 0xFF8A5230, 0xFF5E3A22};
    static final int[] HAIR = {0xFF17171B, 0xFF3A2314, 0xFF6B4423, 0xFFD8B25A, 0xFFA8431F, 0xFFBDBDBD};
    static final int[] BOOT = {0xFF111111, 0xFFF8FAFC, 0xFFFB923C, 0xFF38BDF8, 0xFFA3E635, 0xFFF472B6};

    Scene3(Gfx g) { this.g = g; }

    int up(Mesh3 m) { return g.upload(m.verts(), m.indices()); }

    void init() {
        M4.identity(ident);
        hBox = up(new Mesh3().box(0, 0, 0, 1, 1, 1, 0xFFFFFFFF));
        hCyl = up(new Mesh3().cylDown(1, 1, 10, 0xFFFFFFFF, false));
        hCylCap = up(new Mesh3().cylDown(1, 0.82f, 10, 0xFFFFFFFF, true));
        hSphere = up(new Mesh3().sphere(1, 10, 14, 0xFFFFFFFF));
        hBall = up(new Mesh3().sphere(0, 0, 0, 1, 12, 20, 0xFFFFFFFF, new int[1]));
        hDisc = up(new Mesh3().disc(1, 18, 0.025f, 0xFFFFFFFF));
        buildWorld();
    }

    // ------------------------------------------------------------------ world
    void buildWorld() {
        // ground
        Mesh3 m = new Mesh3();
        m.box((L) / 2, -0.06f, W / 2, 400, 0.1f, 300, 0xFF2B6E32);
        hGround = up(m);
        Mesh3 sa = new Mesh3(), sb = new Mesh3();
        int n = 21;
        for (int i = 0; i < n; i++) {
            float x0 = -5 + i * (L + 10) / n, x1 = -5 + (i + 1) * (L + 10) / n;
            (i % 2 == 0 ? sa : sb).flat(x0, -5, x1, W + 5, 0.0f, 0xFFFFFFFF);
        }
        hStripeA = up(sa); hStripeB = up(sb);
        // lines
        Mesh3 ln = new Mesh3();
        float y = 0.012f, w = 0.13f;
        lineRect(ln, 0, 0, L, W, w, y);
        seg(ln, L / 2, 0, L / 2, W, w, y);
        circle(ln, L / 2, CY, 9.15f, 0, 360, w, y);
        spot(ln, L / 2, CY, y);
        for (int e = 0; e < 2; e++) {
            float s = e == 0 ? 1 : -1, x0 = e == 0 ? 0 : L;
            lineRect(ln, Math.min(x0, x0 + s * 16.5f), CY - 20.16f, Math.max(x0, x0 + s * 16.5f), CY + 20.16f, w, y);
            lineRect(ln, Math.min(x0, x0 + s * 5.5f), CY - 9.16f, Math.max(x0, x0 + s * 5.5f), CY + 9.16f, w, y);
            spot(ln, x0 + s * 11, CY, y);
            circle(ln, x0 + s * 11, CY, 9.15f, e == 0 ? -53 : 127, e == 0 ? 53 : 233, w, y);
        }
        circle(ln, 0, 0, 1, 0, 90, w, y); circle(ln, L, 0, 1, 90, 180, w, y); circle(ln, L, W, 1, 180, 270, w, y); circle(ln, 0, W, 1, 270, 360, w, y);
        hLines = up(ln);
        // goals
        Mesh3 gf = new Mesh3(), gn = new Mesh3();
        for (int e = 0; e < 2; e++) {
            float x0 = e == 0 ? 0 : L, dir = e == 0 ? -1 : 1, dpt = 2.2f;
            float z0 = CY - GH, z1 = CY + GH;
            gf.cylBetween(x0, 0, z0, x0, GOAL_H, z0, 0.065f, 8, 0xFFFFFFFF);
            gf.cylBetween(x0, 0, z1, x0, GOAL_H, z1, 0.065f, 8, 0xFFFFFFFF);
            gf.cylBetween(x0, GOAL_H, z0, x0, GOAL_H, z1, 0.065f, 8, 0xFFFFFFFF);
            gf.cylBetween(x0 + dir * dpt, 0, z0, x0 + dir * dpt, GOAL_H * 0.85f, z0, 0.04f, 6, 0xFFDDDDDD);
            gf.cylBetween(x0 + dir * dpt, 0, z1, x0 + dir * dpt, GOAL_H * 0.85f, z1, 0.04f, 6, 0xFFDDDDDD);
            gf.cylBetween(x0 + dir * dpt, GOAL_H * 0.85f, z0, x0 + dir * dpt, GOAL_H * 0.85f, z1, 0.04f, 6, 0xFFDDDDDD);
            gf.cylBetween(x0, GOAL_H, z0, x0 + dir * dpt, GOAL_H * 0.85f, z0, 0.04f, 6, 0xFFDDDDDD);
            gf.cylBetween(x0, GOAL_H, z1, x0 + dir * dpt, GOAL_H * 0.85f, z1, 0.04f, 6, 0xFFDDDDDD);
            // net grid
            float nr = 0.012f;
            for (int i = 0; i <= 24; i++) { float z = z0 + (z1 - z0) * i / 24f; gn.cylBetween(x0 + dir * dpt, 0, z, x0 + dir * dpt, GOAL_H * 0.85f, z, nr, 3, 0xFFFFFFFF); gn.cylBetween(x0, GOAL_H, z, x0 + dir * dpt, GOAL_H * 0.85f, z, nr, 3, 0xFFFFFFFF); }
            for (int j = 0; j <= 8; j++) { float yy = GOAL_H * 0.85f * j / 8f; gn.cylBetween(x0 + dir * dpt, yy, z0, x0 + dir * dpt, yy, z1, nr, 3, 0xFFFFFFFF); }
            for (int i = 0; i <= 6; i++) { float xx = x0 + dir * dpt * i / 6f; gn.cylBetween(xx, 0, z0, xx, GOAL_H * (1 - 0.15f * i / 6f), z0, nr, 3, 0xFFFFFFFF); gn.cylBetween(xx, 0, z1, xx, GOAL_H * (1 - 0.15f * i / 6f), z1, nr, 3, 0xFFFFFFFF); }
        }
        hGoalFrame = up(gf); hGoalNet = up(gn);
        // boards
        Mesh3 bd = new Mesh3();
        int[] cols = {0xFF0F172A, 0xFF1D4ED8, 0xFF0F172A, 0xFFB91C1C, 0xFF0F172A, 0xFF059669, 0xFF0F172A, 0xFF7C3AED};
        int k = 0;
        for (float x = -6; x < L + 6; x += 6, k++) {
            bd.box(x + 3, 0.5f, -2.2f, 5.9f, 1.0f, 0.2f, cols[k % cols.length]);
            bd.box(x + 3, 1.02f, -2.2f, 5.9f, 0.06f, 0.22f, 0xFFFFFFFF);
            bd.box(x + 3, 0.5f, W + 2.2f, 5.9f, 1.0f, 0.2f, cols[(k + 3) % cols.length]);
        }
        for (int e = 0; e < 2; e++) {
            float x0 = e == 0 ? -2.6f : L + 2.6f;
            for (float z = -2; z < W + 2; z += 6, k++) bd.box(x0, 0.5f, z + 3, 0.2f, 1.0f, 5.9f, cols[k % cols.length]);
        }
        hBoards = up(bd);
        // stands (static concrete + roof + walls), floodlight towers
        Mesh3 st = new Mesh3();
        int rows = 20;
        for (int r = 0; r < rows; r++) {
            float zr = -7 - r * 1.05f, yr = 0.8f + r * 0.78f;
            int col = (r % 2 == 0) ? 0xFF565B6B : 0xFF4A4F5E;
            st.box(L / 2, yr / 2, zr, L + 30, yr, 1.05f, col);
            for (int e = 0; e < 2; e++) {
                float xr = e == 0 ? -7 - r * 1.05f : L + 7 + r * 1.05f;
                st.box(xr, yr / 2, CY, 1.05f, yr, W + 24, col);
            }
        }
        float topY = 0.8f + rows * 0.78f;
        st.box(L / 2, topY / 2, -7 - rows * 1.05f - 0.8f, L + 60, topY + 14, 1.6f, 0xFF30343F);
        for (int e = 0; e < 2; e++) {
            float xr = e == 0 ? -7 - rows * 1.05f - 0.8f : L + 7 + rows * 1.05f + 0.8f;
            st.box(xr, topY / 2, CY, 1.6f, topY + 14, W + 40, 0xFF30343F);
        }
        // roofs (slanted slabs approximated by boxes) 
        st.box(L / 2, topY + 6.5f, -7 - rows * 0.5f - 4, L + 50, 0.9f, rows * 1.05f + 4, 0xFF20232C);
        st.box(-7 - rows * 0.5f - 4, topY + 6.5f, CY, rows * 1.05f + 4, 0.9f, W + 30, 0xFF20232C);
        st.box(L + 7 + rows * 0.5f + 4, topY + 6.5f, CY, rows * 1.05f + 4, 0.9f, W + 30, 0xFF20232C);
        hStandStatic = up(st);
        Mesh3 tw = new Mesh3(), lamps = new Mesh3();
        float[][] tp = {{-16, -16}, {L + 16, -16}, {-16, W + 16}, {L + 16, W + 16}};
        for (float[] p : tp) {
            tw.cylBetween(p[0], 0, p[1], p[0], 34, p[1], 0.5f, 8, 0xFF8A8F9C);
            tw.box(p[0], 35.5f, p[1], 7, 3.4f, 1.0f, 0xFF2A2D36);
            for (int i = 0; i < 4; i++) for (int j = 0; j < 2; j++) lamps.box(p[0] - 2.4f + i * 1.6f, 35.2f + j * 1.4f - 0.2f, p[1] + (p[1] < 0 ? 0.7f : -0.7f), 1.2f, 1.0f, 0.2f, 0xFFFFFFF0);
        }
        hTowers = up(tw); hLamps = up(lamps);
        // sky
        int[][] sk = {{0xFF3A86E0, 0xFFBFE0FF}, {0xFF262466, 0xFFF48A4E}, {0xFF030510, 0xFF14213D}};
        for (int i = 0; i < 3; i++) {
            Mesh3 s = new Mesh3();
            int a0 = vert(s, -300, 0, -170, sk[i][1]), a1 = vert(s, 400, 0, -170, sk[i][1]), a2 = vert(s, 400, 180, -170, sk[i][0]), a3 = vert(s, -300, 180, -170, sk[i][0]);
            s.quad(a0, a1, a2, a3);
            hSky[i] = up(s);
        }
    }
    int vert(Mesh3 m, float x, float y, float z, int col) { return m.vert(x, y, z, 0, 0, 1, Mesh3.cr(col), Mesh3.cg(col), Mesh3.cb(col)); }

    void seg(Mesh3 m, float x0, float z0, float x1, float z1, float w, float y) {
        float dx = x1 - x0, dz = z1 - z0, l = (float) Math.sqrt(dx * dx + dz * dz);
        float nx = -dz / l * w / 2, nz = dx / l * w / 2;
        int c = 0xFFFFFFFF;
        m.face(x0 - nx, y, z0 - nz, x1 - nx, y, z1 - nz, x1 + nx, y, z1 + nz, x0 + nx, y, z0 + nz, 0, 1, 0, 1, 1, 1);
    }
    void lineRect(Mesh3 m, float x0, float z0, float x1, float z1, float w, float y) {
        seg(m, x0 - w / 2, z0, x1 + w / 2, z0, w, y); seg(m, x1, z0, x1, z1, w, y); seg(m, x1 + w / 2, z1, x0 - w / 2, z1, w, y); seg(m, x0, z1, x0, z0, w, y);
    }
    void circle(Mesh3 m, float cx, float cz, float r, float a0, float a1, float w, float y) {
        int n = Math.max(6, (int) (Math.abs(a1 - a0) / 6));
        for (int i = 0; i < n; i++) {
            float t0 = (float) Math.toRadians(a0 + (a1 - a0) * i / n), t1 = (float) Math.toRadians(a0 + (a1 - a0) * (i + 1) / n);
            seg(m, cx + (float) Math.cos(t0) * r, cz + (float) Math.sin(t0) * r, cx + (float) Math.cos(t1) * r, cz + (float) Math.sin(t1) * r, w, y);
        }
    }
    void spot(Mesh3 m, float x, float z, float y) { m.face(x - 0.2f, y, z + 0.2f, x + 0.2f, y, z + 0.2f, x + 0.2f, y, z - 0.2f, x - 0.2f, y, z - 0.2f, 0, 1, 0, 1, 1, 1); }

    // ------------------------------------------------------------------ crowd (per match)
    void buildCrowd(SceneData d) {
        for (int s = 0; s < 3; s++) if (crowd[s] != null) for (int h : crowd[s]) g.delete(h);
        Random r = new Random(d.matchId * 31 + 7);
        int[] pal = {d.c1[0], d.c1[0], d.c1[0], d.c2[0], d.c1[1], d.c1[1], 0xFFE5E7EB, 0xFF9CA3AF, 0xFF1F2937, 0xFFFBBF24, 0xFFEF4444, 0xFF60A5FA, 0xFF111111, 0xFFFFFFFF};
        int rows = 20;
        for (int s = 0; s < 3; s++) {
            crowd[s] = new int[rows];
            for (int rr = 0; rr < rows; rr++) {
                Mesh3 m = new Mesh3();
                float yr = 0.8f + rr * 0.78f;
                if (s == 0) {
                    float zr = -7 - rr * 1.05f + 0.35f;
                    for (float x = -13; x < L + 14; x += 0.62f) person(m, x + r.nextFloat() * 0.1f, yr, zr, 0, 1, pal, r);
                } else {
                    float xr = s == 1 ? -7 - rr * 1.05f - 0.35f : L + 7 + rr * 1.05f + 0.35f;
                    for (float z = -12; z < W + 12; z += 0.62f) person(m, xr, yr, z + r.nextFloat() * 0.1f, s == 1 ? 1 : -1, 0, pal, r);
                }
                crowd[s][rr] = up(m);
            }
        }
        builtFor = d.matchId;
    }

    void person(Mesh3 m, float x, float y, float z, int dirx, int dirz, int[] pal, Random r) {
        int shirt = pal[r.nextInt(pal.length)], skin = SKIN[r.nextInt(6)], hair = HAIR[r.nextInt(6)];
        float w = 0.42f, h = 0.55f;
        float sh = 0.85f + r.nextFloat() * 0.3f;
        float sr = Mesh3.cr(shirt) * sh, sg = Mesh3.cg(shirt) * sh, sb = Mesh3.cb(shirt) * sh;
        float nx = dirx, nz = dirz == 1 ? 1 : 0;
        if (dirz == 1) { // faces +Z
            int a = m.vert(x - w / 2, y, z, 0, 0, 1, sr, sg, sb), b = m.vert(x + w / 2, y, z, 0, 0, 1, sr, sg, sb), c = m.vert(x + w / 2, y + h, z, 0, 0, 1, sr, sg, sb), d = m.vert(x - w / 2, y + h, z, 0, 0, 1, sr, sg, sb);
            m.quad(a, b, c, d);
            float hw = 0.13f;
            float hr = Mesh3.cr(skin), hg = Mesh3.cg(skin), hb = Mesh3.cb(skin);
            a = m.vert(x - hw, y + h, z + 0.02f, 0, 0, 1, hr, hg, hb); b = m.vert(x + hw, y + h, z + 0.02f, 0, 0, 1, hr, hg, hb); c = m.vert(x + hw, y + h + 0.27f, z + 0.02f, 0, 0, 1, hr, hg, hb); d = m.vert(x - hw, y + h + 0.27f, z + 0.02f, 0, 0, 1, hr, hg, hb);
            m.quad(a, b, c, d);
            hr = Mesh3.cr(hair); hg = Mesh3.cg(hair); hb = Mesh3.cb(hair);
            a = m.vert(x - hw, y + h + 0.2f, z + 0.03f, 0, 0, 1, hr, hg, hb); b = m.vert(x + hw, y + h + 0.2f, z + 0.03f, 0, 0, 1, hr, hg, hb); c = m.vert(x + hw, y + h + 0.29f, z + 0.03f, 0, 0, 1, hr, hg, hb); d = m.vert(x - hw, y + h + 0.29f, z + 0.03f, 0, 0, 1, hr, hg, hb);
            m.quad(a, b, c, d);
        } else { // faces +/-X
            float s = dirx;
            int a = m.vert(x, y, z - w / 2 * s, s, 0, 0, sr, sg, sb), b = m.vert(x, y, z + w / 2 * s, s, 0, 0, sr, sg, sb), c = m.vert(x, y + h, z + w / 2 * s, s, 0, 0, sr, sg, sb), d = m.vert(x, y + h, z - w / 2 * s, s, 0, 0, sr, sg, sb);
            m.quad(a, b, c, d);
            float hw = 0.13f;
            float hr = Mesh3.cr(skin), hg = Mesh3.cg(skin), hb = Mesh3.cb(skin);
            float xx = x + 0.02f * s;
            a = m.vert(xx, y + h, z - hw * s, s, 0, 0, hr, hg, hb); b = m.vert(xx, y + h, z + hw * s, s, 0, 0, hr, hg, hb); c = m.vert(xx, y + h + 0.27f, z + hw * s, s, 0, 0, hr, hg, hb); d = m.vert(xx, y + h + 0.27f, z - hw * s, s, 0, 0, hr, hg, hb);
            m.quad(a, b, c, d);
        }
    }

    // ------------------------------------------------------------------ frame
    void render(SceneData d, int w, int h) {
        if (builtFor != d.matchId) buildCrowd(d);
        float[] proj = new float[16], view = new float[16];
        M4.perspective(proj, d.fov, d.aspect, 1f, 400f);
        M4.lookAt(view, d.eye[0], d.eye[1], d.eye[2], d.target[0], d.target[1], d.target[2]);
        M4.mul(vp, proj, view);
        float sr, sg, sb;
        int[][] skyc = {{0xFFBFE0FF}, {0xFFF48A4E}, {0xFF14213D}};
        int sc = skyc[d.tod][0];
        g.begin(w, h, vp, Mesh3.cr(sc), Mesh3.cg(sc), Mesh3.cb(sc));
        float lx = -0.35f, ly = 0.85f, lz = 0.4f;
        if (d.tod == 0) g.env(lx, ly, lz, 0.80f, 0.76f, 0.68f, 0.52f, 0.55f, 0.60f);
        else if (d.tod == 1) g.env(-0.7f, 0.5f, 0.3f, 0.95f, 0.58f, 0.34f, 0.42f, 0.38f, 0.5f);
        else g.env(0, 1, 0.2f, 0.62f, 0.66f, 0.8f, 0.46f, 0.5f, 0.64f);
        float night = d.tod == 2 ? 0.28f : (d.tod == 1 ? 0.1f : 0.04f);
        // sky
        g.draw(hSky[d.tod], ident, 1, 1, 1, 1, Gfx.F_UNLIT);
        g.draw(hGround, ident, 0.55f, 0.75f, 0.58f, 1, 0);
        g.draw(hStandStatic, ident, 1, 1, 1, 1, 0);
        // crowd with excitement bob
        for (int s = 0; s < 3; s++) {
            if (crowd[s] == null) continue;
            for (int r = 0; r < crowd[s].length; r++) {
                M4.identity(mm);
                float bob = (float) Math.abs(Math.sin(d.time * (2.2f + r * 0.13f) + r * 1.7f + s)) * (0.04f + 0.3f * d.excite);
                mm[13] = bob;
                g.draw(crowd[s][r], mm, 1, 1, 1, 1, 0);
            }
        }
        g.draw(hBoards, ident, 1, 1, 1, 1, 0);
        g.draw(hTowers, ident, 1, 1, 1, 1, 0);
        g.draw(hLamps, ident, 1, 1, 0.92f, 1, Gfx.F_UNLIT);
        // pitch
        g.draw(hStripeA, ident, 0.21f, 0.56f, 0.24f, 1, 0);
        g.draw(hStripeB, ident, 0.17f, 0.47f, 0.20f, 1, 0);
        g.draw(hLines, ident, 0.95f, 0.95f, 0.95f, 1, Gfx.F_UNLIT);
        g.draw(hGoalFrame, ident, 1, 1, 1, 1, 0);
        // shadows (blended)
        for (int i = 0; i < 22; i++) if (d.pvisible[i]) shadow(d.px[i], d.py[i], 0.55f * d.ph[i] / 1.8f, d.pz[i], 0.5f);
        shadow(d.bx, d.by, 0.2f, d.bz, 0.55f);
        for (int i = 0; i < d.nOff; i++) shadow(d.ox[i], d.oy[i], 0.45f, 0, 0.4f);
        // players
        for (int i = 0; i < 22; i++) if (d.pvisible[i]) player(d, i);
        for (int i = 0; i < d.nOff; i++) official(d, i);
        // ball
        M4.identity(mm);
        M4.translate(mm, d.bx, d.bz + 0.14f, d.by);
        M4.rotY(mm, d.brx * 0.9f); M4.rotZ(mm, -d.bry * 0.9f);
        M4.scale(mm, 0.14f, 0.14f, 0.14f);
        g.draw(hBall, mm, 1, 1, 1, 1, 0);
        // net (blend) last
        g.draw(hGoalNet, ident, 1, 1, 1, 0.38f, Gfx.F_BLEND | Gfx.F_UNLIT);
        g.end();
    }

    void shadow(float x, float z, float r, float lift, float alpha) {
        M4.identity(mm);
        float k = 1f / (1f + lift * 0.6f);
        M4.translate(mm, x + 0.12f, 0, z + 0.10f);
        M4.scale(mm, r * 1.1f * k, 1, r * 0.9f * k);
        g.draw(hDisc, mm, 0, 0, 0, alpha * k, Gfx.F_BLEND | Gfx.F_UNLIT);
    }

    // ------------------------------------------------------------------ figures
    void part(int h, float[] parent, float ox, float oy, float oz, float sx, float sy, float sz, int col, int flags) {
        M4.copy(t1, parent);
        M4.translate(t1, ox, oy, oz);
        M4.scale(t1, sx, sy, sz);
        g.draw(h, t1, Mesh3.cr(col), Mesh3.cg(col), Mesh3.cb(col), 1, flags);
    }
    void child(float[] out, float[] parent, float ox, float oy, float oz, float rz, float rx) {
        M4.copy(out, parent);
        M4.translate(out, ox, oy, oz);
        if (rz != 0) M4.rotZ(out, rz);
        if (rx != 0) M4.rotX(out, rx);
    }

    void player(SceneData d, int i) {
        boolean gk = d.prole[i] == Player.GK;
        int s = d.pside[i];
        int c1 = gk ? d.gk1[s] : d.c1[s], c2 = gk ? d.gk2[s] : d.c2[s], c3 = gk ? 0xFF111111 : d.c3[s];
        int pattern = gk ? 0 : d.pat[s];
        int shorts = gk ? U.shade(d.gk2[s], 0.8f) : (U.colorDist(d.c3[s], d.c1[s]) > 70 ? d.c3[s] : U.shade(d.c1[s], 0.55f));
        if (!gk && pattern == 3 && U.colorDist(d.c2[s], d.c1[s]) > 50) shorts = d.c2[s];
        if (!gk && U.colorDist(shorts, c1) < 40) shorts = U.colorDist(c1, 0xFFFFFFFF) < 80 ? 0xFF1B1B1B : 0xFFFFFFFF;
        int socks = gk ? c1 : (pattern == 0 || pattern == 3 ? d.c1[s] : d.c2[s]);
        figure(d.px[i], d.py[i], d.pz[i], d.pfx[i], d.pfy[i], d.ph[i], d.pspeed[i], d.pphase[i], d.pkick[i], d.pst[i], d.pstT[i],
                c1, c2, shorts, socks, pattern, SKIN[d.pskin[i]], d.phs[i], HAIR[d.phc[i]], d.pbeard[i], BOOT[d.pboot[i]], gk, 0, 0, d.pside[i]);
    }

    void official(SceneData d, int i) {
        int type = d.otype[i];
        int c1, c2, shorts, socks, hold = 0, pattern = 0;
        if (type == Official.REF) { c1 = 0xFF151515; c2 = 0xFFE8D400; shorts = 0xFF151515; socks = 0xFF151515; if (d.ocard[i] > 0) hold = d.ocard[i] == 1 ? 1 : 2; }
        else if (type == Official.COACH) { c1 = d.oside[i] == 0 ? 0xFF1F2937 : 0xFF374151; c2 = 0xFFFFFFFF; shorts = c1; socks = 0xFF111111; }
        else { c1 = 0xFFF2D100; c2 = 0xFF111111; shorts = 0xFF111111; socks = 0xFF111111; hold = d.oflag[i] > 0 ? 4 : 3; }
        figure(d.ox[i], d.oy[i], 0, d.ofx[i], d.ofy[i], 1.82f, (float) Math.sqrt(0), d.ophase[i], 0, 0, 0, c1, c2, shorts, socks, pattern, SKIN[1 + (i % 3)], 2, HAIR[1], i % 2, 0xFF111111, false, 0, hold, -1);
    }

    static float sm(float t) { t = t < 0 ? 0 : (t > 1 ? 1 : t); return t * t * (3 - 2 * t); }

    void figure(float x, float zw, float lift, float fx, float fy, float height, float speed, float phase, float kickT, int st, float stT,
                int c1, int c2, int shorts, int socks, int pattern, int skin, int hairStyle, int hair, int beard, int boot, boolean gk, int armsUp, int hold, int sideIdx) {
        float s = height / 1.8f;
        float sp = Math.min(1.15f, speed / 7.5f);
        float yaw = (float) Math.atan2(fy, fx);
        float hipH = 0.93f * s;
        float leanZ = 0.06f + 0.20f * sp;
        float roll = 0, pitchBody = 0;
        boolean lying = false;
        float legAng0 = 0, legAng1 = 0, shin0 = 0, shin1 = 0, arm0 = 0, arm1 = 0, fore0 = 0.4f, fore1 = 0.4f, armSide = 0.12f;
        float ph = phase;
        float bob = sp > 0.05f ? Math.abs((float) Math.sin(ph)) * 0.05f * sp : 0;
        // running cycle
        float sw0 = (float) Math.sin(ph), sw1 = (float) Math.sin(ph + Math.PI);
        legAng0 = sw0 * 0.9f * sp; legAng1 = sw1 * 0.9f * sp;
        shin0 = -((0.15f + 1.0f * Math.max(0, (float) Math.cos(ph))) * sp);
        shin1 = -((0.15f + 1.0f * Math.max(0, (float) Math.cos(ph + Math.PI))) * sp);
        arm0 = -sw0 * 0.85f * sp; arm1 = -sw1 * 0.85f * sp;
        fore0 = fore1 = 0.55f + 0.5f * sp;
        if (sp < 0.05f) { float br = (float) Math.sin(phase * 0.3f + x) * 0.02f; arm0 = br; arm1 = -br; fore0 = fore1 = 0.15f; }
        if (kickT > 0) {
            float k = 1 - kickT / 0.3f;
            float f = sm(k * 1.25f);
            legAng1 = k < 0.35f ? -0.9f * (k / 0.35f) : -0.9f + 2.5f * sm((k - 0.35f) / 0.3f);
            shin1 = k < 0.35f ? -1.5f : -1.5f * (1 - sm((k - 0.35f) / 0.25f));
            arm0 = 0.9f; arm1 = -0.9f;
        }
        float yHip = hipH;
        float yawExtra = 0;
        switch (st) {
            case Pawn.SLIDE: { pitchBody = 1.30f; yHip = 0.30f * s; legAng0 = 0.1f; legAng1 = 0f; shin0 = 0; shin1 = 0; arm0 = 1.2f; arm1 = 1.0f; fore0 = fore1 = 0.1f; leanZ = 0; break; }
            case Pawn.FALL: case Pawn.DOWN: { float k = sm(stT / 0.25f); pitchBody = 1.45f * k; yHip = (0.93f - 0.72f * k) * s; legAng0 = 0.2f * k; legAng1 = 0.4f * k; shin0 = -0.3f * k; shin1 = -0.1f * k; arm0 = 1.0f * k; arm1 = 0.5f * k; leanZ = 0; break; }
            case Pawn.GETUP: { float k = 1 - sm(stT / 0.5f); pitchBody = 1.45f * k; yHip = (0.93f - 0.72f * k) * s; leanZ = 0.3f * (1 - k); legAng0 = legAng1 = 0.3f * k; break; }
            case Pawn.DIVE: { float k = sm(stT / 0.25f); roll = (fy >= 0 ? 1 : -1) * 1.25f * k * (sideIdx == 1 ? -1 : 1) * (fx >= 0 ? 1 : -1) * (fx >= 0 ? 1 : 1); yHip = (0.93f - 0.35f * k) * s + 0.1f * (float) Math.sin(Math.min(1, stT) * Math.PI) * s; legAng0 = 0.3f; legAng1 = -0.3f; armsUp = 2; leanZ = 0; break; }
            case Pawn.JUMP: { legAng0 = 0.9f; legAng1 = 0.5f; shin0 = -1.4f; shin1 = -1.2f; armsUp = 1; break; }
            case Pawn.CELEB: { armsUp = 1; yHip += 0.06f * s * (float) Math.abs(Math.sin(stT * 7f)); legAng0 = legAng1 = 0; shin0 = shin1 = 0; leanZ = -0.1f; break; }
            case Pawn.THROW: { armsUp = 3; break; }
            default: break;
        }
        if (armsUp == 1) { arm0 = arm1 = 2.9f + 0.2f * (float) Math.sin(stT * 12); fore0 = fore1 = 0.3f; armSide = 0.3f; }
        else if (armsUp == 3) { arm0 = arm1 = 2.9f; fore0 = fore1 = -0.3f; armSide = 0.05f; }
        else if (armsUp == 2) { arm0 = 2.5f; arm1 = 2.7f; fore0 = fore1 = 0.1f; armSide = 0.5f; }
        // root: world translate, roll (world X axis rotation), yaw, body pitch about hip
        M4.identity(root);
        M4.translate(root, x, yHip + lift * 0.0f + bob * s, zw);
        if (lift > 0) M4.translate(root, 0, lift, 0);
        if (roll != 0) M4.rotX(root, roll);
        M4.rotY(root, -yaw);
        if (pitchBody != 0) M4.rotZ(root, pitchBody);
        M4.scale(root, s, s, s);
        // pelvis / shorts
        part(hBox, root, 0, -0.02f, 0, 0.26f, 0.20f, 0.40f, shorts, 0);
        // torso
        child(a, root, 0, 0.0f, 0, leanZ, 0);
        M4.rotY(a, 0.18f * -(float) Math.sin(ph) * sp);
        part(hBox, a, 0, 0.30f, 0, 0.24f, 0.50f, 0.40f, c1, 0);
        float tw = 0.255f;
        switch (pattern) {
            case 1: for (int k = -1; k <= 1; k++) part(hBox, a, 0, 0.30f, k * 0.14f, tw, 0.505f, 0.075f, c2, 0); break;
            case 2: for (int k = 0; k < 3; k++) part(hBox, a, 0, 0.12f + k * 0.17f, 0, tw, 0.07f, 0.405f, c2, 0); break;
            case 3: part(hBox, a, 0, 0.30f, 0.10f, tw, 0.505f, 0.20f, c2, 0); break;
            case 4: part(hBox, a, 0, 0.36f, 0, tw, 0.09f, 0.405f, c2, 0); break;
            case 5: part(hBox, a, 0, 0.38f, 0, tw, 0.08f, 0.405f, c2, 0); break;
            default: break;
        }
        // neck + head
        part(hCyl, a, 0, 0.58f, 0, 0.05f, 0.08f, 0.05f, skin, 0);
        part(hSphere, a, 0.01f, 0.73f, 0, 0.128f, 0.138f, 0.118f, skin, 0);
        // eyes
        part(hSphere, a, 0.118f, 0.745f, 0.05f, 0.016f, 0.016f, 0.016f, 0xFF15151A, 0);
        part(hSphere, a, 0.118f, 0.745f, -0.05f, 0.016f, 0.016f, 0.016f, 0xFF15151A, 0);
        hairModel(a, hairStyle, hair, beard);
        // arms
        for (int i = 0; i < 2; i++) {
            float zs = i == 0 ? -0.235f : 0.235f;
            float ua = i == 0 ? arm0 : arm1, fa = i == 0 ? fore0 : fore1;
            child(b, a, 0, 0.50f, zs, ua, 0);
            M4.rotX(b, (i == 0 ? 1 : -1) * armSide * (armsUp > 0 ? 1 : 0.5f));
            part(hCyl, b, 0, 0, 0, 0.052f, 0.29f, 0.052f, c1, 0);
            child(c, b, 0, -0.29f, 0, fa, 0);
            part(hCyl, c, 0, 0, 0, 0.043f, 0.27f, 0.043f, skin, 0);
            part(hSphere, c, 0, -0.29f, 0, 0.05f, 0.05f, 0.05f, gk ? 0xFFFDE047 : skin, 0);
            if (i == 1 && hold != 0) holdItem(c, hold);
        }
        // legs
        for (int i = 0; i < 2; i++) {
            float zs = i == 0 ? -0.10f : 0.10f;
            float ta = i == 0 ? legAng0 : legAng1, ka = i == 0 ? shin0 : shin1;
            child(b, root, 0, -0.05f, zs, ta, 0);
            part(hCyl, b, 0, 0, 0, 0.078f, 0.45f, 0.078f, skin, 0);
            part(hCyl, b, 0, 0.02f, 0, 0.092f, 0.27f, 0.092f, shorts, 0);
            child(c, b, 0, -0.45f, 0, ka, 0);
            part(hCyl, c, 0, 0, 0, 0.058f, 0.43f, 0.058f, socks, 0);
            child(d, c, 0, -0.43f, 0, 0, 0);
            part(hBox, d, 0.06f, -0.03f, 0, 0.28f, 0.075f, 0.105f, boot, 0);
        }
    }

    void holdItem(float[] hand, int hold) {
        switch (hold) {
            case 1: part(hBox, hand, 0.05f, -0.34f, 0, 0.01f, 0.1f, 0.075f, 0xFFFFD400, 0); break;
            case 2: part(hBox, hand, 0.05f, -0.34f, 0, 0.01f, 0.1f, 0.075f, 0xFFE11D2E, 0); break;
            case 3: case 4: {
                part(hCyl, hand, 0, -0.29f, 0, 0.01f, 0.42f, 0.01f, 0xFF222222, 0);
                part(hBox, hand, 0.0f, -0.62f, 0.12f, 0.01f, 0.2f, 0.26f, hold == 4 ? 0xFFFFD400 : 0xFFE11D2E, 0);
                break;
            }
            default: break;
        }
    }

    void hairModel(float[] head, int style, int hair, int beard) {
        switch (style) {
            case 0: break;
            case 1: part(hSphere, head, 0.0f, 0.745f, 0, 0.12f, 0.1f, 0.11f, hair, 0); break;
            case 2: part(hSphere, head, -0.005f, 0.755f, 0, 0.123f, 0.108f, 0.115f, hair, 0); break;
            case 3: part(hSphere, head, -0.005f, 0.755f, 0, 0.123f, 0.108f, 0.115f, hair, 0); part(hBox, head, -0.10f, 0.62f, 0, 0.07f, 0.25f, 0.2f, hair, 0); break;
            case 4: part(hBox, head, 0, 0.84f, 0, 0.2f, 0.09f, 0.05f, hair, 0); part(hSphere, head, -0.02f, 0.75f, 0, 0.112f, 0.09f, 0.108f, hair, 0); break;
            case 5: part(hSphere, head, -0.01f, 0.76f, 0, 0.17f, 0.15f, 0.17f, hair, 0); break;
            case 6: part(hSphere, head, 0, 0.77f, 0, 0.14f, 0.1f, 0.14f, hair, 0); break;
            default: part(hSphere, head, -0.005f, 0.755f, 0, 0.123f, 0.108f, 0.115f, hair, 0); part(hBox, head, -0.14f, 0.7f, 0, 0.05f, 0.1f, 0.05f, hair, 0); break;
        }
        if (beard == 1) part(hBox, head, 0.085f, 0.66f, 0, 0.05f, 0.07f, 0.14f, hair, 0);
    }
}
