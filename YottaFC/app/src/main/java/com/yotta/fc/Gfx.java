package com.yotta.fc;

/** Minimal rendering backend (OpenGL ES 2 on device, software rasteriser for previews). */
interface Gfx {
    int F_UNLIT = 1, F_BLEND = 2, F_EMISSIVE = 4;
    int upload(float[] verts, short[] idx);
    void delete(int handle);
    void begin(int w, int h, float[] viewProj, float skyR, float skyG, float skyB);
    void env(float lx, float ly, float lz, float sunR, float sunG, float sunB, float ambR, float ambG, float ambB);
    void draw(int handle, float[] model, float r, float g, float b, float a, int flags);
    void end();
}
