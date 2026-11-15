package com.yotta.fc;

/** Decision making for computer controlled players. */
final class AI {
    static final int PL_NONE = 0, PL_DRIB = 1;

    static float lvlNoise(Match m, Pawn p) {
        if (m.autoPlay || p.side == m.humanSide) return 1f;
        return new float[]{1.5f, 1.12f, 0.92f, 0.78f}[m.diff];
    }
    static float lvlReact(Match m, Pawn p) {
        if (m.autoPlay || p.side == m.humanSide) return 0.26f;
        return new float[]{0.50f, 0.34f, 0.24f, 0.16f}[m.diff];
    }

    static float topSpeed(Match m, Pawn p, boolean sprint) {
        float ms = p.maxSpeed();
        float stamF = 0.82f + 0.18f * p.stamina;
        float top = ms * stamF * (sprint ? 1f : 0.76f);
        if (m.owner == p) top *= 0.94f + p.pl.dri * 0.0006f;
        return top;
    }

    /** Move towards (tx,ty). sp = fraction of top speed. */
    static void goTo(Match m, Pawn p, float tx, float ty, float sp, boolean sprint) {
        float dx = tx - p.x, dy = ty - p.y, d = U.len(dx, dy);
        p.wantSprint = sprint;
        if (d < 0.25f) { p.wvx = p.wvy = 0; return; }
        float top = topSpeed(m, p, sprint) * sp;
        float s = Math.min(top, d * 3.2f);
        p.wvx = dx / d * s; p.wvy = dy / d * s;
    }

    // ---------------------------------------------------------------- think
    static void think(Match m, Pawn p, float dt) {
        if (m.mode == Match.M_PEN || m.mode == Match.M_GOAL) return;
        if (p.st == Pawn.SENT) return;
        if (m.mode == Match.M_SET || m.mode == Match.M_KICKOFF) return;
        if (p.st != Pawn.N && p.st != Pawn.CELEB) { p.wvx = p.wvy = 0; return; }
        if (p.role == Player.GK) { keeper(m, p, dt); return; }
        Match.Side s = m.S[p.side];
        if (m.owner == p) { carrier(m, p, dt); return; }
        p.thinkT -= dt;
        boolean teamBall = m.owner != null ? m.owner.side == p.side : false;
        if (m.passTarget == p && m.owner == null) {
            float[] ip = intercept(m, p);
            goTo(m, p, ip[0], ip[1], 1f, true);
            return;
        }
        if (m.owner == null) {
            // loose ball
            if (p == s.press1 && ballRelevant(m, p)) {
                float[] ip = intercept(m, p);
                goTo(m, p, ip[0], ip[1], 1f, true);
                tryLooseSlide(m, p);
                return;
            }
            boolean mine = m.lastTouchSide == p.side;
            if (p == s.press2 && !mine && Player.group(p.role) <= 2) {
                float[] ip = intercept(m, p);
                goTo(m, p, (ip[0] + p.ax) * 0.5f, (ip[1] + p.ay) * 0.5f, 0.9f, false);
                return;
            }
            offBall(m, p, s, mine);
            return;
        }
        if (teamBall) offBall(m, p, s, true);
        else defend(m, p, s, dt);
    }

    static boolean ballRelevant(Match m, Pawn p) {
        return U.dist(p.x, p.y, m.bx, m.by) < 40;
    }

    static void tryLooseSlide(Match m, Pawn p) {
        // occasional slide for loose ball on defensive phase
        float d = U.dist(p.x, p.y, m.bx, m.by);
        if (d > 1.4f && d < 2.6f && m.ballSpeed() < 4 && p.tackleCd <= 0 && U.chance(0.01f) && m.lastTouchSide != p.side && Player.group(p.role) <= 2) {
            doSlide(m, p);
        }
    }

    static void doSlide(Match m, Pawn p) {
        float dx = m.bx - p.x, dy = m.by - p.y, d = Math.max(0.1f, U.len(dx, dy));
        if (m.owner != null) { dx = m.owner.x - p.x; dy = m.owner.y - p.y; d = Math.max(0.1f, U.len(dx, dy)); }
        p.fx = dx / d; p.fy = dy / d;
        p.st = Pawn.SLIDE; p.stT = 0; p.planKind = 0; p.tackleCd = 1.8f;
        p.vx = p.fx * 7.6f; p.vy = p.fy * 7.6f;
    }

    // ------------------------------------------------ intercept prediction
    static float[] intercept(Match m, Pawn p) {
        float sp = topSpeed(m, p, true) * 0.95f;
        float k = 0.46f;
        float bx = m.bx, by = m.by, vx = m.bvx, vy = m.bvy, z = m.bz, vz = m.bvz;
        float tland = 0;
        if (z > 0.2f || vz > 0.5f) {
            float disc = vz * vz + 2 * 9.8f * z;
            tland = (vz + (float) Math.sqrt(Math.max(0, disc))) / 9.8f;
        }
        float[] res = new float[2];
        for (float t = 0.1f; t <= 2.8f; t += 0.1f) {
            float px, py, zz;
            if (t < tland) { px = bx + vx * t * 0.95f; py = by + vy * t * 0.95f; zz = z + vz * t - 4.9f * t * t; }
            else {
                float lx = bx + vx * tland * 0.95f, ly = by + vy * tland * 0.95f;
                float tt = t - tland;
                float f = (1 - (float) Math.exp(-k * tt)) / k;
                float lvx = vx * 0.6f, lvy = vy * 0.6f;
                px = lx + lvx * f; py = ly + lvy * f; zz = 0;
            }
            px = U.clamp(px, -1, Match.L + 1); py = U.clamp(py, -1, Match.W + 1);
            float need = U.dist(p.x, p.y, px, py) - 0.4f;
            if (need <= sp * t && zz < 1.4f) { res[0] = px; res[1] = py; return res; }
            res[0] = px; res[1] = py;
        }
        return res;
    }

