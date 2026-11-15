package com.yotta.fc;

/** Broadcast style 3D camera that follows the action. */
final class Cam3 {
    float tx = Match.L / 2, tz = Match.CY;     // look-at on the ground
    float zoom = 1f;                          // >1 = closer
    float height = 17f, back = 27f, fov = 0.62f;
    float shakeX, shakeY;
    float aspect = 2.2f;
    final float[] eye = new float[3], tgt = new float[3];
    final float[] proj = new float[16], view = new float[16], vp = new float[16];

    void update(Match m, float dt, float zoomMul) {
        float gx, gz;
        if (m.mode == Match.M_KICKOFF) { gx = Match.L / 2; gz = Match.CY; }
        else if (m.mode == Match.M_SET && m.spType != Match.SP_NONE) {
            gx = m.spX; gz = m.spY * 0.7f + Match.CY * 0.3f;
            if (m.spType == Match.SP_PENALTY || m.spType == Match.SP_CORNER || (m.spType == Match.SP_FREEKICK && Math.abs(m.goalX(m.S[m.spSide]) - m.spX) < 36)) gx = (m.spX + m.goalX(m.S[m.spSide])) * 0.5f;
        } else if (m.mode == Match.M_PEN) { gx = Match.L - 16; gz = Match.CY; }
        else {
            gx = m.bx + m.bvx * 0.4f; gz = Match.CY * 0.45f + m.by * 0.55f;
            if (m.owner != null) gx = m.owner.x + m.owner.vx * 0.55f;
            if (m.mode == Match.M_GOAL) gx = m.bx;
        }
        gx = U.clamp(gx, 10, Match.L - 10);
        gz = U.clamp(gz, 14, Match.W - 14);
        float k = Math.min(1f, dt * (m.mode == Match.M_PLAY ? 3.2f : 2.0f));
        tx += (gx - tx) * k; tz += (gz - tz) * k * 0.8f;
        float zt = zoomMul;
        if (m.mode == Match.M_SET && m.spType == Match.SP_PENALTY || m.mode == Match.M_PEN) zt *= 1.25f;
        if (m.mode == Match.M_GOAL) zt *= 1.3f;
        zoom += (zt - zoom) * Math.min(1f, dt * 2f);
        if (m.shake > 0) { shakeX = (float) Math.sin(m.time * 90) * 0.25f * m.shake; shakeY = (float) Math.cos(m.time * 77) * 0.2f * m.shake; } else shakeX = shakeY = 0;
        compute();
    }

    void compute() {
        float d = 1f / zoom;
        eye[0] = tx + shakeX; eye[1] = height * d + shakeY; eye[2] = tz + back * d;
        tgt[0] = tx + shakeX; tgt[1] = 0; tgt[2] = tz;
        M4.perspective(proj, fov, aspect, 1f, 400f);
        M4.lookAt(view, eye[0], eye[1], eye[2], tgt[0], tgt[1], tgt[2]);
        M4.mul(vp, proj, view);
    }

    void fill(SceneData d) {
        d.eye[0] = eye[0]; d.eye[1] = eye[1]; d.eye[2] = eye[2];
        d.target[0] = tgt[0]; d.target[1] = tgt[1]; d.target[2] = tgt[2];
        d.fov = fov; d.aspect = aspect;
    }
}
