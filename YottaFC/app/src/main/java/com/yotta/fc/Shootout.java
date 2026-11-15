package com.yotta.fc;

/** Penalty shootout logic. */
final class Shootout {
    static void begin(Match m) {
        m.penRound = 0; m.penSide = 0; m.penPhase = 0; m.penT = 0;
        setup(m);
        m.banner("СЕРИЯ ПЕНАЛЬТИ", 2.5f, 0);
    }

    static void setup(Match m) {
        int ks = m.penSide;
        Match.Side k = m.S[ks], d = m.S[1 - ks];
        k.dir = 1; d.dir = -1;
        // pick shooter: rotate through outfield players ordered by shooting
        java.util.ArrayList<Pawn> order = new java.util.ArrayList<Pawn>();
        for (Pawn p : k.pw) if (p.role != Player.GK && p.st != Pawn.SENT) order.add(p);
        java.util.Collections.sort(order, new java.util.Comparator<Pawn>() {
            public int compare(Pawn a, Pawn b) { return b.pl.sho - a.pl.sho; }
        });
        int idx = k.penTaken % Math.max(1, order.size());
        m.penShooter = order.get(idx);
        m.penKeeper = d.pw[0];
        m.penDecided = false; m.penShotFired = false; m.penResultGoal = false; m.penPhase = 0; m.penT = 0;
        // place everyone
        for (Pawn p : m.all) {
            p.vx = p.vy = 0; p.wvx = p.wvy = 0; p.st = Pawn.N; p.human = false; p.z = 0;
            if (p.st == Pawn.SENT) continue;
            int off = p.slot;
            float ang = (p.side == 0 ? 1 : -1) * (0.3f + off * 0.28f);
            p.tx = Match.L - 30 + (p.side == 0 ? -2 : 2) * 0 + (off % 3) * 1.5f;
            p.ty = Match.CY - 14 + off * 2.8f;
            p.x = p.tx; p.y = p.ty;
            p.fx = 1; p.fy = 0;
        }
        Pawn sh = m.penShooter, gk = m.penKeeper;
        sh.x = Match.L - 11 - 3.2f; sh.y = Match.CY; sh.tx = sh.x; sh.ty = sh.y;
        gk.x = Match.L - 0.6f; gk.y = Match.CY; gk.tx = gk.x; gk.ty = gk.y; gk.fx = -1; gk.fy = 0;
        m.S[1 - ks].pw[0].fx = -1;
        // the shooter's own keeper off the field area
        Pawn ogk = k.pw[0]; ogk.x = Match.L - 30; ogk.y = Match.CY + 20; ogk.tx = ogk.x; ogk.ty = ogk.y;
        m.owner = null; m.bx = Match.L - 11; m.by = Match.CY; m.bz = 0; m.bvx = m.bvy = m.bvz = 0; m.shotBy = null;
        m.penAimY = 0; m.penAimZ = 0.8f;
        if (k.human && !m.autoPlay) Ctl.setControlled(m, k, sh);
        m.spAimY = Match.CY;
    }

