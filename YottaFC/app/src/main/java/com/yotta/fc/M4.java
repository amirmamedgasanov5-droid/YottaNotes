package com.yotta.fc;

/** Column-major 4x4 matrix helpers (OpenGL convention). */
final class M4 {
    static void identity(float[] m) { for (int i = 0; i < 16; i++) m[i] = 0; m[0] = m[5] = m[10] = m[15] = 1; }
    static void copy(float[] d, float[] s) { System.arraycopy(s, 0, d, 0, 16); }
    /** out = a * b (out may not alias a or b). */
    static void mul(float[] o, float[] a, float[] b) {
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++)
                o[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
    }
    static void translate(float[] m, float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z; m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z; m[15] += m[3] * x + m[7] * y + m[11] * z;
    }
    static void scale(float[] m, float x, float y, float z) {
        for (int i = 0; i < 4; i++) { m[i] *= x; m[4 + i] *= y; m[8 + i] *= z; }
    }
    static void rotX(float[] m, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int i = 0; i < 4; i++) { float y = m[4 + i], z = m[8 + i]; m[4 + i] = y * c + z * s; m[8 + i] = -y * s + z * c; }
    }
    static void rotY(float[] m, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int i = 0; i < 4; i++) { float x = m[i], z = m[8 + i]; m[i] = x * c - z * s; m[8 + i] = x * s + z * c; }
    }
    static void rotZ(float[] m, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int i = 0; i < 4; i++) { float x = m[i], y = m[4 + i]; m[i] = x * c + y * s; m[4 + i] = -x * s + y * c; }
    }
    static void perspective(float[] m, float fovy, float aspect, float n, float f) {
        float t = 1f / (float) Math.tan(fovy / 2);
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = t / aspect; m[5] = t; m[10] = (f + n) / (n - f); m[11] = -1; m[14] = 2 * f * n / (n - f);
    }
    static void lookAt(float[] m, float ex, float ey, float ez, float cx, float cy, float cz) {
        float fx = cx - ex, fy = cy - ey, fz = cz - ez;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz); fx /= fl; fy /= fl; fz /= fl;
        // up = (0,1,0); s = f x up
        float sx = fy * 0 - fz * 1, sy = fz * 0 - fx * 0, sz = fx * 1 - fy * 0;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz); sx /= sl; sy /= sl; sz /= sl;
        float ux = sy * fz - sz * fy, uy = sz * fx - sx * fz, uz = sx * fy - sy * fx;
        m[0] = sx; m[4] = sy; m[8] = sz; m[12] = -(sx * ex + sy * ey + sz * ez);
        m[1] = ux; m[5] = uy; m[9] = uz; m[13] = -(ux * ex + uy * ey + uz * ez);
        m[2] = -fx; m[6] = -fy; m[10] = -fz; m[14] = (fx * ex + fy * ey + fz * ez);
        m[3] = 0; m[7] = 0; m[11] = 0; m[15] = 1;
    }
    /** Project world point; returns false if behind camera. out = {sx, sy, w}. */
    static boolean project(float[] vp, float x, float y, float z, int vw, int vh, float[] out) {
        float cx = vp[0] * x + vp[4] * y + vp[8] * z + vp[12];
        float cy = vp[1] * x + vp[5] * y + vp[9] * z + vp[13];
        float cw = vp[3] * x + vp[7] * y + vp[11] * z + vp[15];
        if (cw < 0.01f) return false;
        out[0] = (cx / cw * 0.5f + 0.5f) * vw;
        out[1] = (1f - (cy / cw * 0.5f + 0.5f)) * vh;
        out[2] = cw;
        return true;
    }
}
