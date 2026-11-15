package com.yotta.fc;

/** Human control of a side. */
final class Ctl {
    static void setControlled(Match m, Match.Side sd, Pawn p) {
        if (sd.ctl == p) return;
        sd.ctl = p;
        sd.ctlLock = 0.25f;
    }

    static void update(Match m, float dt) {
        if (m.autoPlay) return;
        for (int si = 0; si < 2; si++) {
            Match.Side sd = m.S[si];
            if (!sd.human) continue;
            for (Pawn p : sd.pw) p.human = false;
            if (m.mode != Match.M_PLAY && m.mode != Match.M_SET) continue;
            sd.ctlLock = Math.max(0, sd.ctlLock - dt);
            select(m, sd, dt);
            Pawn c = sd.ctl;
            if (c == null || c.st == Pawn.SENT) continue;
            if (m.mode == Match.M_SET) continue; // taker moved by AI / set piece UI
            Input in = m.in;
            if (si != m.humanSide) continue; // single human input
            c.human = true;
            // held-time tracking
            in.shootHeld = in.shoot ? in.shootHeld + dt : 0;
            move(m, c, in);
            if (m.owner == c) attackButtons(m, sd, c, in);
            else if (m.owner != null && m.owner.side != c.side) defendButtons(m, sd, c, in);
            else looseButtons(m, sd, c, in);
            // keeper distribution
            if (m.owner != null && m.owner.side == si && m.owner.role == Player.GK) {
                Pawn g = m.owner;
                if (in.passP) AI.keeperDistribute(m, g, 0);
                else if (in.lobP || in.shootP || in.throughP) AI.keeperDistribute(m, g, 1);
            }
            if (in.switchP) switchPlayer(m, sd, c);
        }
    }

    static void move(Match m, Pawn c, Input in) {
        float mag = in.stickLen();
        if (mag < 0.1f) { c.wvx = c.wvy = 0; c.wantSprint = false; return; }
        float dx = in.sx / mag, dy = in.sy / mag;
        float k = U.clamp(mag * 1.1f, 0, 1);
        c.wantSprint = in.sprint;
        float top = AI.topSpeed(m, c, in.sprint) * k;
        c.wvx = dx * top; c.wvy = dy * top;
    }

    static void select(Match m, Match.Side sd, float dt) {
        int si = m.sideIdx(sd);
        Pawn cur = sd.ctl;
        if (cur != null && (cur.st == Pawn.SENT || cur.role == Player.GK && m.owner != cur)) cur = null;
        if (m.mode == Match.M_SET) { if (sd.ctl == null) sd.ctl = m.spTaker; return; }
        if (m.owner != null && m.owner.side == si) {
            if (m.owner.role != Player.GK) { sd.ctl = m.owner; return; }
            sd.ctl = m.owner; return;
        }
        if (m.passTarget != null && m.passTarget.side == si && m.owner == null) { sd.ctl = m.passTarget; return; }
        // defending / loose: nearest to ball, with hysteresis
        if (sd.ctlLock > 0 && cur != null) return;
        float tx = m.owner != null ? m.owner.x : m.bx + m.bvx * 0.3f, ty = m.owner != null ? m.owner.y : m.by + m.bvy * 0.3f;
        Pawn best = null; float bd = 1e9f;
        for (Pawn p : sd.pw) {
            if (p.role == Player.GK || p.st == Pawn.SENT || p.st == Pawn.DOWN) continue;
            float d = U.dist(p.x, p.y, tx, ty);
            if (d < bd) { bd = d; best = p; }
        }
        if (cur == null) { sd.ctl = best; return; }
        float cd = U.dist(cur.x, cur.y, tx, ty);
        if (best != null && best != cur && bd + 2.2f < cd) { sd.ctl = best; sd.ctlLock = 0.6f; }
    }

    static void switchPlayer(Match m, Match.Side sd, Pawn cur) {
        if (m.owner != null && m.owner.side == cur.side) return;
        float tx = m.owner != null ? m.owner.x : m.bx, ty = m.owner != null ? m.owner.y : m.by;
        Pawn best = null; float bd = 1e9f;
        for (Pawn p : sd.pw) {
            if (p == cur || p.role == Player.GK || p.st == Pawn.SENT) continue;
            // favour players between the ball and own goal
            float d = U.dist(p.x, p.y, tx, ty);
            if (d < bd) { bd = d; best = p; }
        }
        if (best != null) { sd.ctl = best; sd.ctlLock = 1.0f; }
    }