    static void update(Match m, float dt) {
        m.worldStepIdle2(dt);
        Pawn sh = m.penShooter, gk = m.penKeeper;
        Match.Side k = m.S[m.penSide];
        m.penT += dt;
        boolean humanShoots = k.human && !m.autoPlay;
        boolean humanKeeps = m.S[1 - m.penSide].human && !m.autoPlay;
        switch (m.penPhase) {
            case 0: { // waiting / aiming
                // shooter approaches slowly after delay
                if (m.penT > 1.2f) AI.goTo(m, sh, Match.L - 11 - 0.9f, Match.CY, 0.6f, false);
                if (humanShoots) {
                    Input in = m.in;
                    in.shootHeld = in.shoot ? in.shootHeld + dt : 0;
                    m.penAimY = U.clamp(m.penAimY + in.sy * dt * 4f, -3.4f, 3.4f);
                    m.spAimY = Match.CY + m.penAimY;
                    if (m.penT > 1.0f && (in.shootR || in.passP)) {
                        float pw = in.shootR ? U.clamp(in.shootHeld / 0.7f, 0.3f, 1f) : 0.6f;
                        fire(m, sh, m.penAimY, pw > 0.8f ? 1.9f : (pw > 0.5f ? 1f : 0.4f), Math.max(0.6f, pw), -9);
                    }
                } else if (m.penT > 2.6f) {
                    float side = U.chance(0.5f) ? 1 : -1;
                    fire(m, sh, side * U.rnd(2.2f, 3.1f), U.chance(0.55f) ? U.rnd(0.3f, 0.8f) : U.rnd(1.6f, 2.2f), U.rnd(0.65f, 0.9f), -9);
                }
                if (humanKeeps) {
                    // keeper moves along line with stick
                    gk.wvy = m.in.sy * 4f; gk.wvx = 0; gk.human = true;
                    gk.y = U.clamp(gk.y + m.in.sy * 3f * dt, Match.CY - 2.5f, Match.CY + 2.5f); gk.wvy = 0;
                    gk.human = false;
                }
                break;
            }
            case 1: { // ball in flight
                // goal / save detection
                if (m.bx > Match.L && Math.abs(m.by - Match.CY) < Match.GH && m.bz < Match.GOAL_H) { result(m, true); }
                else if (m.lastTouch != null && m.lastTouch.role == Player.GK && m.lastTouch == gk && m.penT > 0.2f) { result(m, false); m.banner("СЕЙВ!", 1.5f, 0); }
                else if (m.bx > Match.L + 0.3f || m.by < 0 || m.by > Match.W || m.penT > 3.2f || (m.ballSpeed() < 0.4f && m.penT > 1.0f && m.bz < 0.1f)) {
                    result(m, false);
                }
                break;
            }
            case 2: { // showing result
                if (m.penT > 2.4f) next(m);
                break;
            }
            default: break;
        }
    }

    static void fire(Match m, Pawn sh, float aimY, float aimZ, float pw, int keeperDir) {
        m.owner = null;
        m.bx = Match.L - 11; m.by = Match.CY;
        sh.x = Match.L - 11 - 0.8f; sh.y = Match.CY; sh.fx = 1; sh.fy = 0;
        AI.shoot(m, sh, aimY, aimZ, pw, false);
        int dir = keeperDir;
        if (m.S[1 - m.penSide].human && !m.autoPlay) {
            float sy = m.in.sy;
            dir = Math.abs(sy) < 0.3f ? 0 : (sy > 0 ? 1 : -1);
            AI.penaltyKeeperDive(m, sh, dir);
        } else AI.penaltyKeeperDive(m, sh, -1);
        m.penPhase = 1; m.penT = 0;
    }

    static void result(Match m, boolean goal) {
        Match.Side k = m.S[m.penSide];
        k.penRes[Math.min(11, k.penTaken)] = goal;
        k.penTaken++;
        if (goal) { k.penScore++; m.banner("ГОЛ!", 2f, 1); m.sounds.add(Match.S_GOAL); m.sounds.add(Match.S_NET); m.penShooter.st = Pawn.CELEB; }
        else if (m.bannerT <= 0.1f) m.banner("МИМО!", 2f, 0);
        m.penResultGoal = goal;
        m.penPhase = 2; m.penT = 0;
    }

    static void next(Match m) {
        Match.Side a = m.S[0], b = m.S[1];
        int ta = a.penTaken, tb = b.penTaken;
        boolean done = false;
        if (ta >= 5 && tb >= 5 && ta == tb) done = a.penScore != b.penScore;
        else if (ta <= 5 && tb <= 5) {
            int remA = 5 - ta, remB = 5 - tb;
            if (a.penScore > b.penScore + remB || b.penScore > a.penScore + remA) done = true;
        }
        if (done) {
            m.mode = Match.M_FULL; m.modeT = 0;
            m.winner = a.penScore > b.penScore ? 0 : 1;
            m.finished = false; m.period = 5;
            m.banner("КОНЕЦ СЕРИИ " + a.penScore + ":" + b.penScore, 3f, 0);
            for (Match.Side s : m.S) s.dir = s.dir;
            return;
        }
        m.penSide = 1 - m.penSide;
        setup(m);
    }
}