    // ---------------------------------------------------------- carrier
    static void carrier(Match m, Pawn p, float dt) {
        Match.Side s = m.S[p.side];
        p.thinkT -= dt;
        float gx = m.goalX(s);
        if (p.thinkT <= 0) {
            p.thinkT = lvlReact(m, p) * (0.8f + U.rnd() * 0.6f);
            decide(m, p, s);
            if (m.owner != p) return;
        }
        // execute dribble plan
        float dx = p.planX - p.x, dy = p.planY - p.y, d = U.len(dx, dy);
        if (d < 0.5f) { dx = s.dir; dy = 0; d = 1; }
        float press = m.pressureOn(p);
        boolean sprint = press < 0.8f || depthPct(m, s, p) > 0.5f;
        float top = topSpeed(m, p, sprint);
        p.wantSprint = sprint;
        p.wvx = dx / d * top; p.wvy = dy / d * top;
        // 1v1 tackle threat handled by defenders
    }

    static float depthPct(Match m, Match.Side s, Pawn p) { return m.depthOf(s, p.x) / Match.L; }

    static void decide(Match m, Pawn p, Match.Side s) {
        float gx = m.goalX(s);
        float dgc = U.dist(p.x, p.y, gx, Match.CY);
        float press = m.pressureOn(p);
        float aggr = 0.5f + (s.style - 1) * 0.15f + (p.role == Player.ST ? 0.15f : 0) + (p.role == Player.GK ? -0.5f : 0);
        float dep = m.depthOf(s, p.x);
        // --- shot
        float q = shotQuality(m, p, s, gx);
        boolean shoot = false;
        if (dgc < 13 && q > 0.26f) shoot = U.chance(0.92f);
        else if (dgc < 26 && q > 0.33f) shoot = U.chance(0.45f + 0.5f * aggr + (p.pl.sho - 65) * 0.005f);
        else if (dgc < 33 && q > 0.28f && p.pl.sho > 72) shoot = U.chance(0.22f);
        if (shoot) {
            float aimY, aimZ;
            Pawn gk = m.S[1 - p.side].pw[0];
            float side = gk.y > Match.CY ? -1 : 1;
            if (Math.abs(gk.y - Match.CY) < 0.8f) side = U.chance(.5f) ? 1 : -1;
            aimY = side * U.rnd(2.2f, 3.2f);
            aimZ = U.chance(0.5f) ? U.rnd(0.25f, 0.8f) : U.rnd(1.2f, 2.1f);
            float pw = U.clamp(0.55f + dgc * 0.012f + U.rnd(-0.1f, 0.15f), 0.4f, 1f);
            if (dgc < 9) pw = U.rnd(0.5f, 0.8f);
            shoot(m, p, aimY, aimZ, pw, dgc > 17 && U.chance(0.35f));
            return;
        }
        // --- cross from the wing
        boolean wide = Math.abs(p.y - Match.CY) > 18;
        if (wide && dep > Match.L * 0.76f && dep < Match.L - 3 && U.chance(0.55f)) {
            Pawn t = boxTarget(m, p, s);
            if (t != null) { passTo(m, p, t, Match.K_CROSS, 0.7f); return; }
        }
        // --- pass evaluation
        Pawn best = null; float bs = -999; int bk = Match.K_GROUND;
        float ownThreat = press;
        for (Pawn q2 : s.pw) {
            if (q2 == p || q2.st == Pawn.SENT || q2.st == Pawn.DOWN) continue;
            float sc = scorePass(m, p, q2, s, press);
            int kind = Match.K_GROUND;
            float d = U.dist(p.x, p.y, q2.x, q2.y);
            if (sc > -100) {
                if (m.depthOf(s, q2.x) > dep + 8 && q2.speed() > 3.5f && q2.role != Player.GK) { sc += 1.5f; kind = Match.K_THROUGH; }
                else if (d > 26 && p.pl.pas > 70) { kind = Match.K_LOB; sc -= 1.5f; }
            }
            sc += U.rnd(-1.0f, 1.0f) * (m.diff >= 2 ? 0.6f : 1.2f);
            if (sc > bs) { bs = sc; best = q2; bk = kind; }
        }
        // dribble evaluation
        float free = freeSpaceAhead(m, p, s);
        float dribScore = free * 1.0f + (p.pl.dri - 60) * 0.06f - press * 6f + (aggr - 0.5f) * 4f + (dep > Match.L * 0.6f ? 3 : 0);
        float passThreshold = 7.5f - press * 3f;
        boolean holdOk = m.ownerT > 0.45f || press > 0.55f;
        if (best != null && holdOk && (bs > passThreshold && bs > dribScore * 0.95f || (press > 0.6f && bs > 1f) || m.ownerT > 3.2f)) {
            if (p.role != Player.GK) { passTo(m, p, best, bk, 0.5f + U.rnd() * 0.3f); return; }
        }
        if (press > 0.75f && dep < Match.L * 0.35f && (best == null || bs < 2)) {
            // clear the ball
            clear(m, p, s);
            return;
        }
        // dribble
        planDribble(m, p, s);
    }

