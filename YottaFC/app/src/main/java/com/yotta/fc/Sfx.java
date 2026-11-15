package com.yotta.fc;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/** Procedurally synthesised sound effects. */
final class Sfx {
    static final int RATE = 22050;
    static final int CLICK = 12, AMBIENT = 13;
    SoundPool pool;
    int[] ids = new int[16];
    int ambientStream;
    boolean enabled = true;
    float volume = 0.8f;
    boolean ready;

    void init(Context ctx) {
        try {
            pool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            File dir = ctx.getCacheDir();
            for (int i = 0; i <= AMBIENT; i++) {
                short[] pcm = synth(i);
                File f = new File(dir, "sfx" + i + ".wav");
                writeWav(f, pcm);
                ids[i] = pool.load(f.getAbsolutePath(), 1);
            }
            ready = true;
        } catch (Throwable t) { ready = false; }
    }

    void play(int id, float vol) {
        if (!ready || !enabled || id < 0 || id >= ids.length) return;
        try { pool.play(ids[id], vol * volume, vol * volume, 1, 0, 1f); } catch (Throwable t) { }
    }

    void startAmbient() {
        if (!ready || !enabled) return;
        try { stopAmbient(); ambientStream = pool.play(ids[AMBIENT], 0.25f * volume, 0.25f * volume, 1, -1, 1f); } catch (Throwable t) { }
    }
    void ambientLevel(float lvl) {
        if (!ready || ambientStream == 0) return;
        try { float v = (0.15f + 0.5f * lvl) * volume; pool.setVolume(ambientStream, v, v); } catch (Throwable t) { }
    }
    void stopAmbient() { if (ready && ambientStream != 0) { try { pool.stop(ambientStream); } catch (Throwable t) { } ambientStream = 0; } }
    void release() { stopAmbient(); if (pool != null) { pool.release(); pool = null; } ready = false; }

    static short[] synth(int id) {
        Random r = new Random(id * 99 + 1);
        switch (id) {
            case Match.S_WHISTLE: return whistle(0.75f, 2900);
            case Match.S_KICK: return thump(0.14f, 160, 0.9f, r);
            case Match.S_GOAL: return crowd(3.2f, 1.0f, r, true);
            case Match.S_POST: return ring(0.5f, new float[]{520, 1130, 1830}, 0.9f);
            case Match.S_SAVE: return thump(0.16f, 220, 0.6f, r);
            case Match.S_CARD: return noiseBurst(0.25f, 0.5f, r);
            case Match.S_BOUNCE: return thump(0.1f, 110, 0.5f, r);
            case Match.S_FOUL: return whistle(0.35f, 3200);
            case Match.S_END: return whistle(1.0f, 2700);
            case Match.S_NET: return noiseBurst(0.45f, 0.35f, r);
            case Match.S_TACKLE: return thump(0.2f, 90, 0.8f, r);
            case Match.S_OOH: return crowd(1.5f, 0.7f, r, false);
            case CLICK: return ring(0.06f, new float[]{1800}, 0.5f);
            default: return crowd(4f, 0.35f, r, false);
        }
    }

    static short[] whistle(float dur, float f) {
        int n = (int) (dur * RATE);
        short[] s = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double fm = f + 140 * Math.sin(2 * Math.PI * 24 * t);
            ph += 2 * Math.PI * fm / RATE;
            double env = Math.min(1, t * 40) * Math.min(1, (dur - t) * 12);
            double v = Math.sin(ph) * 0.6 + Math.sin(ph * 2) * 0.25 + Math.sin(ph * 3) * 0.1;
            s[i] = (short) (v * env * 14000);
        }
        return s;
    }

    static short[] thump(float dur, float f, float amp, Random r) {
        int n = (int) (dur * RATE);
        short[] s = new short[n];
        double lp = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double env = Math.exp(-t * 28);
            lp += (r.nextGaussian() * 0.6 - lp) * 0.18;
            double v = Math.sin(2 * Math.PI * f * t * (1 - t * 2)) * env + lp * env * 0.6;
            s[i] = (short) (v * amp * 20000);
        }
        return s;
    }

    static short[] ring(float dur, float[] fs, float amp) {
        int n = (int) (dur * RATE);
        short[] s = new short[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE, v = 0;
            for (int k = 0; k < fs.length; k++) v += Math.sin(2 * Math.PI * fs[k] * t) / (k + 1);
            s[i] = (short) (v * Math.exp(-t * (dur > 0.2 ? 7 : 60)) * amp * 14000);
        }
        return s;
    }

    static short[] noiseBurst(float dur, float amp, Random r) {
        int n = (int) (dur * RATE);
        short[] s = new short[n];
        double lp = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            lp += (r.nextGaussian() - lp) * 0.5;
            s[i] = (short) (lp * Math.exp(-t * 14) * amp * 12000);
        }
        return s;
    }

    static short[] crowd(float dur, float amp, Random r, boolean swell) {
        int n = (int) (dur * RATE);
        short[] s = new short[n];
        double lp1 = 0, lp2 = 0, lp3 = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE, x = t / dur;
            double w = r.nextGaussian();
            lp1 += (w - lp1) * 0.10; lp2 += (lp1 - lp2) * 0.10; lp3 += (w - lp3) * 0.35;
            double env = swell ? Math.min(1, t * 4) * Math.pow(Math.max(0, 1 - x), 0.7) * (0.75 + 0.25 * Math.sin(t * 9)) : 1.0;
            if (!swell && amp > 0.5) env = Math.sin(Math.PI * x);
            double v = (lp2 * 2.6 + lp3 * 0.35 + Math.sin(2 * Math.PI * (180 + 40 * Math.sin(t * 2)) * t) * 0.1 * (swell ? 1 : 0.3)) * env;
            if (amp <= 0.5) { // ambient loop: crossfade ends
                double edge = Math.min(1, Math.min(t, dur - t) * 3);
                v = (lp2 * 2.2 + lp3 * 0.18) * (0.7 + 0.3 * Math.sin(t * 1.7)) * (0.6 + 0.4 * edge);
            }
            s[i] = (short) Math.max(-32000, Math.min(32000, v * amp * 12000));
        }
        return s;
    }

    static void writeWav(File f, short[] pcm) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        int dataLen = pcm.length * 2;
        byte[] h = new byte[44];
        put(h, 0, "RIFF"); putInt(h, 4, 36 + dataLen); put(h, 8, "WAVEfmt "); putInt(h, 16, 16); h[20] = 1; h[22] = 1;
        putInt(h, 24, RATE); putInt(h, 28, RATE * 2); h[32] = 2; h[34] = 16; put(h, 36, "data"); putInt(h, 40, dataLen);
        o.write(h);
        byte[] b = new byte[dataLen];
        for (int i = 0; i < pcm.length; i++) { b[i * 2] = (byte) (pcm[i] & 255); b[i * 2 + 1] = (byte) ((pcm[i] >> 8) & 255); }
        o.write(b);
        o.close();
    }
    static void put(byte[] b, int o, String s) { for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i); }
    static void putInt(byte[] b, int o, int v) { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); b[o + 2] = (byte) (v >> 16); b[o + 3] = (byte) (v >> 24); }
}
