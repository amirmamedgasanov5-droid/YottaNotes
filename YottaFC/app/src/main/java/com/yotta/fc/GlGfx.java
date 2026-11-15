package com.yotta.fc;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/** OpenGL ES 2.0 backend + GLSurfaceView renderer. */
final class GlGfx implements Gfx, GLSurfaceView.Renderer {
    volatile SceneData data;
    Scene3 scene;
    int prog, aPos, aNor, aCol, uMvp, uModel, uColor, uAlpha, uLight, uSun, uAmb, uUnlit, uEmis;
    final ArrayList<int[]> bufs = new ArrayList<int[]>();
    int vw = 1, vh = 1;
    final float[] mvp = new float[16];
    float[] vp = new float[16];
    float lx, ly = 1, lz, sr = 1, sg = 1, sb = 1, ar = 0.5f, ag = 0.5f, ab = 0.5f;
    volatile float aspect = 2.2f;

    static final String VS = "attribute vec3 aPos; attribute vec3 aNor; attribute vec3 aCol;\n"
            + "uniform mat4 uMvp; uniform mat4 uModel; uniform vec3 uColor; uniform vec3 uLight; uniform vec3 uSun; uniform vec3 uAmb; uniform float uUnlit; uniform float uEmis;\n"
            + "varying vec3 vC;\n"
            + "void main(){ gl_Position = uMvp * vec4(aPos,1.0);\n"
            + " vec3 n = normalize(mat3(uModel[0].xyz, uModel[1].xyz, uModel[2].xyz) * aNor);\n"
            + " float dl = max(dot(n, uLight), 0.0);\n"
            + " vec3 base = aCol * uColor;\n"
            + " vec3 lit = base * (uAmb + uSun * dl) + vec3(uEmis);\n"
            + " vC = mix(lit, base, uUnlit); }\n";
    static final String FS = "precision mediump float; varying vec3 vC; uniform float uAlpha;\n"
            + "void main(){ gl_FragColor = vec4(vC, uAlpha); }\n";

    public static final class Chooser implements GLSurfaceView.EGLConfigChooser {
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[][] specs = {
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 0, EGL10.EGL_DEPTH_SIZE, 16, 0x3040, 4, 0x3032, 1, 0x3031, 4, EGL10.EGL_NONE},
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 0, EGL10.EGL_DEPTH_SIZE, 16, 0x3040, 4, EGL10.EGL_NONE},
                    {EGL10.EGL_DEPTH_SIZE, 16, 0x3040, 4, EGL10.EGL_NONE}};
            for (int[] spec : specs) {
                int[] num = new int[1];
                if (egl.eglChooseConfig(display, spec, null, 0, num) && num[0] > 0) {
                    EGLConfig[] cfg = new EGLConfig[num[0]];
                    egl.eglChooseConfig(display, spec, cfg, num[0], num);
                    return cfg[0];
                }
            }
            throw new IllegalArgumentException("no EGL config");
        }
    }

    // ---------------------------------------------------------------- renderer
    @Override public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        bufs.clear();
        int v = shader(GLES20.GL_VERTEX_SHADER, VS), f = shader(GLES20.GL_FRAGMENT_SHADER, FS);
        prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, v); GLES20.glAttachShader(prog, f);
        GLES20.glLinkProgram(prog);
        aPos = GLES20.glGetAttribLocation(prog, "aPos"); aNor = GLES20.glGetAttribLocation(prog, "aNor"); aCol = GLES20.glGetAttribLocation(prog, "aCol");
        uMvp = GLES20.glGetUniformLocation(prog, "uMvp"); uModel = GLES20.glGetUniformLocation(prog, "uModel"); uColor = GLES20.glGetUniformLocation(prog, "uColor");
        uAlpha = GLES20.glGetUniformLocation(prog, "uAlpha"); uLight = GLES20.glGetUniformLocation(prog, "uLight"); uSun = GLES20.glGetUniformLocation(prog, "uSun");
        uAmb = GLES20.glGetUniformLocation(prog, "uAmb"); uUnlit = GLES20.glGetUniformLocation(prog, "uUnlit"); uEmis = GLES20.glGetUniformLocation(prog, "uEmis");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        scene = new Scene3(this);
        scene.init();
    }

    @Override public void onSurfaceChanged(GL10 unused, int w, int h) { vw = w; vh = h; GLES20.glViewport(0, 0, w, h); aspect = w / (float) Math.max(1, h); }

    @Override public void onDrawFrame(GL10 unused) {
        SceneData d = data;
        if (d == null || scene == null) { GLES20.glClearColor(0.05f, 0.08f, 0.12f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); return; }
        scene.render(d, vw, vh);
    }

    static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    // ---------------------------------------------------------------- Gfx
    @Override public int upload(float[] verts, short[] idx) {
        int[] ids = new int[2];
        GLES20.glGenBuffers(2, ids, 0);
        FloatBuffer fb = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(verts).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.length * 4, fb, GLES20.GL_STATIC_DRAW);
        ShortBuffer sbuf = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        sbuf.put(idx).position(0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ids[1]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.length * 2, sbuf, GLES20.GL_STATIC_DRAW);
        bufs.add(new int[]{ids[0], ids[1], idx.length});
        return bufs.size() - 1;
    }
    @Override public void delete(int h) {
        int[] b = bufs.get(h);
        if (b[2] < 0) return;
        GLES20.glDeleteBuffers(2, new int[]{b[0], b[1]}, 0);
        b[2] = -1;
    }
    @Override public void begin(int w, int h, float[] viewProj, float r, float g, float b) {
        GLES20.glViewport(0, 0, w, h);
        GLES20.glClearColor(r, g, b, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(prog);
        vp = viewProj;
    }
    @Override public void env(float lx, float ly, float lz, float sr, float sg, float sb, float ar, float ag, float ab) {
        float l = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        GLES20.glUniform3f(uLight, lx / l, ly / l, lz / l);
        GLES20.glUniform3f(uSun, sr, sg, sb);
        GLES20.glUniform3f(uAmb, ar, ag, ab);
    }
    @Override public void draw(int h, float[] model, float r, float g, float b, float a, int flags) {
        int[] bf = bufs.get(h);
        if (bf[2] <= 0) return;
        M4.mul(mvp, vp, model);
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform3f(uColor, r, g, b);
        GLES20.glUniform1f(uAlpha, a);
        GLES20.glUniform1f(uUnlit, (flags & F_UNLIT) != 0 ? 1f : 0f);
        GLES20.glUniform1f(uEmis, (flags & F_EMISSIVE) != 0 ? 0.12f : 0f);
        boolean blend = (flags & F_BLEND) != 0;
        if (blend) { GLES20.glEnable(GLES20.GL_BLEND); GLES20.glDepthMask(false); }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bf[0]);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bf[1]);
        GLES20.glEnableVertexAttribArray(aPos); GLES20.glEnableVertexAttribArray(aNor); GLES20.glEnableVertexAttribArray(aCol);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 36, 0);
        GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 36, 12);
        GLES20.glVertexAttribPointer(aCol, 3, GLES20.GL_FLOAT, false, 36, 24);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, bf[2], GLES20.GL_UNSIGNED_SHORT, 0);
        if (blend) { GLES20.glDisable(GLES20.GL_BLEND); GLES20.glDepthMask(true); }
    }
    @Override public void end() { }
}