    static void planDribble(Match m, Pawn p, Match.Side s) {
        float fx = s.dir, fy = 0;
        float gx = m.goalX(s);
        float dep = m.depthOf(s, p.x);
        // head toward goal, drift to centre when in final third
        float ty = p.y;
        if (dep > Match.L * 0.65f) ty = p.y + (Match.CY - p.y) * 0.5f;
        else ty = p.y + (p.ay - p.y) * 0.3f;
        float vx = fx * 10, vy = ty - p.y;
        // avoid opponents
        for (Pawn q : m.S[1 - p.side].pw) {
            if (q.st == Pawn.SENT) continue;
            float dx = q.x - p.x, dy = q.y - p.y;
            float d = U.len(dx, dy);
            if (d < 6 && d > 0.1f) {
                float ahead = (dx * fx) ;
                if (ahead > -1) {
                    float w = (6 - d) / 6f;
                    float side = dy >= 0 ? -1 : 1;
                    vy += side * w * 9 * (ahead > 0 ? 1 : 0.4f);
                    vx -= w * 4 * (ahead > 0 ? 1 : 0);
                }
            }
        }
        // stay inbounds
        if (p.y < 4 && vy < 0) vy = 0;
        if (p.y > Match.W - 4 && vy > 0) vy = 0;
        float vl = U.len(vx, vy);
        p.planX = p.x + vx / vl * 8; p.planY = p.y + vy / vl * 8;
        p.planKind = PL_DRIB;
    }

    static float freeSpaceAhead(Match m, Pawn p, Match.Side s) {
        float best = 14;
        for (Pawn q : m.S[1 - p.side].pw) {
            if (q.st == Pawn.SENT) continue;
            float dx = (q.x - p.x) * s.dir, dy = q.y - p.y;
            if (dx < -0.5f) continue;
            float lat = Math.abs(dy);
            if (lat < 3.2f && dx < best) best = dx;
        }
        return best;
    }

    static float shotQuality(Match m, Pawn p, Match.Side s, float gx) {
        float dgc = U.dist(p.x, p.y, gx, Match.CY);
        if (dgc > 34) return 0;
        float a1 = (float) Math.atan2(Match.CY - Match.GH - p.y, gx - p.x), a2 = (float) Math.atan2(Match.CY + Match.GH - p.y, gx - p.x);
        float ang = Math.abs(a1 - a2);
        float q = U.clamp(1 - dgc / 32f, 0, 1) * 0.6f + U.clamp(ang / 0.3f, 0, 1) * 0.4f;
        // blockers
        int blockers = 0;
        float sx = gx - p.x, sy = Match.CY - p.y, sl = Math.max(1, U.len(sx, sy));
        for (Pawn o : m.S[1 - p.side].pw) {
            if (o.role == Player.GK || o.st == Pawn.SENT) continue;
            float ox = o.x - p.x, oy = o.y - p.y;
            float proj = (ox * sx + oy * sy) / sl;
            if (proj < 0.8f || proj > sl) continue;
            float perp = Math.abs(ox * sy - oy * sx) / sl;
            if (perp < 1.1f) blockers++;
        }
        return q - 0.2f * blockers;
    }

    static float scorePass(Match m, Pawn p, Pawn q, Match.Side s, float press) {
        float d = U.dist(p.x, p.y, q.x, q.y);
        if (d < 3.5f) return -200;
        // lane clearance
        float clear = 99;
        float dx = q.x - p.x, dy = q.y - p.y;
        for (Pawn o : m.S[1 - p.side].pw) {
            if (o.st == Pawn.SENT) continue;
            float ox = o.x - p.x, oy = o.y - p.y;
            float proj = (ox * dx + oy * dy) / (d * d);
            if (proj < 0.05f || proj > 1.0f) {
                if (proj > 1.0f) { float e = U.dist(o.x, o.y, q.x, q.y); if (e < clear) clear = e; }
                continue;
            }
            float px = p.x + dx * proj, py = p.y + dy * proj;
            float e = U.dist(o.x, o.y, px, py) / (0.5f + proj * 0.6f);
            if (e < clear) clear = e;
        }
        if (clear < 1.2f) return -150;
        float free = 99;
        for (Pawn o : m.S[1 - p.side].pw) {
            if (o.st == Pawn.SENT) continue;
            float e = U.dist(o.x, o.y, q.x, q.y);
            if (e < free) free = e;
        }
        float dp = m.depthOf(s, p.x), dq = m.depthOf(s, q.x);
        float gain = dq - dp;
        float sc = Math.min(clear, 6f) * 1.4f + Math.min(free, 8f) * 1.1f;
        sc += gain > 0 ? gain * 0.30f : gain * (0.18f + (1 - press) * 0.12f);
        sc += dq / Match.L * 6f;
        if (d > 16) sc -= (d - 16) * (p.pl.pas > 78 ? 0.18f : 0.38f);
        if (q.role == Player.GK && p.role != Player.GK) sc -= 5 - press * 3;
        if (p.role == Player.GK) sc += d * 0.05f;
        if ((dq > m.S[p.side].offDepth + 0.3f && dq > Match.L / 2) && !U.chance(0.14f)) sc -= 60;
        sc -= Math.abs(q.y - p.y) > 25 ? 1.5f : 0;
        return sc - 4f;
    }

