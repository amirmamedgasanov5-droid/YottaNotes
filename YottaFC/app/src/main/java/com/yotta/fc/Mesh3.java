package com.yotta.fc;

import java.util.ArrayList;

/** Indexed triangle mesh builder: vertex = pos(3) normal(3) colour(3). */
final class Mesh3 {
    float[] v = new float[9 * 1024]; int nv;
    short[] idx = new short[3 * 2048]; int ni;

    int vert(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b) {
        if ((nv + 1) * 9 > v.length) { float[] n = new float[v.length * 2]; System.arraycopy(v, 0, n, 0, v.length); v = n; }
        int o = nv * 9;
        v[o] = x; v[o + 1] = y; v[o + 2] = z; v[o + 3] = nx; v[o + 4] = ny; v[o + 5] = nz; v[o + 6] = r; v[o + 7] = g; v[o + 8] = b;
        return nv++;
    }
    void tri(int a, int b, int c) {
        if (ni + 3 > idx.length) { short[] n = new short[idx.length * 2]; System.arraycopy(idx, 0, n, 0, idx.length); idx = n; }
        idx[ni++] = (short) a; idx[ni++] = (short) b; idx[ni++] = (short) c;
    }
    void quad(int a, int b, int c, int d) { tri(a, b, c); tri(a, c, d); }

    static float cr(int c) { return ((c >> 16) & 255) / 255f; }
    static float cg(int c) { return ((c >> 8) & 255) / 255f; }
    static float cb(int c) { return (c & 255) / 255f; }

