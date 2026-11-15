package com.yotta.fc;

import java.util.Random;

/** Small math / random helpers. */
final class U {
    static final Random R = new Random();
    static final float PI = (float) Math.PI;

    static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    static int clampi(int v, int a, int b) { return v < a ? a : (v > b ? b : v); }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float rnd() { return R.nextFloat(); }
    static float rnd(float a, float b) { return a + R.nextFloat() * (b - a); }
    static int ri(int a, int b) { return a + R.nextInt(b - a + 1); }
    static boolean chance(float p) { return R.nextFloat() < p; }
    static float gauss() { return (float) R.nextGaussian(); }
    static float sq(float v) { return v * v; }
    static float len(float x, float y) { return (float) Math.sqrt(x * x + y * y); }
    static float dist(float x1, float y1, float x2, float y2) { return len(x2 - x1, y2 - y1); }
    static float sign(float v) { return v < 0 ? -1f : 1f; }
    static float smooth(float t) { t = clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    static float approach(float v, float target, float step) {
        if (v < target) return Math.min(v + step, target);
        return Math.max(v - step, target);
    }
    static float angDiff(float a, float b) {
        float d = (b - a) % (2 * PI);
        if (d > PI) d -= 2 * PI;
        if (d < -PI) d += 2 * PI;
        return d;
    }
    static <T> T pick(T[] arr) { return arr[R.nextInt(arr.length)]; }
    static int mix(int c1, int c2, float t) {
        int a = (int) lerp((c1 >>> 24) & 255, (c2 >>> 24) & 255, t);
        int r = (int) lerp((c1 >> 16) & 255, (c2 >> 16) & 255, t);
        int g = (int) lerp((c1 >> 8) & 255, (c2 >> 8) & 255, t);
        int b = (int) lerp(c1 & 255, c2 & 255, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    static int alpha(int c, int a) { return (c & 0x00FFFFFF) | (clampi(a, 0, 255) << 24); }
    static int shade(int c, float f) {
        int r = clampi((int) (((c >> 16) & 255) * f), 0, 255);
        int g = clampi((int) (((c >> 8) & 255) * f), 0, 255);
        int b = clampi((int) ((c & 255) * f), 0, 255);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
    static float colorDist(int a, int b) {
        float dr = ((a >> 16) & 255) - ((b >> 16) & 255);
        float dg = ((a >> 8) & 255) - ((b >> 8) & 255);
        float db = (a & 255) - (b & 255);
        return (float) Math.sqrt(dr * dr * 0.3f + dg * dg * 0.59f + db * db * 0.11f);
    }
    static String money(float m) {
        if (m >= 100) return "€" + Math.round(m) + "M";
        if (m >= 10) return "€" + (Math.round(m * 10) / 10f) + "M";
        if (m >= 1) return "€" + (Math.round(m * 100) / 100f) + "M";
        return "€" + Math.round(m * 1000) + "K";
    }
}