    static Pawn bestTarget(Match m, Pawn p, boolean allowGk) {
        Match.Side s = m.S[p.side];
        Pawn best = null; float bs = -999;
        float press = m.pressureOn(p);
        for (Pawn q : s.pw) {
            if (q == p || q.st == Pawn.SENT || (q.role == Player.GK && !allowGk)) continue;
            float sc = scorePass(m, p, q, s, press);
            if (sc > bs) { bs = sc; best = q; }
        }
        return best;
    }

    static Pawn boxTarget(Match m, Pawn p, Match.Side s) {
        Pawn best = null; float bd = 1e9f;
        float gx = m.goalX(s);
        for (Pawn q : s.pw) {
            if (q == p || q.role == Player.GK || q.st == Pawn.SENT) continue;
            float d = U.dist(q.x, q.y, gx - s.dir * 9, Match.CY);
            if (d < bd && d < 15) { bd = d; best = q; }
        }
        return best;
    }

    static void clear(Match m, Pawn p, Match.Side s) {
        // hoof upfield, away from pressure, to the flank
        float ty = p.y < Match.CY ? 8 : Match.W - 8;
        float tx = m.xOfDepth(s, Math.min(Match.L - 10, m.depthOf(s, p.x) + 32));
        float dx = tx - p.x, dy = ty - p.y, d = U.len(dx, dy);
        float v = 17 + p.pl.pas * 0.06f;
        float T = 1.9f;
        float ang = (float) Math.atan2(dy, dx) + U.gauss() * 0.14f;
        float vh = d / T * 1.05f;
        p.fx = (float) Math.cos(ang); p.fy = (float) Math.sin(ang);
        m.kick(p, (float) Math.cos(ang) * vh, (float) Math.sin(ang) * vh, 9f, 0, Match.K_CLEAR);
        m.passTarget = null;
    }

    // ------------------------------------------------------ passing physics
    static void passTo(Match m, Pawn p, Pawn t, int kind, float power) {
        Match.Side s = m.S[p.side];
        float dx = t.x - p.x, dy = t.y - p.y, d = Math.max(1f, U.len(dx, dy));
        float skill = p.pl.pas / 100f;
        float pr = m.pressureOn(p);
        float err = (0.010f + (1 - skill) * 0.07f) * (0.55f + d / 30f) * (1 + pr * 1.1f) * lvlNoise(m, p);
        float ax, ay, v, vz = 0, spin = 0;
        switch (kind) {
            case Match.K_THROUGH: {
                float lead = 6.5f + t.speed() * 1.3f;
                ax = t.x + s.dir * lead; ay = t.y + t.vy * 1.0f;
                float dd = U.dist(p.x, p.y, ax, ay);
                v = U.clamp(10.5f + dd * 0.78f, 12, 27) * (0.9f + 0.2f * power);
                break;
            }
            case Match.K_LOB: case Match.K_CROSS: {
                float T = U.clamp(0.55f + d * 0.042f, 0.8f, 2.4f) * (kind == Match.K_CROSS ? 0.92f : 1f);
                ax = t.x + t.vx * T * 0.8f; ay = t.y + t.vy * T * 0.8f;
                if (kind == Match.K_CROSS) { ax = t.x + t.vx * T * 0.6f; }
                float dd = Math.max(1f, U.dist(p.x, p.y, ax, ay));
                v = dd / T * 1.07f;
                vz = 4.9f * T * 1.02f;
                float sgn = ((ax - p.x) * (Match.CY - p.y) > 0) ? 1f : -1f;
                if (kind == Match.K_CROSS) spin = sgn * 0.3f;
                if (spin != 0) { float ang0 = (float) Math.atan2(ay - p.y, ax - p.x) - spin * T * 0.45f; ax = p.x + (float) Math.cos(ang0) * dd; ay = p.y + (float) Math.sin(ang0) * dd; }
                break;
            }
            default: {
                float v0 = U.clamp(7.5f + d * 0.78f, 9, 25) * (0.88f + 0.24f * power);
                float tt = d / (v0 * 0.85f);
                ax = t.x + t.vx * tt * 0.9f; ay = t.y + t.vy * tt * 0.9f;
                v = v0;
            }
        }
        float ang = (float) Math.atan2(ay - p.y, ax - p.x) + U.gauss() * err;
        v *= 1f + U.gauss() * 0.025f;
        p.fx = (float) Math.cos(ang); p.fy = (float) Math.sin(ang);
        m.kick(p, (float) Math.cos(ang) * v, (float) Math.sin(ang) * v, vz, spin, kind);
        m.passTarget = t;
        if (s.human && !m.autoPlay && p.role != Player.GK) Ctl.setControlled(m, s, t);
        if (s.human && !m.autoPlay && p.role == Player.GK) {}
    }