    // --------------------------------------------------------- attack
    public static Pawn pickTarget(Match m, Pawn c, float dx, float dy, int kind) {
        Match.Side sd = m.S[c.side];
        float dl = U.len(dx, dy);
        if (dl < 0.2f) { dx = c.fx; dy = c.fy; dl = 1; } else { dx /= dl; dy /= dl; }
        Pawn best = null; float bs = -99;
        for (Pawn q : sd.pw) {
            if (q == c || q.st == Pawn.SENT || q.role == Player.GK && c.role != Player.GK) continue;
            float vx = q.x - c.x, vy = q.y - c.y, d = U.len(vx, vy);
            if (d < 3) continue;
            float cos = (vx * dx + vy * dy) / d;
            float sc = cos * 4f - d / 45f;
            if (kind == Match.K_THROUGH) sc += (m.depthOf(sd, q.x) - m.depthOf(sd, c.x)) * 0.05f - (cos < 0.5f ? 3 : 0) + (d > 12 ? 1 : 0);
            if (kind == Match.K_LOB) { sc += d > 16 ? 0.8f : -1f; }
            if (kind == Match.K_GROUND && cos < 0.2f) sc -= 2;
            // prefer open targets
            float free = 99;
            for (Pawn o : m.S[1 - c.side].pw) free = Math.min(free, U.dist(o.x, o.y, q.x, q.y));
            sc += Math.min(free, 6f) * 0.12f;
            if (sc > bs) { bs = sc; best = q; }
        }
        return best;
    }

    static void attackButtons(Match m, Match.Side sd, Pawn c, Input in) {
        float sx = in.sx, sy = in.sy;
        if (c.role == Player.GK) return;
        if (in.passP) {
            Pawn t = pickTarget(m, c, sx, sy, Match.K_GROUND);
            if (t != null) AI.passTo(m, c, t, Match.K_GROUND, 0.5f);
        } else if (in.throughP) {
            Pawn t = pickTarget(m, c, sx, sy, Match.K_THROUGH);
            if (t != null) AI.passTo(m, c, t, Match.K_THROUGH, 0.6f);
        } else if (in.lobP) {
            float dep = m.depthOf(sd, c.x);
            boolean wide = Math.abs(c.y - Match.CY) > 16;
            Pawn t = null;
            if (wide && dep > Match.L * 0.7f) t = AI.boxTarget(m, c, sd);
            if (t != null) AI.passTo(m, c, t, Match.K_CROSS, 0.7f);
            else {
                t = pickTarget(m, c, sx, sy, Match.K_LOB);
                if (t != null) AI.passTo(m, c, t, Match.K_LOB, 0.6f);
            }
        } else if (in.shootR) {
            float pw = U.clamp(in.shootHeld / 0.7f, 0.2f, 1f);
            if (in.shootHeld < 0.01f) pw = 0.5f;
            shootFromInput(m, c, in, pw);
        }
    }

    static void shootFromInput(Match m, Pawn c, Input in, float pw) {
        Match.Side sd = m.S[c.side];
        float gx = m.goalX(sd);
        float aimY;
        float mag = in.stickLen();
        Pawn gk = m.S[1 - c.side].pw[0];
        if (mag > 0.35f && Math.abs(in.sy) > 0.25f) aimY = U.clamp(in.sy * 3.6f, -3.3f, 3.3f);
        else {
            float side = gk.y > Match.CY ? -1 : 1;
            if (Math.abs(gk.y - Match.CY) < 0.7f) side = U.chance(0.5f) ? 1 : -1;
            aimY = side * U.rnd(2.1f, 3.1f);
        }
        float aimZ = pw > 0.85f ? U.rnd(1.3f, 2.1f) : (pw > 0.55f ? U.rnd(0.5f, 1.4f) : U.rnd(0.2f, 0.8f));
        // face goal
        c.fx = gx > c.x ? 1 : -1; c.fy = 0;
        AI.shoot(m, c, aimY, aimZ, pw, pw > 0.5f && U.dist(c.x, c.y, gx, Match.CY) > 16);
    }

    // --------------------------------------------------------- defend
    static void defendButtons(Match m, Match.Side sd, Pawn c, Input in) {
        Pawn car = m.owner;
        float d = U.dist(c.x, c.y, car.x, car.y);
        if (in.shootP && c.tackleCd <= 0) {
            if (d < 2.0f) {
                c.fx = (car.x - c.x) / Math.max(0.1f, d); c.fy = (car.y - c.y) / Math.max(0.1f, d);
                m.tackle(c, car, false);
            } else { c.tackleCd = 0.4f; }
        } else if (in.lobP && c.tackleCd <= 0) {
            AI.doSlide(m, c);
        } else if (in.throughP) {
            // call teammate to press
            Pawn best = null; float bd = 1e9f;
            for (Pawn p : sd.pw) {
                if (p == c || p.role == Player.GK || p.st == Pawn.SENT) continue;
                float dd = U.dist(p.x, p.y, car.x, car.y);
                if (dd < bd) { bd = dd; best = p; }
            }
            if (best != null) best.pressT = 3.0f;
        } else if (in.passP) switchPlayer(m, sd, c);
    }