    /** axis aligned box, centre (cx,cy,cz), size (w,h,d), colour. */
    Mesh3 box(float cx, float cy, float cz, float w, float h, float d, int col) {
        float r = cr(col), g = cg(col), b = cb(col);
        float x0 = cx - w / 2, x1 = cx + w / 2, y0 = cy - h / 2, y1 = cy + h / 2, z0 = cz - d / 2, z1 = cz + d / 2;
        face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r, g, b);
        face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, r, g, b);
        face(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, r, g, b);
        face(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, r, g, b);
        face(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, r, g, b);
        face(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r, g, b);
        return this;
    }
    void face(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz, float r, float g, float b) {
        int a = vert(ax, ay, az, nx, ny, nz, r, g, b), b2 = vert(bx, by, bz, nx, ny, nz, r, g, b), c = vert(cx, cy, cz, nx, ny, nz, r, g, b), d = vert(dx, dy, dz, nx, ny, nz, r, g, b);
        quad(a, b2, c, d);
    }
    /** vertical or arbitrary quad with colour. */
    Mesh3 quadXYZ(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int col) {
        float ux = bx - ax, uy = by - ay, uz = bz - az, vx = dx - ax, vy = dy - ay, vz = dz - az;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz); if (l < 1e-6f) l = 1;
        face(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx / l, ny / l, nz / l, cr(col), cg(col), cb(col));
        return this;
    }
    /** flat horizontal rectangle at height y. */
    Mesh3 flat(float x0, float z0, float x1, float z1, float y, int col) {
        face(x0, y, z1, x1, y, z1, x1, y, z0, x0, y, z0, 0, 1, 0, cr(col), cg(col), cb(col));
        return this;
    }
    /** tapered cylinder along -Y from y=0 (radius r0) to y=-1 (radius r1), unit length. */
    Mesh3 cylDown(float r0, float r1, int seg, int col, boolean caps) {
        float cr = cr(col), cg = cg(col), cb = cb(col);
        int first = nv;
        for (int i = 0; i <= seg; i++) {
            float a = (float) (Math.PI * 2 * i / seg), c = (float) Math.cos(a), s = (float) Math.sin(a);
            vert(c * r0, 0, s * r0, c, 0, s, cr, cg, cb);
            vert(c * r1, -1, s * r1, c, 0, s, cr, cg, cb);
        }
        for (int i = 0; i < seg; i++) { int a = first + i * 2; tri(a, a + 1, a + 3); tri(a, a + 3, a + 2); }
        if (caps) {
            int c0 = vert(0, 0, 0, 0, 1, 0, cr, cg, cb), c1 = vert(0, -1, 0, 0, -1, 0, cr, cg, cb);
            int f2 = nv;
            for (int i = 0; i <= seg; i++) {
                float a = (float) (Math.PI * 2 * i / seg), c = (float) Math.cos(a), s = (float) Math.sin(a);
                vert(c * r0, 0, s * r0, 0, 1, 0, cr, cg, cb); vert(c * r1, -1, s * r1, 0, -1, 0, cr, cg, cb);
            }
            for (int i = 0; i < seg; i++) { tri(c0, f2 + i * 2 + 2, f2 + i * 2); tri(c1, f2 + i * 2 + 1, f2 + i * 2 + 3); }
        }
        return this;
    }
    /** cylinder between two points (world space), constant radius. */
    Mesh3 cylBetween(float x0, float y0, float z0, float x1, float y1, float z1, float rad, int seg, int col) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float ax = dx / len, ay = dy / len, az = dz / len;
        // perpendicular basis
        float px, py, pz;
        if (Math.abs(ay) < 0.9f) { px = ay * 0 - az * 1 * 0; px = -az; py = 0; pz = ax; } else { px = 1; py = 0; pz = 0; }
        float pl = (float) Math.sqrt(px * px + py * py + pz * pz); px /= pl; py /= pl; pz /= pl;
        float qx = ay * pz - az * py, qy = az * px - ax * pz, qz = ax * py - ay * px;
        float r = cr(col), g = cg(col), b = cb(col);
        int first = nv;
        for (int i = 0; i <= seg; i++) {
            float a = (float) (Math.PI * 2 * i / seg), c = (float) Math.cos(a), s = (float) Math.sin(a);
            float nx = px * c + qx * s, ny = py * c + qy * s, nz = pz * c + qz * s;
            vert(x0 + nx * rad, y0 + ny * rad, z0 + nz * rad, nx, ny, nz, r, g, b);
            vert(x1 + nx * rad, y1 + ny * rad, z1 + nz * rad, nx, ny, nz, r, g, b);
        }
        for (int i = 0; i < seg; i++) { int a = first + i * 2; tri(a, a + 1, a + 3); tri(a, a + 3, a + 2); }
        return this;
    }
    Mesh3 sphere(float rad, int lat, int lon, int col) { return sphere(0, 0, 0, rad, lat, lon, col, null); }
    /** sphere; if patch != null dark patches on icosahedron-ish directions. */
    Mesh3 sphere(float cx, float cy, float cz, float rad, int lat, int lon, int col, int[] patchCols) {
        float r = cr(col), g = cg(col), b = cb(col);
        int first = nv;
        for (int i = 0; i <= lat; i++) {
            float th = (float) (Math.PI * i / lat), st = (float) Math.sin(th), ct = (float) Math.cos(th);
            for (int j = 0; j <= lon; j++) {
                float ph = (float) (Math.PI * 2 * j / lon), sp = (float) Math.sin(ph), cp = (float) Math.cos(ph);
                float nx = st * cp, ny = ct, nz = st * sp;
                float rr = r, gg = g, bb = b;
                if (patchCols != null) {
                    float best = 0;
                    for (int k = 0; k < PENT.length; k++) { float d = nx * PENT[k][0] + ny * PENT[k][1] + nz * PENT[k][2]; if (d > best) best = d; }
                    if (best > 0.90f) { rr = 0.1f; gg = 0.11f; bb = 0.13f; }
                }
                vert(cx + nx * rad, cy + ny * rad, cz + nz * rad, nx, ny, nz, rr, gg, bb);
            }
        }
        for (int i = 0; i < lat; i++) for (int j = 0; j < lon; j++) {
            int a = first + i * (lon + 1) + j, b2 = a + lon + 1;
            tri(a, b2, a + 1); tri(a + 1, b2, b2 + 1);
        }
        return this;
    }
    static final float[][] PENT = new float[12][3];
    static {
        float t = (1 + (float) Math.sqrt(5)) / 2;
        float[][] raw = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t}, {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        for (int i = 0; i < 12; i++) { float l = (float) Math.sqrt(raw[i][0] * raw[i][0] + raw[i][1] * raw[i][1] + raw[i][2] * raw[i][2]); for (int k = 0; k < 3; k++) PENT[i][k] = raw[i][k] / l; }
    }
    /** flat disc on ground (y). */
    Mesh3 disc(float rad, int seg, float y, int col) {
        float r = cr(col), g = cg(col), b = cb(col);
        int c = vert(0, y, 0, 0, 1, 0, r, g, b);
        int first = nv;
        for (int i = 0; i <= seg; i++) { float a = (float) (Math.PI * 2 * i / seg); vert((float) Math.cos(a) * rad, y, (float) Math.sin(a) * rad, 0, 1, 0, r, g, b); }
        for (int i = 0; i < seg; i++) tri(c, first + i + 1, first + i);
        return this;
    }
    /** append other mesh with offset. */
    Mesh3 add(Mesh3 o, float dx, float dy, float dz) {
        int base = nv;
        for (int i = 0; i < o.nv; i++) vert(o.v[i * 9] + dx, o.v[i * 9 + 1] + dy, o.v[i * 9 + 2] + dz, o.v[i * 9 + 3], o.v[i * 9 + 4], o.v[i * 9 + 5], o.v[i * 9 + 6], o.v[i * 9 + 7], o.v[i * 9 + 8]);
        for (int i = 0; i < o.ni; i += 3) tri(base + o.idx[i], base + o.idx[i + 1], base + o.idx[i + 2]);
        return this;
    }
    float[] verts() { float[] a = new float[nv * 9]; System.arraycopy(v, 0, a, 0, nv * 9); return a; }
    short[] indices() { short[] a = new short[ni]; System.arraycopy(idx, 0, a, 0, ni); return a; }
}