    /** Cross / lofted ball to a spot (set pieces). */
    static void kickToPoint(Match m, Pawn p, float ax, float ay, float T, float spinMag, Pawn aimed) {
        float dd = Math.max(1f, U.dist(p.x, p.y, ax, ay));
        float skill = p.pl.pas / 100f;
        float err = (0.008f + (1 - skill) * 0.06f) * (0.5f + dd / 40f) * lvlNoise(m, p);
        float sgn = ((ax - p.x) * (Match.CY - p.y) > 0) ? 1f : -1f;
        float spin = sgn * spinMag;
        float ang = (float) Math.atan2(ay - p.y, ax - p.x) - spin * T * 0.45f + U.gauss() * err;
        float v = dd / T * 1.07f;
        p.fx = (float) Math.cos(ang); p.fy = (float) Math.sin(ang);
        m.kick(p, (float) Math.cos(ang) * v, (float) Math.sin(ang) * v, 4.9f * T * 1.02f, spin, Match.K_CROSS);
        m.passTarget = aimed;
        if (aimed != null && m.S[p.side].human && !m.autoPlay) Ctl.setControlled(m, m.S[p.side], aimed);
    }

    // ---------------------------------------------------------- shooting
    static void shoot(Match m, Pawn p, float aimY, float aimZ, float power, boolean finesse) {
        Match.Side s = m.S[p.side];
        float gx = m.goalX(s);
        float ay = Match.CY + U.clamp(aimY, -3.35f, 3.35f);
        float dx = gx - p.x, dy = ay - p.y;
        float d = Math.max(1, U.len(dx, dy));
        float v = 17f + 15f * power + p.pl.sho * 0.045f;
        float pr = m.pressureOn(p);
        float sig = (0.040f + (100 - p.pl.sho) * 0.0016f + power * 0.055f + pr * 0.07f + d * 0.0015f) * lvlNoise(m, p);
        if (finesse) sig *= 0.8f;
        float ang = (float) Math.atan2(dy, dx) + U.gauss() * sig;
        float t = d / (v * 0.93f);
        float vz = (aimZ + 4.9f * t * t) / t + U.gauss() * sig * 9f;
        float spin = finesse ? ((dy > 0 ? 1 : -1) * 0.35f) : 0;
        if (finesse) ang -= spin * t * 0.4f;
        p.fx = (float) Math.cos(ang); p.fy = (float) Math.sin(ang);
        m.kick(p, (float) Math.cos(ang) * v, (float) Math.sin(ang) * v, vz, spin, Match.K_SHOT);
        m.shotBy = p; m.passTarget = null;
        p.stat.shots++; s.shots++;
        p.fx = (float) Math.cos(ang); p.fy = (float) Math.sin(ang);
        // on target?
        float tt = (gx - m.bx) / (m.bvx == 0 ? 0.001f : m.bvx);
        if (tt > 0) {
            float yy = m.by + m.bvy * tt, zz = m.bz + m.bvz * tt - 4.9f * tt * tt;
            if (spin != 0) yy = ay;
            if (Math.abs(yy - Match.CY) < Match.GH && zz > 0 && zz < Match.GOAL_H) { p.stat.sot++; s.sot++; }
        }
        m.say(p.pl.last + " бьёт по воротам!");
    }

    // --------------------------------------------------------- off ball
    static void offBall(Match m, Pawn p, Match.Side s, boolean mine) {
        float tx = p.ax, ty = p.ay;
        int g = Player.group(p.role);
        float sprintP = 0.8f;
        boolean sprint = false;
        Pawn car = m.owner;
        if (car != null && car.side == p.side && car != p) {
            // support runs for nearby teammates
            float d = U.dist(p.x, p.y, car.x, car.y);
            int rank = supportRank(m, s, p, car);
            if (rank < 2 && d > 5 && d < 28 && car.role != Player.GK) {
                float lat = (p.ay >= car.y ? 1 : -1) * (rank == 0 ? 9f : 14f);
                float fwd = rank == 0 ? 5f : 11f;
                if (g == 3) fwd += 6;
                float sx = car.x + s.dir * fwd, sy = U.clamp(car.y + lat, 3, Match.W - 3);
                if (m.depthOf(s, sx) > s.offDepth - 0.8f) sx = m.xOfDepth(s, s.offDepth - 0.8f);
                tx = tx * 0.35f + sx * 0.65f; ty = ty * 0.3f + sy * 0.7f;
            } else if (g == 3 && car.role != Player.GK) {
                // striker run behind the line
                float dep = Math.min(s.offDepth - 0.7f, Match.L - 6);
                tx = m.xOfDepth(s, Math.max(m.depthOf(s, p.ax), Math.min(dep, m.depthOf(s, car.x) + 22)));
                ty = p.ay + 5f * (float) Math.sin(m.time * 0.6f + p.slot);
                sprint = m.depthOf(s, car.x) > Match.L * 0.45f && (Math.sin(m.time * 0.9f + p.slot * 2) > 0.55f);
            }
        } else if (!mine || car == null) {
            // loose ball, team around
            ty = p.ay;
        }
        float d = U.dist(p.x, p.y, tx, ty);
        sprint = sprint || d > 14;
        goTo(m, p, tx, ty, d > 6 ? 1f : 0.85f, sprint);
    }

    static int supportRank(Match m, Match.Side s, Pawn p, Pawn car) {
        float dp = U.dist(p.x, p.y, car.x, car.y);
        int rank = 0;
        for (Pawn q : s.pw) {
            if (q == p || q == car || q.role == Player.GK || q.st == Pawn.SENT) continue;
            if (U.dist(q.x, q.y, car.x, car.y) < dp) rank++;
        }
        return rank;
    }