    static void looseButtons(Match m, Match.Side sd, Pawn c, Input in) {
        if (in.passP || in.shootP) switchPlayer(m, sd, c);
        else if (in.lobP && c.tackleCd <= 0 && U.dist(c.x, c.y, m.bx, m.by) < 3.5f) AI.doSlide(m, c);
    }

    // --------------------------------------------------- set pieces
    static void setPieceHuman(Match m, float dt) {
        Input in = m.in;
        Pawn tk = m.spTaker;
        Match.Side sd = m.S[m.spSide];
        float gx = m.goalX(sd);
        in.shootHeld = in.shoot ? in.shootHeld + dt : 0;
        int t = m.spType;
        if (t == Match.SP_FREEKICK || t == Match.SP_PENALTY || t == Match.SP_CORNER) {
            // move aim reticle
            float sp = 14f * dt;
            m.spAimX = U.clamp(m.spAimX + in.sx * sp * (t == Match.SP_CORNER ? 1.6f : 0.35f), 0, Match.L);
            m.spAimY = U.clamp(m.spAimY + in.sy * sp * (t == Match.SP_CORNER ? 1.6f : 1.0f), t == Match.SP_CORNER ? 2 : Match.CY - 7, t == Match.SP_CORNER ? Match.W - 2 : Match.CY + 7);
            if (t != Match.SP_CORNER) { m.spAimX = gx; }
            if (t == Match.SP_CORNER && in.sx == 0 && in.sy == 0 && m.spTimer < 3.0f) { m.spAimX = gx - sd.dir * 8; m.spAimY = Match.CY; }
        }
        boolean fire = in.shootR || in.passP || in.lobP || in.throughP;
        if (!fire) return;
        float pw = U.clamp(in.shootHeld / 0.7f, 0.25f, 1f);
        if (in.shootR && in.shootHeld < 0.01f) pw = 0.6f;
        m.owner = null;
        switch (t) {
            case Match.SP_PENALTY: {
                float aimY = U.clamp(m.spAimY - Match.CY, -3.3f, 3.3f);
                float aimZ = pw > 0.8f ? 1.9f : (pw > 0.5f ? 1.0f : 0.4f);
                AI.shoot(m, tk, aimY, aimZ, Math.max(0.55f, pw), false);
                AI.penaltyKeeperDive(m, tk, -1);
                m.finishSetPiece();
                break;
            }
            case Match.SP_FREEKICK: {
                float gd = U.dist(m.spX, m.spY, gx, Match.CY);
                if (in.shootR && gd < 38) {
                    float aimY = U.clamp(m.spAimY - Match.CY, -3.3f, 3.3f);
                    float aimZ = U.clamp(0.4f + pw * 1.8f, 0.4f, 2.2f);
                    AI.shoot(m, tk, aimY, aimZ, Math.max(0.6f, pw), true);
                } else {
                    int kind = in.lobP ? Match.K_CROSS : (in.throughP ? Match.K_THROUGH : Match.K_GROUND);
                    Pawn tg = in.lobP ? AI.boxTarget(m, tk, sd) : pickTarget(m, tk, in.sx, in.sy, kind);
                    if (tg == null) tg = AI.bestTarget(m, tk, false);
                    AI.passTo(m, tk, tg, kind, pw);
                }
                m.finishSetPiece();
                break;
            }
            case Match.SP_CORNER: {
                float ax = m.spAimX, ay = m.spAimY;
                Pawn aimed = null; float bd = 1e9f;
                for (Pawn q : sd.pw) { if (q == tk || q.role == Player.GK) continue; float d = U.dist(q.tx, q.ty, ax, ay); if (d < bd) { bd = d; aimed = q; } }
                if (in.passP) { Pawn tg = AI.bestTarget(m, tk, false); AI.passTo(m, tk, tg, Match.K_GROUND, 0.5f); }
                else AI.kickToPoint(m, tk, ax, ay, U.clamp(0.9f + U.dist(tk.x, tk.y, ax, ay) * 0.04f, 1f, 1.9f) * (in.shootR ? 1.0f : 0.95f), 0.35f, bd < 5 ? aimed : null);
                m.finishSetPiece();
                break;
            }
            case Match.SP_THROW: {
                Pawn tg = pickTarget(m, tk, in.sx, in.sy, Match.K_GROUND);
                if (tg == null) tg = AI.bestTarget(m, tk, false);
                AI.throwBall(m, tk, tg, U.dist(tk.x, tk.y, tg.x, tg.y));
                m.finishSetPiece();
                break;
            }
            case Match.SP_GOALKICK: {
                Pawn tg = pickTarget(m, tk, in.sx, in.sy, in.passP ? Match.K_GROUND : Match.K_LOB);
                if (tg == null) tg = AI.bestTarget(m, tk, false);
                AI.passTo(m, tk, tg, in.passP ? Match.K_GROUND : Match.K_LOB, 0.9f);
                m.finishSetPiece();
                break;
            }
            default: AI.executeSetPiece(m);
        }
    }
}