    // ----------------------------------------------------------- defend
    static void defend(Match m, Pawn p, Match.Side s, float dt) {
        Pawn car = m.owner;
        float pressRange = new float[]{16f, 24f, 38f}[s.press];
        boolean engage = U.dist(p.x, p.y, car.x, car.y) < pressRange || p.pressT > 0;
        float ownGx = m.ownGoalX(s);
        if (p == s.press1 && engage && p.role != Player.GK) { pressCarrier(m, p, car, s, dt); return; }
        if (p == s.press2 && engage && s.press >= 1 && Player.group(p.role) >= 1) {
            // cover: stand 6m from carrier on line to own goal
            float dx = ownGx - car.x, dy = Match.CY - car.y, dl = Math.max(1, U.len(dx, dy));
            float tx = car.x + dx / dl * 7f, ty = car.y + dy / dl * 7f;
            goTo(m, p, tx, ty, 0.95f, U.dist(p.x, p.y, tx, ty) > 6);
            return;
        }
        // marking
        float tx = p.ax, ty = p.ay;
        Pawn mark = null; float bd = 9f;
        for (Pawn o : m.S[1 - p.side].pw) {
            if (o == car || o.role == Player.GK || o.st == Pawn.SENT) continue;
            float d = U.dist(o.x, o.y, p.ax, p.ay);
            if (d < bd) { bd = d; mark = o; }
        }
        if (mark != null) {
            float gdx = ownGx - mark.x, gdy = Match.CY - mark.y, gl = Math.max(1, U.len(gdx, gdy));
            float mx = mark.x + gdx / gl * 1.8f, my = mark.y + gdy / gl * 1.8f;
            float w = Player.group(p.role) <= 1 ? 0.65f : 0.5f;
            tx = tx * (1 - w) + mx * w; ty = ty * (1 - w) + my * w;
        }
        float d = U.dist(p.x, p.y, tx, ty);
        goTo(m, p, tx, ty, d > 6 ? 1f : 0.85f, d > 10);
    }

    static void pressCarrier(Match m, Pawn p, Pawn car, Match.Side s, float dt) {
        float d = U.dist(p.x, p.y, car.x, car.y);
        float lead = Math.min(0.7f, d / Math.max(3f, p.maxSpeed()));
        float ownGx = m.ownGoalX(s);
        if (d > 4f) {
            goTo(m, p, car.x + car.vx * lead, car.y + car.vy * lead, 1f, true);
        } else {
            float gdx = ownGx - car.x, gdy = Match.CY - car.y, gl = Math.max(1, U.len(gdx, gdy));
            float tx = car.x + gdx / gl * 1.35f + car.vx * 0.25f, ty = car.y + gdy / gl * 1.35f + car.vy * 0.25f;
            float sp = U.clamp(car.speed() / Math.max(3, p.maxSpeed()) + 0.35f, 0.45f, 1f);
            goTo(m, p, tx, ty, sp, d > 2.4f);
            if (d < 1.55f && p.tackleCd <= 0) {
                float rate = (2.3f + p.pl.def / 50f) * (m.autoPlay || p.side == m.humanSide ? 1f : new float[]{0.6f, 0.9f, 1.15f, 1.4f}[m.diff]);
                if (U.chance(rate * dt)) {
                    p.fx = (car.x - p.x) / Math.max(0.1f, d); p.fy = (car.y - p.y) / Math.max(0.1f, d);
                    m.tackle(p, car, false);
                }
            } else if (d < 2.5f && d > 1.6f && p.tackleCd <= 0 && car.speed() > 4 && Player.group(p.role) <= 2 && U.chance(0.1f * dt * (1 + p.pl.def / 80f))) {
                doSlide(m, p);
            }
        }
    }

    // -------------------------------------------------------------- keeper
    static void keeper(Match m, Pawn g, float dt) {
        Match.Side s = m.S[g.side];
        float ownX = m.ownGoalX(s);
        float in = s.dir; // direction into field
        // holding the ball
        if (m.owner == g) {
            g.thinkT -= dt;
            g.wvx = g.wvy = 0;
            g.fx = in; g.fy = 0;
            if (g.thinkT <= 0 && !(s.human && !m.autoPlay && g.thinkT > -1.5f)) keeperDistribute(m, g, -1);
            return;
        }
        if (g.st != Pawn.N) { g.wvx = g.wvy = 0; return; }
        // incoming shot
        Pawn sh = m.shotBy;
        float lvl = (m.autoPlay || g.side == m.humanSide) ? 1f : new float[]{0.85f, 0.95f, 1.05f, 1.15f}[m.diff];
        if (sh != null && sh.side != g.side) {
            float vxin = -in; // moving towards my goal means vx has sign -in
            if (m.bvx * -in > 1.5f) {
                float t = (ownX - m.bx) / m.bvx;
                if (t > 0 && t < 2.2f) {
                    float yImp = m.by + m.bvy * t;
                    float zImp = Math.max(0.1f, m.bz + m.bvz * t - 4.9f * t * t);
                    if (Math.abs(yImp - Match.CY) < Match.GH + 1.5f && zImp < 3.2f) {
                        float react = 0.10f + (100 - g.pl.gk) / 450f;
                        float tx = ownX + in * 0.7f, ty = U.clamp(yImp, Match.CY - Match.GH - 0.3f, Match.CY + Match.GH + 0.3f);
                        float dy = ty - g.y;
                        float lat = 6.2f * lvl * (0.85f + g.pl.gk / 400f);
                        if (t < 0.62f && Math.abs(dy) > 0.9f && m.time - m.lastKickT > react) {
                            // dive
                            g.st = Pawn.DIVE; g.stT = 0;
                            float need = Math.min(Math.abs(dy), 3.2f * lvl + 0.8f);
                            g.vx = 0; g.vy = (dy > 0 ? 1 : -1) * Math.max(3f, need / Math.max(0.2f, t) * 0.75f);
                            g.vy = U.clamp(g.vy, -9f, 9f);
                            g.fx = in; g.fy = dy > 0 ? 1 : -1;
                            return;
                        }
                        if (m.time - m.lastKickT > react) {
                            goTo(m, g, tx, ty, 1f, true);
                            g.wvy = U.clamp(dy * 6f, -lat, lat);
                            g.wvx = 0;
                        }
                        return;
                    }
                }
            }
        }
        // ball loose in box: claim
        float bdepth = m.depthOf(s, m.bx);
        boolean inReach = bdepth < 18f && Math.abs(m.by - Match.CY) < 21 && m.owner == null;
        if (inReach && m.ballSpeed() < 10 && m.bz < 1.5f) {
            // is any attacker clearly closer?
            float dg = U.dist(g.x, g.y, m.bx, m.by);
            boolean attackerCloser = false;
            for (Pawn o : m.S[1 - g.side].pw) { if (U.dist(o.x, o.y, m.bx, m.by) + 0.8f < dg) attackerCloser = true; }
            if (!attackerCloser || dg < 7) {
                float[] ip = intercept(m, g);
                goTo(m, g, ip[0], ip[1], 1f, true);
                if (dg < 1.5f && m.bz < 1.2f) m.keeperContactPublic(g);
                return;
            }
        }
        // high ball into box
        if (m.bz > 1f && bdepth < 20f && m.owner == null && (m.lastKickKind == Match.K_CROSS || m.lastKickKind == Match.K_LOB) && m.lastTouchSide != g.side) {
            float[] ip = intercept(m, g);
            float landDepth = m.depthOf(s, ip[0]);
            if (landDepth < 14f && Math.abs(ip[1] - Match.CY) < 12f && U.dist(g.x, g.y, ip[0], ip[1]) < 11) {
                goTo(m, g, ip[0], ip[1], 1f, true);
                return;
            }
        }
        // positioning
        float gx = ownX, gyy = Match.CY;
        float dxb = m.bx - gx, dyb = m.by - gyy;
        float db = Math.max(1, U.len(dxb, dyb));
        float off = U.clamp(1.3f + db * 0.05f, 1.3f, 5f);
        Pawn car = m.owner;
        if (car != null && car.side != g.side && m.depthOf(s, car.x) < 22f) off = U.clamp(db * 0.2f, 1.6f, 7f);
        if (m.depthOf(s, m.bx) > 55) off = 6f + (m.depthOf(s, m.bx) - 55) * 0.06f;
        float tx = gx + dxb / db * off, ty = gyy + dyb / db * off * 0.9f;
        ty = U.clamp(ty, Match.CY - Match.GH - 0.8f, Match.CY + Match.GH + 0.8f);
        goTo(m, g, tx, ty, 0.9f, false);
    }

    /** kind: -1 auto, 0 short pass, 1 long kick. */
    static void keeperDistribute(Match m, Pawn g, int kind) {
        Match.Side s = m.S[g.side];
        Pawn best = null; float bs = -999;
        for (Pawn q : s.pw) {
            if (q == g || q.st == Pawn.SENT) continue;
            float d = U.dist(g.x, g.y, q.x, q.y);
            float sc = scorePass(m, g, q, s, 0);
            if (kind == 0 && d > 30) sc -= 20;
            if (kind == 1) sc += d * 0.12f;
            if (d > 55) sc -= 10;
            if (sc > bs) { bs = sc; best = q; }
        }
        if (best == null) { clear(m, g, s); return; }
        float d = U.dist(g.x, g.y, best.x, best.y);
        m.owner = null;
        if (kind == 1 || (kind < 0 && d > 30)) passTo(m, g, best, Match.K_LOB, 0.9f);
        else if (d > 18) passTo(m, g, best, Match.K_LOB, 0.5f);
        else passTo(m, g, best, Match.K_GROUND, 0.5f);
    }

    // ------------------------------------------------------ set pieces (AI)
    static void executeSetPiece(Match m) {
        Pawn tk = m.spTaker; Match.Side s = m.S[m.spSide];
        if (tk == null) { m.finishSetPiece(); return; }
        float gx = m.goalX(s);
        int type = m.spType;
        m.owner = null;
        tk.x = U.clamp(tk.x, -3, Match.L + 3);
        switch (type) {
            case Match.SP_THROW: {
                Pawn t = bestTarget(m, tk, false);
                if (t == null) t = s.pw[1];
                float d = U.dist(tk.x, tk.y, t.x, t.y);
                tk.st = Pawn.THROW; tk.stT = 0;
                throwBall(m, tk, t, d);
                break;
            }
            case Match.SP_GOALKICK: {
                Pawn best = null; float bs = -999;
                for (Pawn q : s.pw) {
                    if (q == tk) continue;
                    float sc = scorePass(m, tk, q, s, 0) + U.dist(tk.x, tk.y, q.x, q.y) * (U.chance(0.6f) ? 0.12f : 0);
                    if (sc > bs) { bs = sc; best = q; }
                }
                float d = U.dist(tk.x, tk.y, best.x, best.y);
                passTo(m, tk, best, d > 24 ? Match.K_LOB : Match.K_GROUND, 0.9f);
                break;
            }
            case Match.SP_CORNER: {
                Pawn tgt = null; float bd = 1e9f;
                float px = gx - s.dir * 7f;
                for (Pawn q : s.pw) {
                    if (q == tk || q.role == Player.GK) continue;
                    float d = U.dist(q.tx, q.ty, px, Match.CY + U.rnd(-3, 3));
                    if (d < bd) { bd = d; tgt = q; }
                }
                if (tgt != null && U.chance(0.88f)) {
                    float ax = tgt.tx, ay = tgt.ty;
                    kickToPoint(m, tk, ax, ay, U.clamp(0.9f + U.dist(tk.x, tk.y, ax, ay) * 0.04f, 1f, 1.8f), 0.35f, tgt);
                } else {
                    Pawn t = bestTarget(m, tk, false);
                    passTo(m, tk, t, Match.K_GROUND, 0.5f);
                }
                break;
            }
            case Match.SP_FREEKICK: {
                float gd = U.dist(m.spX, m.spY, gx, Match.CY);
                if (gd < 31 && tk.pl.sho > 55 && !(m.spFoulSeverity == 1)) {
                    float side = U.chance(0.5f) ? 1f : -1f;
                    float aimY = side * U.rnd(2.2f, 3.2f);
                    float aimZ = gd < 20 ? U.rnd(1.6f, 2.2f) : U.rnd(1.0f, 2.1f);
                    shoot(m, tk, aimY, aimZ, 0.75f + U.rnd(0, 0.15f), true);
                } else if (gd < 48 && U.chance(0.55f)) {
                    Pawn t = boxTarget(m, tk, s);
                    if (t == null) t = bestTarget(m, tk, false);
                    passTo(m, tk, t, Match.K_CROSS, 0.7f);
                } else {
                    Pawn t = bestTarget(m, tk, false);
                    float d = U.dist(tk.x, tk.y, t.x, t.y);
                    passTo(m, tk, t, d > 26 ? Match.K_LOB : Match.K_GROUND, 0.6f);
                }
                break;
            }
            case Match.SP_PENALTY: {
                float side = U.chance(0.5f) ? 1f : -1f;
                shoot(m, tk, side * U.rnd(2.3f, 3.1f), U.chance(0.55f) ? U.rnd(0.25f, 0.7f) : U.rnd(1.7f, 2.2f), 0.7f, false);
                penaltyKeeperDive(m, tk, -1);
                break;
            }
            default: break;
        }
        m.finishSetPiece();
    }

    static void throwBall(Match m, Pawn tk, Pawn t, float d) {
        float T = U.clamp(0.5f + d * 0.05f, 0.7f, 1.5f);
        float ax = t.x + t.vx * T * 0.6f, ay = t.y + t.vy * T * 0.6f;
        float dd = Math.max(1, U.dist(tk.x, tk.y, ax, ay));
        float ang = (float) Math.atan2(ay - tk.y, ax - tk.x) + U.gauss() * 0.04f;
        tk.fx = (float) Math.cos(ang); tk.fy = (float) Math.sin(ang);
        m.kick(tk, (float) Math.cos(ang) * dd / T, (float) Math.sin(ang) * dd / T, 2.5f * T + 1f, 0, Match.K_GROUND);
        m.bz = 1.9f; m.bvz = 1.5f + T * 0.5f;
        m.passTarget = t;
        if (m.S[tk.side].human && !m.autoPlay) Ctl.setControlled(m, m.S[tk.side], t);
        tk.st = Pawn.THROW; tk.stT = 0;
    }

    static void penaltyKeeperDive(Match m, Pawn shooter, int forcedDir) {
        Pawn gk = m.S[1 - shooter.side].pw[0];
        int dir;
        if (forcedDir != -1) dir = forcedDir;
        else {
            float r = U.rnd();
            float lv = (m.autoPlay || gk.side == m.humanSide) ? 1f : new float[]{0.85f, 1f, 1.1f, 1.2f}[m.diff];
            // slight chance to guess the right side using actual shot direction
            boolean guess = U.chance(0.34f * lv * (gk.pl.gk / 80f));
            int truth = m.bvy > 0 ? 1 : -1;
            if (Math.abs(m.bvy) < 1) truth = 0;
            if (guess) dir = truth; else dir = r < 0.4f ? -1 : (r < 0.8f ? 1 : 0);
            if (truth != 0 && !guess && dir == truth && U.chance(0.2f)) dir = 0;
        }
        if (dir == 0) return;
        gk.st = Pawn.DIVE; gk.stT = 0; gk.vx = 0; gk.vy = dir * 6.5f; gk.fx = -m.S[gk.side].dir * -1; gk.fy = dir;
        gk.fx = m.S[gk.side].dir; gk.fy = dir;
    }
}
