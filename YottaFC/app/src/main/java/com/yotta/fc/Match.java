package com.yotta.fc;

import java.util.ArrayList;
import java.util.HashMap;

/** Pure-Java football simulation: ball physics, rules, referees, set pieces. */
final class Match {
    static final float L = 105f, W = 68f, CY = 34f, GH = 3.66f, GOAL_H = 2.44f, GRAV = 9.8f, BR = 0.11f;
    static final int M_KICKOFF = 0, M_PLAY = 1, M_SET = 2, M_GOAL = 3, M_HALF = 4, M_FULL = 5, M_ETBREAK = 6, M_PEN = 7, M_END = 8;
    static final int SP_NONE = 0, SP_KICKOFF = 1, SP_THROW = 2, SP_GOALKICK = 3, SP_CORNER = 4, SP_FREEKICK = 5, SP_PENALTY = 6;
    static final int K_GROUND = 0, K_THROUGH = 1, K_LOB = 2, K_CROSS = 3, K_SHOT = 4, K_CLEAR = 5;
    // sounds
    static final int S_WHISTLE = 0, S_KICK = 1, S_GOAL = 2, S_POST = 3, S_SAVE = 4, S_CARD = 5, S_BOUNCE = 6, S_FOUL = 7, S_END = 8, S_NET = 9, S_TACKLE = 10, S_OOH = 11;
    // events
    static final int E_GOAL = 0, E_YEL = 1, E_RED = 2, E_SUB = 3, E_INJ = 4, E_PEN = 5, E_OWN = 6;

    static final class Evt {
        int min, type, side; String text; Player pl;
        Evt(int min, int type, int side, Player pl, String text) { this.min = min; this.type = type; this.side = side; this.pl = pl; this.text = text; }
    }

    static final class Side {
        Team team;
        String name, abbr;
        int c1, c2, c3, pattern, gk1, gk2;
        Pawn[] pw = new Pawn[11];
        ArrayList<Player> bench = new ArrayList<Player>();
        HashMap<Player, Stat> stats = new HashMap<Player, Stat>();
        int formation, style = 1, press = 1, line = 1, width = 1, tempo = 1;
        int dir;
        boolean human;
        int score, shots, sot, passes, passOK, fouls, corners, offsides, yellows, reds, tackles, subs, saves, woodwork;
        float possTime;
        Pawn ctl; float ctlLock;
        Pawn press1, press2;
        float offDepth;           // offside line depth for attackers of this side
        int penScore, penTaken;
        boolean[] penRes = new boolean[12];
        Player captain;
    }

    // ---- settings / state
    final Side[] S = new Side[2];
    Pawn[] all = new Pawn[22];
    Official ref, arNear, arFar;
    Official[] coaches = new Official[2];
    ArrayList<Official> officials = new ArrayList<Official>();
    ArrayList<Evt> events = new ArrayList<Evt>();
    ArrayList<Integer> sounds = new ArrayList<Integer>();
    String banner = ""; float bannerT; int bannerKind; // 0 info,1 goal,2 yellow,3 red,4 sub
    String commentary = ""; float commT;
    int diff = 1;
    boolean cup;                // knockout (needs winner)
    boolean friendlyHuman2;
    float clockSpeed = 15f;     // game seconds per real second
    int mode = M_KICKOFF; float modeT;
    float clock;                // game seconds (0..)
    int period;                 // 0 first half,1 second,2 ET1,3 ET2
    float periodStart, periodEnd, stoppage;
    float time;
    float weatherRain;          // 0..1
    int kickoffSide = 0;
    boolean halfPending;        // waiting for UI to continue
    boolean finished;
    int winner = -1;            // after penalties/final
    boolean autoPlay;           // both sides AI (headless)
    boolean replayActive;

    // ball
    float bx = L / 2, by = CY, bz, bvx, bvy, bvz, bspin;
    float brx, bry;             // rolling angles (for rendering)
    Pawn owner, lastTouch, lastKicker, passTarget, shotBy, lastPasser;
    int lastTouchSide;
    float lastKickT, lastPassT;
    int lastKickKind;
    float ballTrail;
    boolean noOffside;          // next touch is exempt (restarts)
    float ownerT;               // time owner has had the ball
    Pawn prevOwner;
    float touchLockOpp;

    // set piece
    int spType, spSide; Pawn spTaker; float spX, spY, spTimer, spAimX, spAimY, spPower; boolean spHuman, spReady;
    float spDelay;
    int spFoulSeverity;
    Pawn spVictim;

    // shootout
    boolean shootout; int penSide, penRound, penPhase; float penT; boolean penDecided; Pawn penShooter, penKeeper;
    float penAimY = 0, penAimZ = 1f; boolean penResultGoal; int penDiveDir; boolean penShotFired;
    float penWait;

    // human input
    final Input in = new Input();

    // replay recording
    static final int REP_FRAMES = 30 * 9;
    float[][] repBuf = new float[REP_FRAMES][];
    int repHead, repCount; float repAcc;
    float goalTime = -100;

    int humanSide = 0;  // main controlled side (for camera / HUD)
    float shake;

    Match(Team home, Team away, boolean homeHuman, boolean awayHuman, int diff, float realMinutes, boolean cup, boolean night) {
        this.diff = diff;
        this.cup = cup;
        clockSpeed = 90f / realMinutes;
        S[0] = makeSide(home, homeHuman, 0);
        S[1] = makeSide(away, awayHuman, 1);
        humanSide = homeHuman ? 0 : (awayHuman ? 1 : 0);
        // kits
        pickKits();
        for (int s = 0; s < 2; s++) buildPawns(s);
        ref = new Official(Official.REF); officials.add(ref);
        arNear = new Official(Official.AR_NEAR); arFar = new Official(Official.AR_FAR);
        officials.add(arNear); officials.add(arFar);
        for (int i = 0; i < 2; i++) { coaches[i] = new Official(Official.COACH); coaches[i].side = i; officials.add(coaches[i]); }
        arNear.y = W + 1.8f; arFar.y = -1.8f; arNear.x = arFar.x = L / 2;
        ref.x = L / 2 - 6; ref.y = CY + 8;
        coaches[0].x = 36; coaches[0].y = -3.2f; coaches[1].x = 70; coaches[1].y = -3.2f;
        periodStart = 0; periodEnd = 45 * 60; stoppage = U.ri(60, 180);
        startKickoff(kickoffSide, true);
    }

    Side makeSide(Team t, boolean human, int idx) {
        Side s = new Side();
        s.team = t; s.name = t.name; s.abbr = t.shortName; s.human = human;
        s.formation = t.formation; s.style = t.style; s.press = t.press; s.line = t.line; s.width = t.width; s.tempo = t.tempo;
        s.dir = idx == 0 ? 1 : -1;
        return s;
    }

    void pickKits() {
        Side a = S[0], b = S[1];
        Team ta = a.team, tb = b.team;
        a.c1 = ta.c1; a.c2 = ta.c2; a.c3 = ta.c3; a.pattern = ta.pattern;
        int[] cand = {tb.c1, tb.c2, tb.c3, 0xFFFFFFFF, 0xFF111111};
        int best = 0; float bd = -1;
        for (int i = 0; i < cand.length; i++) {
            float d = U.colorDist(cand[i], a.c1);
            if (i == 0) d += 40; // prefer home colour
            if (d > bd && (d > 95 || i == cand.length - 1)) { bd = d; best = i; if (d > 95 && i < 3) break; }
        }
        if (U.colorDist(tb.c1, a.c1) < 95) {
            b.c1 = cand[1]; b.c2 = tb.c1; b.c3 = tb.c3;
            if (U.colorDist(b.c1, a.c1) < 95) { b.c1 = U.colorDist(a.c1, 0xFFFFFFFF) > 120 ? 0xFFFFFFFF : 0xFF111111; b.c2 = tb.c1; }
            b.pattern = (tb.pattern + 1) % 3;
        } else { b.c1 = tb.c1; b.c2 = tb.c2; b.c3 = tb.c3; b.pattern = tb.pattern; }
        int[] gks = {0xFF22C55E, 0xFFF59E0B, 0xFFEC4899, 0xFF38BDF8, 0xFFA3E635, 0xFF9333EA};
        for (int s = 0; s < 2; s++) {
            Side sd = S[s];
            int bestc = gks[0]; float bdist = -1;
            for (int c : gks) {
                float d = Math.min(U.colorDist(c, S[0].c1), U.colorDist(c, S[1].c1));
                if (d > bdist) { bdist = d; bestc = c; }
            }
            sd.gk1 = bestc; sd.gk2 = U.shade(bestc, 0.55f);
            if (s == 1 && sd.gk1 == S[0].gk1) { sd.gk1 = gks[(java.util.Arrays.binarySearch(gks, sd.gk1) + 3) & 3]; sd.gk2 = U.shade(sd.gk1, 0.55f); }
        }
    }

    void buildPawns(int s) {
        Side sd = S[s]; Team t = sd.team;
        int[] roles = Data.FORM_ROLE[sd.formation];
        for (int i = 0; i < 11; i++) {
            Player p = t.byId(t.lineup[i]);
            if (p == null) { p = t.squad.get(i); }
            Stat st = statFor(sd, p);
            st.played = true;
            Pawn pw = new Pawn(p, st, s, i, roles[i]);
            pw.human = false;
            sd.pw[i] = pw;
            all[s * 11 + i] = pw;
        }
        for (int i = 0; i < 7; i++) {
            Player p = t.byId(t.bench[i]);
            if (p != null) sd.bench.add(p);
        }
        sd.captain = t.byId(t.captain);
    }

    Stat statFor(Side sd, Player p) {
        Stat st = sd.stats.get(p);
        if (st == null) { st = new Stat(p); sd.stats.put(p, st); }
        return st;
    }

    // ================= geometry helpers
    float depthOf(Side s, float x) { return s.dir > 0 ? x : L - x; }
    float xOfDepth(Side s, float d) { return s.dir > 0 ? d : L - d; }
    float yOfV(Side s, float v) { return s.dir > 0 ? v * W : (1 - v) * W; }
    float goalX(Side attacker) { return attacker.dir > 0 ? L : 0; }
    float ownGoalX(Side s) { return s.dir > 0 ? 0 : L; }
    Side opp(Side s) { return S[1 - sideIdx(s)]; }
    int sideIdx(Side s) { return s == S[0] ? 0 : 1; }
    float ballSpeed() { return U.len(bvx, bvy); }
    boolean inBox(int defSide, float x, float y) {
        Side d = S[defSide];
        float dep = depthOf(d, x);
        return dep < 16.5f && Math.abs(y - CY) < 20.16f;
    }

    // ================= minute
    int minute() {
        int base = period >= 2 ? (period == 2 ? 90 : 105) : (period == 1 ? 45 : 0);
        float inPer = clock - periodStart;
        int m = base + (int) (inPer / 60f);
        return m + 1;
    }
    String clockText() {
        float inPer = Math.max(0, clock - periodStart);
        int base = period == 0 ? 0 : period == 1 ? 45 : period == 2 ? 90 : 105;
        int lenMin = period < 2 ? 45 : 15;
        int mm = base + (int) (inPer / 60);
        if (inPer / 60f >= lenMin) {
            int extra = (int) ((inPer - lenMin * 60) / 60) + 1;
            return (base + lenMin) + "+" + extra + "'";
        }
        int ss = (int) (inPer % 60);
        return (mm < 10 ? "0" : "") + mm + ":" + (ss < 10 ? "0" : "") + ss;
    }

    // ================= main update
    void update(float dt) {
        time += dt;
        modeT += dt;
        if (bannerT > 0) bannerT -= dt;
        if (commT > 0) commT -= dt;
        if (shake > 0) shake -= dt * 2;
        recordReplay(dt);
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            p.lock = Math.max(0, p.lock - dt);
            p.tackleCd = Math.max(0, p.tackleCd - dt);
            p.kickT = Math.max(0, p.kickT - dt);
            p.pressT = Math.max(0, p.pressT - dt);
            p.offT = Math.max(0, p.offT - dt);
            if (p.offT <= 0) p.offPos = false;
        }
        switch (mode) {
            case M_KICKOFF: updateKickoff(dt); break;
            case M_PLAY: updatePlay(dt); break;
            case M_SET: updateSet(dt); break;
            case M_GOAL: updateGoalCelebration(dt); break;
            case M_HALF: case M_ETBREAK: updateBreak(dt); break;
            case M_FULL: updateFull(dt); break;
            case M_PEN: Shootout.update(this, dt); break;
            default: break;
        }
        updateOfficials(dt);
        in.clearEdges();
    }

    void worldStep(float dt, boolean ballLive) {
        updateSideInfo(dt);
        Ctl.update(this, dt);
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            if (p.human) continue; // controlled by Ctl
            AI.think(this, p, dt);
        }
        for (int i = 0; i < 22; i++) integrate(all[i], dt);
        separate();
        if (ballLive) {
            ballStep(dt);
            contacts(dt);
        } else if (owner != null) carry(dt);
        stamina(dt);
    }

    void updateKickoff(float dt) {
        // players stand; taker waits then plays
        for (int i = 0; i < 22; i++) { all[i].wvx = 0; all[i].wvy = 0; }
        worldStepIdle(dt);
        Side ks = S[kickoffSide];
        if (modeT > 0.4f && !spReady) { spReady = true; sounds.add(S_WHISTLE); }
        boolean go = false;
        if (ks.human && !autoPlay) { if (modeT > 1.0f && (in.anyPress() || modeT > 9f)) go = true; }
        else if (modeT > 2.0f) go = true;
        if (go) {
            Pawn k = spTaker;
            Pawn mate = null; float bd = 1e9f;
            for (Pawn p : ks.pw) {
                if (p == k || p.role == Player.GK) continue;
                float d = U.dist(p.x, p.y, k.x, k.y) + (depthOf(ks, p.x) > depthOf(ks, k.x) ? 8 : 0);
                if (d < bd) { bd = d; mate = p; }
            }
            noOffside = true;
            setOwnerSilent(k);
            AI.passTo(this, k, mate, K_GROUND, 0.4f);
            mode = M_PLAY; modeT = 0; spType = SP_NONE; spReady = false;
        }
    }

    void keeperContactPublic(Pawn g) { keeperContact(g); }

    /** Shootout world step: no AI thinking, ball physics live. */
    void worldStepIdle2(float dt) {
        updateSideInfo(dt);
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            if (p.st == Pawn.N && p != penShooter && p != penKeeper) AI.goTo(this, p, p.tx, p.ty, 0.5f, false);
            else if (p.st == Pawn.N && p == penShooter && penPhase != 0) { p.wvx = p.wvy = 0; }
        }
        for (int i = 0; i < 22; i++) integrate(all[i], dt);
        separate();
        ballStep(dt);
        if (owner == null) { keeperContact(penKeeper); if (owner == null) contacts(dt); }
    }

    void worldStepIdle(float dt) {
        updateSideInfo(dt);
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            // keep to targets (kickoff)
            if (p.human) p.human = false;
            AI.goTo(this, p, p.tx, p.ty, 0.9f, false);
        }
        for (int i = 0; i < 22; i++) integrate(all[i], dt);
        separate();
        if (owner != null) carry(dt);
        stamina(dt);
    }

    void updatePlay(float dt) {
        clock += dt * clockSpeed;
        worldStep(dt, true);
        ruleChecks(dt);
        checkPeriodEnd();
        S[lastTouchSide].possTime += dt; // approximate possession by last touch
        ballTrail = bvx * bvx + bvy * bvy > 400 ? Math.min(1, ballTrail + dt * 4) : Math.max(0, ballTrail - dt * 3);
    }

    // ================= side info
    float infoT;
    void updateSideInfo(float dt) {
        for (int s = 0; s < 2; s++) {
            Side sd = S[s], od = S[1 - s];
            // offside depth (attacker's depth of second-last defender)
            float a = -1, b = -1;
            for (Pawn p : od.pw) {
                if (p.st == Pawn.SENT) continue;
                float d = depthOf(sd, p.x);
                if (d > a) { b = a; a = d; } else if (d > b) b = d;
            }
            sd.offDepth = Math.max(L / 2, Math.max(b, 0));
        }
        infoT -= dt;
        if (infoT <= 0) { infoT = 0.2f; pickPressers(); }
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            anchorFor(p);
        }
    }

    void pickPressers() {
        for (int s = 0; s < 2; s++) {
            Side sd = S[s];
            sd.press1 = sd.press2 = null;
            float b1 = 1e9f, b2 = 1e9f;
            float tx = bx + bvx * 0.4f, ty = by + bvy * 0.4f;
            if (owner != null) { tx = owner.x; ty = owner.y; }
            for (Pawn p : sd.pw) {
                if (p.role == Player.GK || p.st == Pawn.SENT || p.st == Pawn.DOWN || p.st == Pawn.FALL) continue;
                if (p == passTarget) continue;
                float d = U.dist(p.x, p.y, tx, ty) / (p.maxSpeed() * (p.stamina * 0.2f + 0.8f)) + (p.pressT > 0 ? -2 : 0);
                if (d < b1) { b2 = b1; sd.press2 = sd.press1; b1 = d; sd.press1 = p; }
                else if (d < b2) { b2 = d; sd.press2 = p; }
            }
        }
    }

    void anchorFor(Pawn p) {
        Side s = S[p.side];
        int f = s.formation;
        float u = Data.FORM_U[f][p.slot], v = Data.FORM_V[f][p.slot];
        // Roles may differ after substitutions: keep slot geometry
        boolean inPoss = owner != null ? owner.side == p.side : lastTouchSide == p.side;
        float bu = clampd(depthOf(s, bx) / L);
        if (p.role == Player.GK) {
            p.ax = xOfDepth(s, 3f); p.ay = CY; return;
        }
        float depth = u * L * 0.78f + bu * L * 0.34f;
        int g = Player.group(p.role);
        float push = 0;
        if (inPoss) push += (g == 3 ? 6 : g == 2 ? 3.5f : 1.5f) + (s.style - 1) * 3f + (s.tempo - 1) * 1.5f;
        else push -= (g == 3 ? 9 : g == 2 ? 4 : 1) + (1 - s.style) * 3f;
        push += (s.line - 1) * 4f;
        depth += push;
        // team may be pinned deep when scoring/leading handled via style
        depth = U.clamp(depth, 7f, L - 4f);
        if (inPoss && g >= 2) depth = Math.min(depth, s.offDepth - (g == 3 ? 0.6f : 2f));
        float wMul = inPoss ? 1.0f + 0.12f * (s.width - 1) + 0.1f : 0.84f + 0.08f * (s.width - 1);
        float vv = 0.5f + (v - 0.5f) * wMul;
        float shiftY = (by - CY) * (inPoss ? 0.2f : 0.28f);
        p.ax = xOfDepth(s, depth);
        p.ay = U.clamp(yOfV(s, vv) + shiftY, 2.5f, W - 2.5f);
    }
    static float clampd(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    // ================= integration
    void integrate(Pawn p, float dt) {
        if (p.st == Pawn.SENT) { p.vx = p.vy = 0; return; }
        p.stT += dt;
        float ms = p.maxSpeed();
        switch (p.st) {
            case Pawn.SLIDE: {
                float k = Math.max(0, 1 - p.stT / 0.75f);
                p.vx = p.fx * 7.6f * k * k + p.vx * 0.0f;
                p.vy = p.fy * 7.6f * k * k;
                p.x += p.vx * dt; p.y += p.vy * dt;
                if (p.stT > 0.45f && p.stT < 0.5f) {}
                if (p.stT > 0.12f && p.stT < 0.65f && p.planKind != 99) {
                    if (owner != null && owner.side != p.side && U.dist(p.x, p.y, owner.x, owner.y) < 1.35f) {
                        p.planKind = 99; tackle(p, owner, true);
                    }
                }
                if (p.stT > 0.95f) { p.st = Pawn.GETUP; p.stT = 0; p.planKind = 0; }
                limitToArea(p);
                return;
            }
            case Pawn.FALL: case Pawn.DOWN: {
                p.vx *= (1 - Math.min(1, dt * 6)); p.vy *= (1 - Math.min(1, dt * 6));
                p.x += p.vx * dt; p.y += p.vy * dt;
                if (p.st == Pawn.FALL && p.stT > 1.3f) { p.st = Pawn.GETUP; p.stT = 0; }
                return;
            }
            case Pawn.GETUP: case Pawn.STUMBLE: {
                p.vx *= (1 - Math.min(1, dt * 5)); p.vy *= (1 - Math.min(1, dt * 5));
                p.x += p.vx * dt; p.y += p.vy * dt;
                if (p.stT > (p.st == Pawn.GETUP ? 0.5f : 0.4f)) { p.st = Pawn.N; p.stT = 0; }
                return;
            }
            case Pawn.DIVE: {
                p.x += p.vx * dt; p.y += p.vy * dt;
                p.vx *= (1 - Math.min(1, dt * 2.2f)); p.vy *= (1 - Math.min(1, dt * 2.2f));
                if (p.stT > 1.0f) { p.st = Pawn.GETUP; p.stT = 0; }
                limitToArea(p);
                return;
            }
            case Pawn.JUMP: {
                p.z = 0.5f * (float) Math.sin(Math.min(1, p.stT / 0.55f) * U.PI);
                p.x += p.vx * dt * 0.8f; p.y += p.vy * dt * 0.8f;
                if (p.stT > 0.55f) { p.st = Pawn.N; p.z = 0; p.stT = 0; }
                return;
            }
            case Pawn.THROW: {
                p.vx *= (1 - Math.min(1, dt * 8)); p.vy *= (1 - Math.min(1, dt * 8));
                p.x += p.vx * dt; p.y += p.vy * dt;
                return;
            }
            default: break;
        }
        // normal movement
        float stamF = 0.82f + 0.18f * p.stamina;
        float top = ms * stamF * (p.wantSprint ? 1f : 0.84f);
        if (owner == p) top *= 0.94f + p.pl.dri * 0.0006f;
        if (p.side != humanSide && !autoPlay) top *= new float[]{0.95f, 0.99f, 1.01f, 1.03f}[diff];
        float wl = U.len(p.wvx, p.wvy);
        float wx = p.wvx, wy = p.wvy;
        if (wl > top) { wx *= top / wl; wy *= top / wl; wl = top; }
        float dvx = wx - p.vx, dvy = wy - p.vy;
        float dl = U.len(dvx, dvy);
        float acc = 13f + p.pl.pac * 0.07f;
        if (wl < U.len(p.vx, p.vy)) acc *= 1.9f;
        float maxd = acc * dt;
        if (dl > maxd) { dvx *= maxd / dl; dvy *= maxd / dl; }
        p.vx += dvx; p.vy += dvy;
        p.x += p.vx * dt; p.y += p.vy * dt;
        float sp = U.len(p.vx, p.vy);
        // facing
        float tfx = p.fx, tfy = p.fy;
        if (sp > 0.6f) { tfx = p.vx / sp; tfy = p.vy / sp; }
        float aw = U.len(p.wvx, p.wvy);
        if (p.human && aw > 0.2f) { tfx = p.wvx / aw; tfy = p.wvy / aw; }
        if (owner == p && sp < 1.5f && aw < 0.2f) { /* keep */ }
        float ang = (float) Math.atan2(p.fy, p.fx), tang = (float) Math.atan2(tfy, tfx);
        float da = U.angDiff(ang, tang);
        float turn = (16f - sp * 0.5f) * dt;
        if (p.st == Pawn.CELEB) turn = 12 * dt;
        ang += U.clamp(da, -turn, turn);
        p.fx = (float) Math.cos(ang); p.fy = (float) Math.sin(ang);
        p.phase += sp * dt * 1.55f + (sp > 0.3f ? 0 : 0);
        if (sp < 0.3f) p.phase += dt * 0.0f;
        if (p.st == Pawn.CELEB) p.z = Math.abs((float) Math.sin(p.stT * 7f)) * 0.25f;
        else p.z = 0;
        limitToArea(p);
    }

    void limitToArea(Pawn p) {
        float mx = (mode == M_SET && spType == SP_THROW) || true ? 3.5f : 2f;
        p.x = U.clamp(p.x, -mx, L + mx);
        p.y = U.clamp(p.y, -mx + 0.5f, W + mx - 0.5f);
    }

    void stamina(float dt) {
        for (Pawn p : all) {
            float sp = p.speed();
            float ms = Math.max(1f, p.maxSpeed());
            float rate;
            if (sp > ms * 0.8f) rate = -0.020f * (1.35f - p.pl.sta * 0.007f);
            else if (sp > ms * 0.5f) rate = -0.003f;
            else rate = 0.012f;
            p.stamina = U.clamp(p.stamina + rate * dt * clockSpeed * 0.1f, 0.25f, 1f);
            if (p.pl.sta > 0 && p.stamina < 0.3f) p.stamina = 0.3f;
        }
    }

    void separate() {
        for (int i = 0; i < 22; i++) {
            Pawn a = all[i];
            if (a.st == Pawn.SENT) continue;
            for (int j = i + 1; j < 22; j++) {
                Pawn b = all[j];
                if (b.st == Pawn.SENT) continue;
                float dx = b.x - a.x, dy = b.y - a.y;
                float d2 = dx * dx + dy * dy;
                if (d2 < 0.55f * 0.55f && d2 > 1e-6f) {
                    float d = (float) Math.sqrt(d2);
                    float push = (0.55f - d) * 0.5f;
                    float nx = dx / d, ny = dy / d;
                    boolean as = a.st == Pawn.SLIDE || a.st == Pawn.FALL || a.st == Pawn.DOWN || a.st == Pawn.DIVE;
                    boolean bs = b.st == Pawn.SLIDE || b.st == Pawn.FALL || b.st == Pawn.DOWN || b.st == Pawn.DIVE;
                    if (as || bs) continue;
                    float wa = a.pl.phy + 50, wb = b.pl.phy + 50;
                    float fa = wb / (wa + wb), fb = wa / (wa + wb);
                    a.x -= nx * push * 2 * fa; a.y -= ny * push * 2 * fa;
                    b.x += nx * push * 2 * fb; b.y += ny * push * 2 * fb;
                }
            }
        }
    }

    // ================= ball
    void carry(float dt) {
        Pawn o = owner;
        if (o == null) return;
        if (o.st != Pawn.N && o.st != Pawn.CELEB && o.st != Pawn.THROW) { release(o); return; }
        float sp = o.speed();
        float off;
        if (o.role == Player.GK && mode != M_PLAY) off = 0.35f;
        else off = 0.36f + Math.min(sp, 8f) * 0.045f + 0.17f * (0.5f + 0.5f * (float) Math.sin(o.phase * 2f - 0.8f)) * Math.min(1, sp / 2f);
        float tx = o.x + o.fx * off, ty = o.y + o.fy * off;
        float k = Math.min(1f, dt * 22f);
        bx += (tx - bx) * k; by += (ty - by) * k;
        bz = 0; bvz = 0; bspin = 0;
        bvx = o.vx; bvy = o.vy;
        brx += bvx * dt / 0.11f * 0.55f; bry += bvy * dt / 0.11f * 0.55f;
        ownerT += dt;
        if (o.role == Player.GK && mode == M_PLAY && owner == o) {
            // keeper holds
            bx = o.x + o.fx * 0.3f; by = o.y + o.fy * 0.3f; bz = 1.1f;
        }
    }

    void setOwnerSilent(Pawn p) {
        owner = p; lastTouch = p; lastTouchSide = p.side; ownerT = 0;
        bx = p.x + p.fx * 0.4f; by = p.y + p.fy * 0.4f; bz = 0; bvx = bvy = bvz = 0;
        passTarget = null; shotBy = null;
    }

    void release(Pawn p) {
        if (owner == p) {
            owner = null;
            bvx = p.vx * 0.8f; bvy = p.vy * 0.8f; bvz = 0;
            prevOwner = p;
        }
    }

    void ballStep(float dt) {
        if (owner != null) { carry(dt); return; }
        int n = 3; float h = dt / n;
        for (int i = 0; i < n; i++) {
            if (bz <= 0.025f && Math.abs(bvz) < 0.6f) { bz = 0; bvz = 0; }
            boolean air = bz > 0f || bvz > 0f;
            if (air) {
                bvz -= GRAV * h;
                float k = (float) Math.exp(-0.10f * h);
                bvx *= k; bvy *= k; bvz *= (float) Math.exp(-0.03f * h);
                if (Math.abs(bspin) > 0.01f) {
                    float c = (float) Math.cos(bspin * h), s = (float) Math.sin(bspin * h);
                    float nx = bvx * c - bvy * s, ny = bvx * s + bvy * c;
                    bvx = nx; bvy = ny;
                    bspin *= (float) Math.exp(-0.35f * h);
                }
            }
            bx += bvx * h; by += bvy * h; bz += bvz * h;
            if (bz < 0) {
                bz = 0;
                if (bvz < -1.2f) {
                    if (bvz < -4f) sounds.add(S_BOUNCE);
                    bvz = -bvz * 0.58f; bvx *= 0.9f; bvy *= 0.9f;
                } else bvz = 0;
            }
            if (bz <= 0f && bvz == 0f) {
                float sp = U.len(bvx, bvy);
                if (sp > 0) {
                    float rain = 1f - weatherRain * 0.08f;
                    float ns = sp * (float) Math.exp(-0.46f * h) * (rain > 0 ? 1 : 1) - 0.32f * h;
                    if (ns < 0.05f) ns = 0;
                    float r = ns / sp;
                    bvx *= r; bvy *= r;
                }
                bspin = 0;
            }
            goalFrame();
            if (inNet()) netPhysics(h);
        }
        float sp = U.len(bvx, bvy);
        brx += bvx * dt / 0.11f * 0.5f; bry += bvy * dt / 0.11f * 0.5f;
    }

    boolean inNet() { return (bx < 0 || bx > L) && Math.abs(by - CY) < GH + 1f; }

    void netPhysics(float dt) {
        float line = bx < 0 ? 0 : L;
        float depth = Math.abs(bx - line);
        float k = (float) Math.exp(-3.5f * dt);
        bvx *= k; bvy *= k;
        if (depth > 2.2f) { bx = line + (bx < 0 ? -2.2f : 2.2f); bvx *= -0.15f; }
        if (Math.abs(by - CY) > GH + 0.4f && Math.abs(by - CY) < GH + 1f) { /* side net */ }
        if (bz > GOAL_H + 0.1f) { bz = GOAL_H + 0.1f; bvz = -Math.abs(bvz) * 0.2f; }
        if (Math.abs(by - CY) > GH + 0.9f) { by = CY + U.sign(by - CY) * (GH + 0.9f); bvy *= -0.2f; }
    }

    void goalFrame() {
        for (int e = 0; e < 2; e++) {
            float line = e == 0 ? 0 : L;
            // posts
            for (int s = -1; s <= 1; s += 2) {
                float py = CY + s * GH;
                float dx = bx - line, dy = by - py;
                float d = U.len(dx, dy);
                if (d < BR + 0.06f && bz < GOAL_H + 0.05f && d > 1e-4f) {
                    float nx = dx / d, ny = dy / d;
                    float vn = bvx * nx + bvy * ny;
                    if (vn < 0) {
                        bvx -= 1.6f * vn * nx; bvy -= 1.6f * vn * ny;
                        bx = line + nx * (BR + 0.065f); by = py + ny * (BR + 0.065f);
                        sounds.add(S_POST); woodwork();
                    }
                }
            }
            // crossbar
            if (Math.abs(by - CY) < GH) {
                float dx = bx - line, dz = bz - GOAL_H;
                float d = U.len(dx, dz);
                if (d < BR + 0.06f && d > 1e-4f) {
                    float nx = dx / d, nz = dz / d;
                    float vn = bvx * nx + bvz * nz;
                    if (vn < 0) {
                        bvx -= 1.55f * vn * nx; bvz -= 1.55f * vn * nz;
                        bx = line + nx * (BR + 0.065f); bz = GOAL_H + nz * (BR + 0.065f);
                        sounds.add(S_POST); woodwork();
                    }
                }
            }
        }
    }

    void woodwork() {
        if (shotBy != null) {
            S[shotBy.side].woodwork++;
            say("В штангу! Какое невезение!");
            banner("ШТАНГА!", 1.2f, 0);
            sounds.add(S_OOH);
        }
    }

    // ================= contacts
    void contacts(float dt) {
        if (owner != null) {
            // owner may lose ball to a very close opponent when owner is stumbling etc.
            return;
        }
        // goalkeepers first
        for (int s = 0; s < 2; s++) keeperContact(S[s].pw[0]);
        if (owner != null) return;
        Pawn best = null; float bestScore = 1e9f;
        float sp = ballSpeed();
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            if (p.lock > 0 || p.st == Pawn.FALL || p.st == Pawn.DOWN || p.st == Pawn.SENT || p.st == Pawn.GETUP || p.st == Pawn.DIVE) continue;
            if (p.role == Player.GK && mode == M_PLAY && shotBy != null && shotBy.side != p.side) continue;
            float dx = bx - p.x, dy = by - p.y;
            float d = U.len(dx, dy);
            float reach = p.st == Pawn.SLIDE ? 1.3f : 0.72f + Math.min(0.5f, sp * 0.02f);
            if (p == passTarget) reach += 0.15f;
            boolean ground = bz < 0.65f;
            boolean head = bz >= 0.8f && bz < 2.2f && d < 1.0f;
            if (d > reach && !head) continue;
            if (!ground && !head) continue;
            float skill = (p.pl.dri * 0.5f + p.pl.pas * 0.3f + p.pl.phy * 0.2f) / 100f;
            float score = d * (1.4f - skill * 0.5f) + U.rnd() * 0.25f;
            if (head) score += 0.2f;
            if (score < bestScore) { bestScore = score; best = p; }
        }
        if (best != null) touch(best);
    }

    void touch(Pawn p) {
        float sp = U.len(bvx, bvy);
        boolean head = bz >= 0.8f;
        float ctrl = (p.pl.dri * 0.55f + p.pl.pas * 0.3f + p.pl.phy * 0.15f) / 100f;
        if (p.st == Pawn.SLIDE) ctrl += 0.2f;
        float difficulty = sp / (9f + 16f * ctrl);
        if (bz > 0.4f) difficulty += 0.25f;
        if (p == passTarget) difficulty *= 0.7f;
        // offside check on any teammate touch
        if (p.side == lastTouchSide && lastKicker != null && lastKicker != p && p.offPos && p.offT > 0 && !noOffside && lastKicker.side == p.side) {
            callOffside(p);
            return;
        }
        if (head && bz >= 0.8f) {
            headBall(p);
            return;
        }
        if (difficulty < 1f) {
            takePossession(p);
        } else {
            // heavy touch / deflection
            float k = 0.3f + U.rnd() * 0.25f;
            float ang = (U.rnd() - 0.5f) * 1.2f;
            float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
            float nvx = bvx * c - bvy * s, nvy = bvx * s + bvy * c;
            bvx = nvx * k; bvy = nvy * k; bvz = Math.min(bvz, 0) * 0.3f + U.rnd() * 1.5f;
            if (shotBy != null && shotBy.side != p.side && U.chance(0.4f)) {
                float back = -S[p.side].dir; // towards own goal line
                bvx = back * U.rnd(5f, 9f); bvy = (by < CY ? -1 : 1) * U.rnd(2f, 6f); bvz = U.rnd(1f, 3f);
            }
            p.lock = 0.2f;
            if (p.side != lastTouchSide) { p.stat.inter++; }
            lastTouch = p; lastTouchSide = p.side;
            if (shotBy != null && shotBy.side != p.side) { p.stat.rating += 0.15f; shotBy = null; }
            else if (shotBy != null) {}
            passTarget = null;
            sounds.add(S_KICK);
        }
    }

    void takePossession(Pawn p) {
        boolean sameTeamPass = lastKicker != null && lastKicker.side == p.side && lastKicker != p && time - lastKickT < 4f;
        if (sameTeamPass) {
            S[p.side].passOK++;
            lastKicker.stat.passOK++;
            lastPasser = lastKicker; lastPassT = time;
            if (lastKickKind == K_THROUGH) lastKicker.stat.keyPass++;
        } else if (lastKicker != null && lastKicker.side != p.side && time - lastKickT < 4f) {
            p.stat.inter++; p.stat.rating += 0.12f;
            lastKicker.stat.rating -= 0.04f;
            lastPasser = null;
        } else if (prevOwner != null && prevOwner.side == p.side) {
            // regained own loose ball
        } else lastPasser = null;
        if (shotBy != null && shotBy.side != p.side) { p.stat.rating += 0.25f; }
        owner = p; lastTouch = p; lastTouchSide = p.side; ownerT = 0; passTarget = null; shotBy = null;
        p.lock = 0; noOffside = false;
        for (Pawn q : all) { q.offPos = false; }
        if (p.role == Player.GK) {
            p.st = Pawn.N;
        }
        if (!S[p.side].human) {} 
        p.planKind = 0; p.thinkT = 0.12f + U.rnd() * 0.12f;
        // switch human control on receive
        Side sd = S[p.side];
        if (sd.human && p.role != Player.GK) Ctl.setControlled(this, sd, p);
    }

    void headBall(Pawn p) {
        // header: attackers try to score/pass, defenders clear
        Side sd = S[p.side], od = S[1 - p.side];
        if (lastKicker != null && p.side == lastKicker.side && p != lastKicker && p.offPos && p.offT > 0 && !noOffside) { callOffside(p); return; }
        p.st = Pawn.JUMP; p.stT = 0; p.lock = 0.5f;
        float gx = goalX(sd);
        float toGoalD = U.dist(p.x, p.y, gx, CY);
        float sk = (p.pl.phy * 0.5f + p.pl.sho * 0.3f + p.pl.def * 0.2f) / 100f;
        boolean attacking = depthOf(sd, p.x) > L * 0.62f;
        float tx, ty, spd, vz;
        if (attacking && toGoalD < 22) {
            // header at goal
            float aimY = CY + U.rnd(-2.8f, 2.8f);
            tx = gx; ty = aimY;
            spd = 12 + sk * 9;
            float err = (1.0f - sk) * 0.35f + 0.06f;
            float ang = (float) Math.atan2(ty - p.y, tx - p.x) + U.gauss() * err;
            bvx = (float) Math.cos(ang) * spd; bvy = (float) Math.sin(ang) * spd;
            float t = toGoalD / spd;
            vz = (1.0f + U.rnd(-0.6f, 0.8f) + 4.9f * t * t - bz) / Math.max(t, 0.1f);
            bvz = vz;
            shotBy = p; p.stat.shots++; sd.shots++;
            if (Math.abs(aimY - CY) < GH && vz < 8) { p.stat.sot++; sd.sot++; }
            lastKickKind = K_SHOT;
            say(p.pl.last + " бьёт головой!");
        } else {
            // clear or nod to teammate
            Pawn t = AI.bestTarget(this, p, false);
            if (t != null && !(depthOf(sd, p.x) < 24f)) {
                float dx = t.x - p.x, dy = t.y - p.y, d = Math.max(1, U.len(dx, dy));
                float v = Math.min(18, 7 + d * 0.5f);
                bvx = dx / d * v; bvy = dy / d * v; bvz = 2.5f;
            } else {
                float dir = sd.dir;
                float ang = (float) Math.atan2((U.rnd() - 0.5f) * 30, dir * 20) + U.gauss() * 0.15f;
                float v = 13 + sk * 7;
                bvx = (float) Math.cos(ang) * v; bvy = (float) Math.sin(ang) * v; bvz = 4.5f;
                if (bvx * dir < 0) bvx = -bvx;
            }
            shotBy = null;
            lastKickKind = K_CLEAR;
            p.stat.rating += (depthOf(sd, p.x) < 20 ? 0.12f : 0.04f);
        }
        owner = null; passTarget = null; lastTouch = p; lastTouchSide = p.side; lastKicker = p; lastKickT = time;
        bspin = 0;
        sounds.add(S_KICK);
        for (Pawn q : all) q.offPos = false;
    }

    void keeperContact(Pawn g) {
        if (g.st == Pawn.SENT || g.lock > 0) return;
        if (owner != null) return;
        float dx = bx - g.x, dy = by - g.y;
        float d = U.len(dx, dy);
        boolean diving = g.st == Pawn.DIVE;
        float reachR = 0.85f + (diving ? 1.3f : 0.15f);
        float reachZ = diving ? 2.7f : 2.35f;
        if (d > reachR || bz > reachZ) return;
        if (g.st == Pawn.FALL || g.st == Pawn.STUMBLE) return;
        float sp3 = (float) Math.sqrt(bvx * bvx + bvy * bvy + bvz * bvz);
        boolean shot = shotBy != null && shotBy.side != g.side;
        float skill = g.pl.gk / 100f;
        Side sd = S[g.side];
        if (!shot) {
            // loose ball / pass back / cross
            if (lastKicker != null && lastKicker.side == g.side && lastKickKind == K_GROUND && lastKicker != g && time - lastKickT < 3 && false) return;
            if (sp3 < 17 && U.chance(0.55f + skill * 0.4f)) { gkCollect(g, sd); return; }
            // punch away
            float aw = sd.dir;
            bvx = aw * (7 + U.rnd(0, 6)); bvy = (U.rnd() - 0.5f) * 14; bvz = 3 + U.rnd(0, 3);
            lastTouch = g; lastTouchSide = g.side; g.lock = 0.5f; lastKicker = g; lastKickT = time; passTarget = null;
            sounds.add(S_SAVE);
            return;
        }
        float diff = Math.min(1f, (sp3 - 12f) / 24f);
        float place = Math.min(1f, d / reachR);
        float pSave = 0.76f + (skill - 0.7f) * 0.9f - diff * 0.30f - place * 0.20f + (diving ? 0.0f : 0.06f);
        if (g.side != humanSide && !autoPlay) pSave += new float[]{-0.12f, -0.02f, 0.03f, 0.09f}[diff() ];
        pSave = U.clamp(pSave, 0.08f, 0.93f);
        shotBy.stat.shots += 0; // counted on shooting
        if (U.chance(pSave)) {
            g.stat.saves++; sd.saves++;
            g.stat.rating += 0.55f;
            sounds.add(S_SAVE);
            float catchP = (0.42f + skill * 0.35f) * (sp3 < 21 ? 1f : 0.35f) * (place < 0.7f ? 1f : 0.5f);
            if (U.chance(catchP) && bz < 2.2f) {
                gkCollect(g, sd);
                banner("СЕЙВ!", 1.0f, 0);
                say("Вратарь " + g.pl.last + " забирает мяч!");
            } else {
                // parry
                float aw = sd.dir;
                float lat = (U.chance(0.5f) ? 1 : -1) * U.rnd(2, 7);
                if (U.chance(0.4f)) { bvx = -aw * U.rnd(2f, 4f); bvy = (by < CY ? -1 : 1) * U.rnd(5f, 9f); bvz = U.rnd(1f, 3f); }
                else { bvx = aw * U.rnd(1.5f, 5f); bvy = (by < CY ? -1 : 1) * Math.abs(lat) * (U.chance(.7f) ? 1 : -1); bvz = U.rnd(1.5f, 5f); }
                lastTouch = g; lastTouchSide = g.side; g.lock = 0.6f; lastKicker = g; lastKickT = time; passTarget = null; shotBy = null;
                banner("СЕЙВ!", 1.0f, 0);
                say("Вратарь " + g.pl.last + " отбивает удар!");
            }
        } else {
            // fingertip: reduce speed slightly, GK locked
            g.lock = 0.55f;
            bvx *= 0.82f; bvy *= 0.82f; bvz *= 0.9f;
            sounds.add(S_SAVE);
        }
    }
    int diff() { return diff; }

    void gkCollect(Pawn g, Side sd) {
        owner = g; lastTouch = g; lastTouchSide = g.side; ownerT = 0; passTarget = null; shotBy = null;
        bvx = bvy = bvz = 0; g.vx *= 0.3f; g.vy *= 0.3f;
        g.st = Pawn.N; g.planKind = 0; g.thinkT = 0.9f + U.rnd() * 0.8f;
        g.fx = sd.dir; g.fy = 0;
        noOffside = false;
        for (Pawn q : all) q.offPos = false;
        sounds.add(S_SAVE);
        if (S[g.side].human) {} 
    }

    // ================= kicking
    float pressureOn(Pawn p) {
        float best = 99;
        for (Pawn q : S[1 - p.side].pw) {
            if (q.st == Pawn.SENT) continue;
            float d = U.dist(p.x, p.y, q.x, q.y);
            if (d < best) best = d;
        }
        return U.clamp(1f - (best - 0.8f) / 3.5f, 0, 1);
    }

    void kick(Pawn p, float vx, float vy, float vz, float spin, int kind) {
        Side sd = S[p.side];
        if (owner == p) owner = null;
        prevOwner = p;
        bvx = vx; bvy = vy; bvz = vz; bspin = spin;
        bx = p.x + p.fx * 0.45f; by = p.y + p.fy * 0.45f; bz = Math.max(bz, 0.05f);
        if (kind == K_SHOT) bz = 0.1f;
        lastTouch = p; lastTouchSide = p.side; lastKicker = p; lastKickT = time; lastKickKind = kind;
        p.lock = 0.4f; p.kickT = 0.3f;
        sounds.add(S_KICK);
        if (kind != K_SHOT) { sd.passes++; p.stat.passes++; }
        // offside snapshot
        for (Pawn q : all) { q.offPos = false; }
        if (!noOffside && (kind == K_GROUND || kind == K_THROUGH || kind == K_LOB || kind == K_CROSS) && mode == M_PLAY) {
            float bd = depthOf(sd, p.x);
            for (Pawn q : sd.pw) {
                if (q == p || q.st == Pawn.SENT) continue;
                float qd = depthOf(sd, q.x);
                if (qd > L / 2 && qd > bd + 0.2f && qd > sd.offDepth + 0.2f) { q.offPos = true; q.offT = 3.5f; }
            }
        } else if (noOffside) { /* first restart pass exempt */ }
        noOffside = false;
        ownerT = 0;
    }

    // ================= rules
    void ruleChecks(float dt) {
        if (mode != M_PLAY) return;
        // goal
        if (bx < 0 || bx > L) {
            float line = bx < 0 ? 0 : L;
            if (Math.abs(by - CY) < GH - 0.05f && bz < GOAL_H - 0.0f) {
                Side scorer = bx > L ? attackerAt(+1) : attackerAt(-1);
                scoreGoal(scorer);
                return;
            }
            // out over goal line
            Side att = bx > L ? attackerAt(+1) : attackerAt(-1);
            Side def = S[1 - sideIdx(att)];
            boolean corner = lastTouchSide == def.team.id * 0 + sideIdx(def);
            if (corner) {
                float cx = bx > L ? L : 0;
                float cyy = by < CY ? 0 : W;
                S[sideIdx(att)].corners++;
                startSetPiece(SP_CORNER, sideIdx(att), cx, cyy);
                say("Угловой в пользу " + att.name);
            } else {
                startSetPiece(SP_GOALKICK, sideIdx(def), bx > L ? L - 5.5f : 5.5f, CY + (by < CY ? -4 : 4) * 0.0f);
            }
            sounds.add(S_WHISTLE);
            return;
        }
        if (by < 0 || by > W) {
            int taker = 1 - lastTouchSide;
            startSetPiece(SP_THROW, taker, U.clamp(bx, 1, L - 1), by < 0 ? 0 : W);
            return;
        }
        // injury random
        // loose-ball nothing
    }

    Side attackerAt(int dirSign) { return S[0].dir == dirSign ? S[0] : S[1]; }

    void checkPeriodEnd() {
        float end = periodEnd + (period == 1 || period == 3 ? stoppage : (period == 0 ? stoppage * 0.8f : 20));
        if (period == 2) end = periodEnd + 20;
        if (clock >= end) {
            boolean calm = owner == null || depthOf(S[owner.side], owner.x) < L * 0.66f;
            if (calm && ball3Speed() < 12 || clock > end + 25) endPeriod();
        }
    }
    float ball3Speed() { return (float) Math.sqrt(bvx * bvx + bvy * bvy + bvz * bvz); }

    void endPeriod() {
        sounds.add(S_WHISTLE); sounds.add(S_WHISTLE); sounds.add(S_WHISTLE);
        if (period == 0) { mode = M_HALF; modeT = 0; halfPending = true; say("Перерыв. Счёт " + S[0].score + ":" + S[1].score); banner("ПЕРЕРЫВ", 3f, 0); }
        else if (period == 1) {
            if (cup && S[0].score == S[1].score) { mode = M_ETBREAK; modeT = 0; halfPending = true; banner("ДОПОЛНИТЕЛЬНОЕ ВРЕМЯ", 3f, 0); say("Ничья! Назначено дополнительное время"); }
            else { mode = M_FULL; modeT = 0; banner("КОНЕЦ МАТЧА", 3f, 0); }
        } else if (period == 2) { mode = M_HALF; modeT = 0; halfPending = true; banner("ПЕРЕРЫВ ДОП. ВРЕМЕНИ", 3f, 0); }
        else {
            if (S[0].score == S[1].score) { mode = M_ETBREAK; modeT = 0; halfPending = true; banner("СЕРИЯ ПЕНАЛЬТИ", 3f, 0); period = 4; }
            else { mode = M_FULL; modeT = 0; banner("КОНЕЦ МАТЧА", 3f, 0); }
        }
        owner = null; bvx = bvy = bvz = 0;
        for (Pawn p : all) { p.wvx = p.wvy = 0; }
    }

    void updateBreak(float dt) {
        for (Pawn p : all) { p.wvx = p.wvy = 0; if (p.st == Pawn.CELEB) p.st = Pawn.N; }
        worldStepIdle(dt);
        if (autoPlay && modeT > 0.5f) continueFromBreak();
    }

    /** Called by UI when the user dismisses half-time. */
    void continueFromBreak() {
        if (!halfPending && mode != M_HALF && mode != M_ETBREAK) return;
        halfPending = false;
        if (period == 4) { startShootout(); return; }
        period++;
        if (period == 1) {
            periodStart = 45 * 60; periodEnd = 90 * 60; stoppage = U.ri(90, 240) + stoppageExtra();
            S[0].dir = -1; S[1].dir = 1;
            startKickoff(1, true);
        } else if (period == 2) {
            periodStart = 90 * 60; periodEnd = 105 * 60; clock = 90 * 60; stoppage = 20;
            S[0].dir = 1; S[1].dir = -1;
            startKickoff(0, true);
        } else if (period == 3) {
            periodStart = 105 * 60; periodEnd = 120 * 60; clock = 105 * 60; stoppage = 30;
            S[0].dir = -1; S[1].dir = 1;
            startKickoff(1, true);
        }
        if (period == 1) clock = 45 * 60;
    }

    float stoppageExtra() {
        int ev = 0;
        for (Evt e : events) if (e.type != E_SUB) ev++;
        return ev * 12;
    }

    void updateFull(float dt) {
        for (Pawn p : all) { p.wvx = p.wvy = 0; }
        worldStepIdle(dt);
        if (!finished) {
            finished = true;
            for (Pawn p : all) {
                if (S[p.side].score > S[1 - p.side].score) { if (p.st != Pawn.SENT) { p.st = Pawn.CELEB; p.stT = 0; } }
            }
            if (shootout) winner = S[0].penScore > S[1].penScore ? 0 : 1;
            else if (S[0].score > S[1].score) winner = 0; else if (S[1].score > S[0].score) winner = 1; else winner = -1;
            computeRatings();
        }
    }

    void computeRatings() {
        for (int s = 0; s < 2; s++) {
            Side sd = S[s], od = S[1 - s];
            for (Stat st : sd.stats.values()) {
                if (!st.played) continue;
                float r = st.rating;
                r += st.goals * 1.1f + st.assists * 0.7f + st.keyPass * 0.15f;
                if (st.passes > 5) r += (st.passOK / (float) st.passes - 0.7f) * 1.2f;
                r += st.tackleWon * 0.1f - st.fouls * 0.12f - st.yel * 0.3f - st.red * 1.5f;
                if (st.pl.pos == Player.GK) r += st.saves * 0.18f + (od.score == 0 ? 0.7f : -od.score * 0.15f);
                else if (st.pl.pos == Player.CB || st.pl.pos == Player.LB || st.pl.pos == Player.RB) r += (od.score == 0 ? 0.4f : -od.score * 0.1f);
                r += (sd.score - od.score) * 0.12f;
                st.rating = U.clamp(r, 3.0f, 10f);
            }
        }
    }

    // ================= goals
    void scoreGoal(Side att) {
        int ai = sideIdx(att);
        Pawn scorer = shotBy != null && shotBy.side == ai ? shotBy : lastTouch;
        boolean own = scorer != null && scorer.side != ai;
        att.score++;
        mode = M_GOAL; modeT = 0; goalTime = time;
        String txt;
        int min = minute();
        if (own) {
            events.add(new Evt(min, E_OWN, ai, scorer.pl, scorer.pl.shortName() + " (автогол)"));
            txt = "Автогол! " + scorer.pl.last;
        } else {
            scorer.stat.goals++; scorer.stat.rating += 0.9f;
            Pawn as = lastPasser != null && lastPasser.side == ai && lastPasser != scorer && time - lastPassT < 6 ? lastPasser : null;
            if (as != null) { as.stat.assists++; as.stat.rating += 0.5f; }
            events.add(new Evt(min, E_GOAL, ai, scorer.pl, scorer.pl.shortName() + (as != null ? " (пас: " + as.pl.shortName() + ")" : "")));
            txt = "ГОЛ! " + scorer.pl.last;
            scorer.pl.goals += 0;
        }
        banner(txt, 4f, 1);
        say("ГООООЛ! " + att.name + " " + S[0].score + ":" + S[1].score);
        sounds.add(S_GOAL); sounds.add(S_NET); sounds.add(S_WHISTLE);
        shake = 0.6f;
        // celebration
        Pawn cel = own ? null : scorer;
        for (Pawn p : att.pw) {
            if (p == cel) { p.st = Pawn.CELEB; p.stT = 0; }
        }
        owner = null; shotBy = null;
        if (cel != null) {
            cel.celebX = U.clamp(cel.x + att.dir * 10, 6, L - 6);
            cel.celebY = cel.y < CY ? 5 : W - 5;
            cel.st = Pawn.N; cel.human = false;
        }
        S[1 - ai].ctl = null;
        celScorer = cel;
        celebPhase = 0;
    }
    Pawn celScorer; int celebPhase;

    void updateGoalCelebration(float dt) {
        // ball settles in net, players celebrate
        for (Pawn p : all) { p.human = false; }
        updateSideInfo(dt);
        Side att = null;
        for (Side s : S) if (s.score > 0 && lastScoreSide() == sideIdx(s)) att = s;
        for (Pawn p : all) {
            if (p.st == Pawn.SENT) continue;
            if (att != null && p.side == sideIdx(att)) {
                if (celScorer != null && p == celScorer) {
                    if (modeT < 1.6f) AI.goTo(this, p, p.celebX, p.celebY, 1f, true);
                    else { p.wvx = p.wvy = 0; if (p.st == Pawn.N) { p.st = Pawn.CELEB; p.stT = 0; } }
                } else if (celScorer != null && p.role != Player.GK) {
                    if (modeT > 1.0f) AI.goTo(this, p, celScorer.x - att.dir * 1.5f + (p.slot % 3 - 1) * 1.2f, celScorer.y + (p.slot % 4 - 1.5f) * 0.9f, 0.9f, false);
                    else AI.goTo(this, p, p.x, p.y, 0.3f, false);
                } else { p.wvx = p.wvy = 0; }
            } else {
                // dejected: walk toward own half
                AI.goTo(this, p, p.x - S[p.side].dir * 3 * 0, p.y, 0.2f, false);
                p.wvx = p.wvy = 0;
            }
        }
        for (int i = 0; i < 22; i++) integrate(all[i], dt);
        separate();
        ballStep(dt);
        if (modeT > 5.5f) {
            if (clock >= periodEnd) {
                // end of period reached during celebration
                if (clock >= periodEnd + 0) endPeriod();
            } else {
                int conc = 1 - lastScoreSide();
                startKickoff(conc, true);
            }
        }
    }

    int lastScoreSide() {
        for (int i = events.size() - 1; i >= 0; i--) {
            Evt e = events.get(i);
            if (e.type == E_GOAL || e.type == E_OWN) return e.side;
        }
        return 0;
    }

    // ================= set pieces
    void startKickoff(int side, boolean reset) {
        kickoffSide = side;
        mode = M_KICKOFF; modeT = 0; spType = SP_KICKOFF; spSide = side; spReady = false;
        owner = null; bx = L / 2; by = CY; bz = 0; bvx = bvy = bvz = 0; shotBy = null; passTarget = null;
        celScorer = null;
        for (Side s : S) { s.ctl = null; }
        for (Pawn p : all) {
            if (p.st == Pawn.SENT) continue;
            if (p.st != Pawn.N) { p.st = Pawn.N; }
            p.z = 0; p.vx = p.vy = 0; p.wvx = p.wvy = 0; p.human = false;
        }
        for (int s = 0; s < 2; s++) {
            Side sd = S[s];
            for (Pawn p : sd.pw) {
                if (p.st == Pawn.SENT) continue;
                float u = Data.FORM_U[sd.formation][p.slot], v = Data.FORM_V[sd.formation][p.slot];
                float depth = Math.min(u * L * 0.9f, L / 2 - 2f);
                if (p.role == Player.GK) depth = 3f;
                p.x = xOfDepth(sd, depth); p.y = yOfV(sd, v);
                p.fx = sd.dir; p.fy = 0;
                p.tx = p.x; p.ty = p.y;
            }
        }
        Side ks = S[side];
        // taker: most advanced forward
        Pawn taker = null; float bd = -1;
        for (Pawn p : ks.pw) { if (p.role == Player.GK) continue; float d = depthOf(ks, p.x); if (d > bd) { bd = d; taker = p; } }
        spTaker = taker;
        taker.x = L / 2 - ks.dir * 0.55f; taker.y = CY; taker.tx = taker.x; taker.ty = taker.y; taker.fx = ks.dir;
        // second forward beside
        Pawn second = null; float bd2 = -1;
        for (Pawn p : ks.pw) { if (p == taker || p.role == Player.GK) continue; float d = depthOf(ks, p.x); if (d > bd2) { bd2 = d; second = p; } }
        if (second != null) { second.x = L / 2 - ks.dir * 3f; second.y = CY + 5; second.tx = second.x; second.ty = second.y; }
        if (ks.human && !autoPlay) { Ctl.setControlled(this, ks, taker); }
        for (Pawn p : all) { p.lock = 0; }
        for (Pawn p : all) { p.phase = U.rnd() * 6; }
        ref.x = L / 2 - 4; ref.y = CY + 9;
        lastTouchSide = side;
        noOffside = true;
        lastKicker = null; lastPasser = null;
        spDelay = 2f;
    }

    int[] spCount = new int[8];
    void startSetPiece(int type, int side, float x, float y) {
        spCount[type]++;
        mode = M_SET; modeT = 0; spType = type; spSide = side; spX = x; spY = y; spTimer = 0; spReady = false; spHuman = false;
        owner = null; bx = x; by = y; bz = 0; bvx = bvy = bvz = 0; bspin = 0; shotBy = null; passTarget = null;
        for (Pawn p : all) { p.offPos = false; p.human = false; if (p.st == Pawn.JUMP || p.st == Pawn.SLIDE) p.st = Pawn.N; }
        Side sd = S[side];
        spTaker = chooseTaker(type, sd, x, y);
        spDelay = type == SP_PENALTY ? 2.6f : (type == SP_CORNER ? 2.4f : (type == SP_GOALKICK ? 1.4f : 1.5f)) + U.rnd(0, 0.6f);
        spAimX = 0; spAimY = 0; spPower = 0;
        noOffside = type == SP_THROW || type == SP_GOALKICK || type == SP_CORNER;
        lastTouchSide = side;
        lastKicker = null;
        setPiecePositions();
        for (Side s : S) s.ctl = null;
        if (sd.human && !autoPlay) {
            Ctl.setControlled(this, sd, spTaker);
            spHuman = true;
            if (type == SP_FREEKICK || type == SP_PENALTY || type == SP_CORNER) {
                float gx = goalX(sd);
                spAimX = gx; spAimY = CY;
            }
        }
        if (type != SP_KICKOFF) sounds.add(S_WHISTLE);
    }

    Pawn chooseTaker(int type, Side sd, float x, float y) {
        Pawn best = null; float bs = -1e9f;
        for (Pawn p : sd.pw) {
            if (p.st == Pawn.SENT) continue;
            float score;
            float d = U.dist(p.x, p.y, x, y);
            switch (type) {
                case SP_GOALKICK: score = p.role == Player.GK ? 1000 : -d; break;
                case SP_THROW: score = p.role == Player.GK ? -999 : -d; break;
                case SP_CORNER: score = p.pl.pas * 1.0f + p.pl.sho * 0.2f - (p.role == Player.GK ? 999 : 0) - d * 0.05f; break;
                case SP_PENALTY: score = p.pl.sho * 1.0f + p.pl.phy * 0.1f - (p.role == Player.GK ? 999 : 0); break;
                case SP_FREEKICK: {
                    float gd = U.dist(x, y, goalX(sd), CY);
                    score = gd < 32 ? p.pl.sho * 0.7f + p.pl.pas * 0.6f : p.pl.pas * 1.0f;
                    if (p.role == Player.GK) score = -999;
                    score -= d * 0.1f;
                    break;
                }
                default: score = -d;
            }
            if (score > bs) { bs = score; best = p; }
        }
        if (type == SP_FREEKICK && spVictim != null && false) return spVictim;
        return best;
    }

    void setPiecePositions() {
        Side sd = S[spSide], od = S[1 - spSide];
        int si = spSide;
        float gx = goalX(sd);
        // default: anchors based on ball at set-piece location -> use formation anchors
        for (Pawn p : all) { p.tx = p.ax; p.ty = p.ay; }
        // taker stand position
        Pawn tk = spTaker;
        switch (spType) {
            case SP_THROW: {
                tk.tx = spX; tk.ty = spY < CY ? -0.6f : W + 0.6f;
                // two nearby options
                int c = 0;
                float inward = spY < CY ? 1 : -1;
                Pawn[] cand = nearest(sd, spX, spY, 3, tk);
                for (int i = 0; i < cand.length; i++) {
                    Pawn q = cand[i]; if (q == null) continue;
                    q.tx = U.clamp(spX + (i == 0 ? -7 : (i == 1 ? 8 : 15)) * sd.dir * (i == 2 ? 1 : 1), 3, L - 3);
                    q.ty = spY + inward * (i == 2 ? 14 : 8 + i * 2);
                }
                markNearby(od, sd, 2f);
                break;
            }
            case SP_GOALKICK: {
                tk.tx = spX - sd.dir * 0.8f; tk.ty = CY;
                for (Pawn p : od.pw) { // opp stay outside box
                    if (p.role == Player.GK) continue;
                    float depthFromGoal = Math.abs(p.ax - ownGoalX(sd));
                    if (depthFromGoal < 24) { p.tx = xOfDepth(od, L - 24 - 2); p.tx = ownGoalX(sd) + sd.dir * (24 + (p.slot % 3)); }
                    p.ty = p.ay;
                }
                float ownX = ownGoalX(sd);
                for (Pawn p : sd.pw) {
                    if (p == tk) continue;
                    if (p.role == Player.CB || p.role == Player.LB || p.role == Player.RB) {
                        p.tx = ownX + sd.dir * U.clamp(16 + (p.slot % 3) * 2, 16, 24); p.ty = p.ay;
                        if (p.role == Player.CB) p.ty = CY + (p.slot % 2 == 0 ? -14 : 14);
                    }
                }
                break;
            }
            case SP_CORNER: {
                float cdir = spX > L / 2 ? -1 : 1; // direction into field along x
                tk.tx = spX + cdir * 0.9f; tk.ty = spY + (spY < CY ? 0.9f : -0.9f);
                float gl = spX;
                // attackers (best headers)
                ArrayList<Pawn> att = new ArrayList<Pawn>();
                for (Pawn p : sd.pw) if (p != tk && p.role != Player.GK) att.add(p);
                java.util.Collections.sort(att, new java.util.Comparator<Pawn>() {
                    public int compare(Pawn a, Pawn b) { return b.pl.phy + b.pl.sho / 2 - a.pl.phy - a.pl.sho / 2; }
                });
                float[][] spots = {{5.5f, 4.0f}, {9.5f, -1.0f}, {7.5f, -5.5f}, {13.5f, 1.5f}, {16.5f, -8f}, {18f, 6f}};
                for (int i = 0; i < Math.min(6, att.size()); i++) {
                    Pawn q = att.get(i);
                    q.tx = gl + cdir * spots[i][0]; q.ty = CY + spots[i][1] * (spY < CY ? 1 : -1);
                }
                // remaining attackers stay back
                // defenders: mark
                int di = 0;
                ArrayList<Pawn> defs = new ArrayList<Pawn>();
                for (Pawn p : od.pw) if (p.role != Player.GK) defs.add(p);
                // two on posts
                int post = 0;
                for (Pawn q : defs) {
                    if (post < 2 && (q.role == Player.LB || q.role == Player.RB || q.role == Player.CB)) {
                        q.tx = gl + cdir * 0.8f; q.ty = CY + (post == 0 ? -GH + 0.5f : GH - 0.5f); post++;
                        q.planKind = -1; continue;
                    }
                    // mark nearest attacker target spot
                    Pawn tgt = null; float bd = 1e9f;
                    for (Pawn a : att) {
                        if (a.planKind == -2) continue;
                        float d = U.dist(a.tx, a.ty, q.ax, q.ay);
                        if (d < bd) { bd = d; tgt = a; }
                    }
                    if (tgt != null && di < 6) { q.tx = tgt.tx - cdir * 0.9f * 0 + cdir * 0.3f; q.ty = tgt.ty + (tgt.ty < CY ? 0.5f : -0.5f); tgt.planKind = -2; di++; }
                    else { q.tx = gl + cdir * 26; q.ty = q.ay; }
                }
                for (Pawn a : att) if (a.planKind == -2) a.planKind = 0;
                for (Pawn q : defs) if (q.planKind == -1) q.planKind = 0;
                break;
            }
            case SP_FREEKICK: {
                float gd = U.dist(spX, spY, gx, CY);
                // direction from ball to goal
                float dxg = gx - spX, dyg = CY - spY, dl = Math.max(1, U.len(dxg, dyg));
                float ux = dxg / dl, uy = dyg / dl;
                tk.tx = spX - ux * 1.8f; tk.ty = spY - uy * 1.8f;
                if (gd < 36) {
                    // wall
                    int wallN = gd < 22 ? 4 : 3;
                    ArrayList<Pawn> defs = new ArrayList<Pawn>();
                    for (Pawn p : od.pw) if (p.role != Player.GK) defs.add(p);
                    final float fx = spX, fy = spY;
                    java.util.Collections.sort(defs, new java.util.Comparator<Pawn>() {
                        public int compare(Pawn a, Pawn b) { return Float.compare(U.dist(a.x, a.y, fx, fy), U.dist(b.x, b.y, fx, fy)); }
                    });
                    float px = -uy, py = ux;
                    for (int i = 0; i < wallN && i < defs.size(); i++) {
                        Pawn q = defs.get(i);
                        float off = (i - (wallN - 1) / 2f) * 0.62f;
                        q.tx = spX + ux * 9.5f + px * off; q.ty = spY + uy * 9.5f + py * off;
                    }
                    // attackers into box
                    int k = 0;
                    for (Pawn p : sd.pw) {
                        if (p == tk || p.role == Player.GK) continue;
                        if (Player.group(p.role) >= 2 && k < 4) {
                            p.tx = gx - sd.dir * (7 + k * 3); p.ty = CY + (k % 2 == 0 ? -1 : 1) * (4 + k * 2.2f); k++;
                        }
                    }
                    // defenders mark box
                    int m = 0;
                    for (int i = wallN; i < defs.size(); i++) {
                        Pawn q = defs.get(i);
                        q.tx = gx - sd.dir * (6 + (m % 4) * 3); q.ty = CY + ((m % 2) * 2 - 1) * (3 + m * 1.8f); m++;
                        q.tx = gx + (od.dir > 0 ? -1 : 1) * (6 + (m % 4) * 3) * -1 * -1;
                        q.tx = gx - sd.dir * (6 + (m % 4) * 3);
                    }
                }
                break;
            }
            case SP_PENALTY: {
                tk.tx = spX - sd.dir * 3f; tk.ty = CY;
                float lineX = gx;
                for (Pawn p : all) {
                    if (p == tk) continue;
                    if (p.role == Player.GK && p.side != spSide) { p.tx = lineX - sd.dir * 0.4f; p.ty = CY; continue; }
                    if (p.role == Player.GK) { p.tx = ownGoalX(sd) + sd.dir * 3; p.ty = CY; continue; }
                    // outside box and arc
                    float edgeX = gx - sd.dir * (16.5f + 2.5f + (p.slot % 4) * 1.6f);
                    p.tx = edgeX; p.ty = CY + (p.slot - 5) * 3.4f;
                    if (p.side == spSide && Player.group(p.role) == 3) { p.tx = edgeX - sd.dir * 0f; }
                    if (p.side != spSide && Player.group(p.role) <= 1 && false) {}
                    // players not near the box go to the half line
                    if (depthOf(sd, p.ax) < L * 0.5f) { p.tx = xOfDepth(sd, L - 24 - (p.slot % 3) * 2); p.ty = CY + (p.slot - 5) * 4f; }
                }
                break;
            }
            default: break;
        }
        // keep keepers home on non-penalty restarts
        for (Pawn p : all) {
            if (p.role == Player.GK && spType != SP_PENALTY && spType != SP_GOALKICK) {
                float depth = 3f;
                if (spType == SP_CORNER && p.side != spSide) depth = 1.2f;
                if (spType == SP_FREEKICK && p.side != spSide) depth = 2f;
                p.tx = xOfDepth(S[p.side], depth); p.ty = CY;
                if (spType == SP_FREEKICK && p.side != spSide) {
                    float gdd = U.dist(spX, spY, goalX(sd), CY);
                    if (gdd < 36) p.ty = CY + (spY - CY) * 0.12f;
                }
            }
            p.tx = U.clamp(p.tx, -1, L + 1); p.ty = U.clamp(p.ty, -1, W + 1);
        }
        // anyone in a prohibited zone for the taker's team? keep players out of the ball's 9m for opp except wall rules
        if (spType != SP_PENALTY && spType != SP_FREEKICK) {
            for (Pawn p : od.pw) {
                float d = U.dist(p.tx, p.ty, spX, spY);
                if (d < 6f && p.role != Player.GK && !(spType == SP_CORNER)) {
                    float dx = p.tx - spX, dy = p.ty - spY, dl = Math.max(0.5f, U.len(dx, dy));
                    p.tx = spX + dx / dl * 6f; p.ty = spY + dy / dl * 6f;
                }
            }
        }
    }

    Pawn[] nearest(Side sd, float x, float y, int n, Pawn exclude) {
        Pawn[] res = new Pawn[n];
        float[] ds = new float[n];
        for (int i = 0; i < n; i++) ds[i] = 1e9f;
        for (Pawn p : sd.pw) {
            if (p == exclude || p.role == Player.GK || p.st == Pawn.SENT) continue;
            float d = U.dist(p.x, p.y, x, y);
            for (int i = 0; i < n; i++) {
                if (d < ds[i]) {
                    for (int j = n - 1; j > i; j--) { ds[j] = ds[j - 1]; res[j] = res[j - 1]; }
                    ds[i] = d; res[i] = p; break;
                }
            }
        }
        return res;
    }

    void markNearby(Side defenders, Side attackers, float dist) {
        for (Pawn p : defenders.pw) {
            if (p.role == Player.GK) continue;
            Pawn best = null; float bd = 1e9f;
            for (Pawn a : attackers.pw) {
                float d = U.dist(a.tx, a.ty, p.ax, p.ay);
                if (d < bd) { bd = d; best = a; }
            }
            if (best != null && bd < 14) { p.tx = best.tx + (ownGoalX(defenders) > best.tx ? 1 : -1) * dist * 0.5f; p.ty = best.ty + dist * 0.2f; }
        }
    }

    void updateSet(float dt) {
        clock += dt * clockSpeed;
        spTimer += dt;
        updateSideInfo(dt);
        Ctl.update(this, dt);
        for (Pawn p : all) {
            if (p.human) continue;
            if (p == spTaker && spType != SP_PENALTY || p == spTaker) {
                AI.goTo(this, p, p.tx, p.ty, 0.9f, false);
            } else AI.goTo(this, p, p.tx, p.ty, 0.9f, spTimer < 1.2f);
        }
        for (int i = 0; i < 22; i++) integrate(all[i], dt);
        separate();
        if (spType == SP_GOALKICK && spTaker.role == Player.GK) {}
        ballStepSet();
        boolean ready = spTimer > Math.min(spDelay, 1.2f) && U.dist(spTaker.x, spTaker.y, spTaker.tx, spTaker.ty) < 1.3f || spTimer > 5f;
        if (!ready) return;
        if (spHuman && !autoPlay) {
            Ctl.setPieceHuman(this, dt);
            if (spTimer > 14f) AI.executeSetPiece(this);
        } else if (spTimer >= spDelay) {
            AI.executeSetPiece(this);
        }
    }
    void ballStepSet() {
        if (mode != M_SET) return;
        // ball stays put until struck
        bx = spX; by = spY; bz = 0;
    }

    void finishSetPiece() {
        mode = M_PLAY; modeT = 0; spType = SP_NONE; spHuman = false;
        for (Pawn p : all) p.planKind = 0;
    }

    // ================= fouls / cards / offside
    void callOffside(Pawn p) {
        Side sd = S[p.side];
        sd.offsides++;
        banner("ОФСАЙД", 1.8f, 0);
        say("Офсайд! " + p.pl.last + " в положении вне игры");
        sounds.add(S_WHISTLE);
        arNear.flagT = 2.0f; arFar.flagT = 2.0f;
        for (Pawn q : all) q.offPos = false;
        float fx = U.clamp(p.x, 1, L - 1), fy = U.clamp(p.y, 1, W - 1);
        startSetPiece(SP_FREEKICK, 1 - p.side, fx, fy);
        spFoulSeverity = 0;
    }

    /** Resolve a tackle. returns true if the tackler wins the ball. */
    boolean tackle(Pawn d, Pawn a, boolean slide) {
        d.tackleCd = slide ? 1.6f : 1.1f;
        float dsk = d.pl.def * 0.6f + d.pl.phy * 0.2f + d.pl.pac * 0.2f;
        float ask = a.pl.dri * 0.6f + a.pl.phy * 0.2f + a.pl.pac * 0.2f;
        float lvl = 0;
        if (d.side != humanSide && !autoPlay) lvl = new float[]{-0.07f, 0f, 0.05f, 0.11f}[diff];
        if (a.side != humanSide && !autoPlay) lvl -= new float[]{-0.07f, 0f, 0.05f, 0.11f}[diff] * 0.6f;
        float pWin = U.clamp(0.36f + (dsk - ask) / 190f + (slide ? 0.08f : 0f) + lvl, 0.12f, 0.8f);
        boolean behind = (d.x - a.x) * a.fx + (d.y - a.y) * a.fy < -0.2f;
        float pFoul = (slide ? 0.40f : 0.19f) + (behind ? 0.32f : 0f) + (1 - pWin) * 0.16f;
        boolean win = U.chance(pWin);
        if (win) pFoul *= 0.35f;
        a.stat.rating += 0;
        d.stat.tackles++; S[d.side].tackles++;
        if (U.chance(pFoul)) {
            callFoul(d, a, slide, behind);
            return false;
        }
        if (win) {
            d.stat.tackleWon++; d.stat.rating += 0.18f; a.stat.rating -= 0.05f;
            sounds.add(S_TACKLE);
            // ball breaks free toward tackler's facing
            owner = null; prevOwner = a;
            float ang = (float) Math.atan2(d.fy, d.fx) + U.gauss() * 0.5f;
            float v = 3 + U.rnd(0, 3);
            bvx = (float) Math.cos(ang) * v; bvy = (float) Math.sin(ang) * v; bvz = 0;
            a.st = Pawn.STUMBLE; a.stT = 0; a.vx *= 0.3f; a.vy *= 0.3f;
            a.lock = 0.5f; d.lock = 0;
            lastTouch = d; lastTouchSide = d.side; lastKicker = d; lastKickT = time - 3;
            if (!slide && U.chance(0.6f)) takePossession(d);
            return true;
        } else {
            d.st = slide ? d.st : Pawn.STUMBLE; if (!slide) d.stT = 0;
            d.vx *= 0.4f; d.vy *= 0.4f;
            return false;
        }
    }

    void callFoul(Pawn d, Pawn a, boolean slide, boolean behind) {
        Side ds = S[d.side];
        ds.fouls++; d.stat.fouls++; d.stat.rating -= 0.15f;
        float sev = (slide ? 0.4f : 0.12f) + (behind ? 0.3f : 0.0f) + U.rnd(0, 0.35f);
        int card = 0;
        if (sev > 0.88f && U.chance(0.35f)) card = 2;
        else if (sev > 0.55f) card = 1;
        boolean pen = inBox(d.side, a.x, a.y) && inBoxFor(a) ;
        sounds.add(S_WHISTLE); sounds.add(S_FOUL);
        a.st = Pawn.FALL; a.stT = 0; a.vx *= 0.3f; a.vy *= 0.3f;
        say("Фол " + d.pl.last + " на " + a.pl.last);
        owner = null; bvx = bvy = 0;
        ref.armT = 1.6f;
        // injury
        if (U.chance(0.05f + sev * 0.04f)) injure(a, true);
        // card
        if (card > 0) giveCard(d, card, a);
        if (pen) {
            events.add(new Evt(minute(), E_PEN, 1 - d.side, a.pl, "Пенальти"));
            banner("ПЕНАЛЬТИ!", 2.5f, 1);
            say("Пенальти!!! " + d.pl.last + " нарушил правила в штрафной");
            startSetPiece(SP_PENALTY, 1 - d.side, goalX(S[1 - d.side]) + (S[1 - d.side].dir > 0 ? -11f : 11f), CY);
        } else {
            startSetPiece(SP_FREEKICK, 1 - d.side, U.clamp(a.x, 1, L - 1), U.clamp(a.y, 1, W - 1));
            spVictim = a;
        }
    }
    boolean inBoxFor(Pawn a) { return true; }

    void giveCard(Pawn d, int card, Pawn victim) {
        Side ds = S[d.side];
        d.stat.yel += card == 1 ? 1 : 0;
        if (card == 1 && d.stat.yel >= 2) card = 3; // second yellow
        ref.card = card == 1 ? 1 : 2; ref.cardT = 2.2f;
        ref.tx = d.x; ref.ty = d.y;
        sounds.add(S_CARD);
        if (card == 1) {
            ds.yellows++;
            events.add(new Evt(minute(), E_YEL, d.side, d.pl, d.pl.shortName()));
            banner("ЖЁЛТАЯ КАРТОЧКА: " + d.pl.last, 2.6f, 2);
            d.pl.yellows++;
        } else {
            ds.reds++; d.stat.red++;
            events.add(new Evt(minute(), E_RED, d.side, d.pl, d.pl.shortName() + (card == 3 ? " (2-я жёлтая)" : "")));
            banner("УДАЛЕНИЕ: " + d.pl.last, 3f, 3);
            sendOff(d);
        }
    }

    void sendOff(Pawn d) {
        d.st = Pawn.SENT; d.stT = 0;
        if (owner == d) owner = null;
        Side ds = S[d.side];
        // move off pitch
        d.x = d.x < L / 2 ? -4 : L + 4; d.y = -4;
        d.vx = d.vy = 0;
        if (ds.ctl == d) ds.ctl = null;
        d.human = false;
    }

    void injure(Pawn p, boolean fromFoul) {
        p.injured = true; p.stat.injured = true;
        p.st = Pawn.DOWN; p.stT = 0;
        events.add(new Evt(minute(), E_INJ, p.side, p.pl, p.pl.shortName()));
        banner("ТРАВМА: " + p.pl.last, 2.5f, 0);
        stoppage += 30;
        p.pl.injury = Math.max(p.pl.injury, U.ri(1, 6));
    }

    // ================= substitutions
    boolean substitute(int side, Pawn out, Player in) {
        Side sd = S[side];
        if (sd.subs >= 5 || out == null || !sd.bench.contains(in)) return false;
        if (out.st == Pawn.SENT) return false;
        Stat ns = statFor(sd, in);
        ns.played = true; ns.subbedOn = true; ns.minOn = minute();
        out.stat.subbedOff = true; out.stat.minOff = minute();
        int role = out.role;
        Pawn np = new Pawn(in, ns, side, out.slot, role);
        np.x = out.x; np.y = out.y; np.fx = out.fx; np.fy = out.fy; np.vx = 0; np.vy = 0;
        np.stamina = Math.max(0.7f, in.fitness / 100f); np.human = false;
        if (np.pl.pos != Player.GK && role == Player.GK) { /* emergency */ }
        sd.pw[out.slot] = np;
        all[side * 11 + out.slot] = np;
        sd.bench.remove(in);
        sd.bench.add(out.pl);
        sd.bench.remove(out.pl); // outgoing player cannot return
        sd.subs++;
        if (sd.ctl == out) sd.ctl = null;
        if (owner == out) owner = null;
        events.add(new Evt(minute(), E_SUB, side, in, in.shortName() + " ← " + out.pl.shortName()));
        banner("ЗАМЕНА: " + in.last, 2f, 4);
        return true;
    }

    // ================= officials
    void updateOfficials(float dt) {
        // referee follows play on a diagonal
        float bxx = bx, byy = by;
        float tx = bxx - 6 * (bxx > L / 2 ? 1 : -1) * 0.3f, ty = byy + (byy < CY ? 9 : -9);
        if (mode == M_SET || mode == M_KICKOFF) { tx = spX + 3; ty = spY + (spY < CY ? 8 : -8); if (mode == M_KICKOFF) { tx = L / 2 - 5; ty = CY + 8; } }
        if (spType == SP_PENALTY && mode == M_SET) { tx = spX - S[spSide].dir * 6; ty = CY + 14; }
        if (ref.cardT > 0) { tx = ref.tx - 1.5f; ty = ref.ty + 1.5f; ref.cardT -= dt; if (ref.cardT <= 0) ref.card = 0; }
        tx = U.clamp(tx, 3, L - 3); ty = U.clamp(ty, 3, W - 3);
        moveOfficial(ref, tx, ty, 6.2f, dt);
        ref.armT = Math.max(0, ref.armT - dt);
        // assistants follow offside lines
        float lineNearX = bx;
        for (Side sd : S) {
            // second last defender of the side that is defending the half where ball is
        }
        float trackX = U.clamp(secondLastX(bx > L / 2 ? 1 : -1), 2, L - 2);
        if (bx > L / 2) { moveOfficial(arNear, trackX, W + 1.8f, 7f, dt); moveOfficial(arFar, L / 2 + 6 * 0 + clampf(bx - 25, 8, 52), -1.8f, 7f, dt); }
        else { moveOfficial(arFar, trackX, -1.8f, 7f, dt); moveOfficial(arNear, clampf(bx + 25, 53, L - 8), W + 1.8f, 7f, dt); }
        arNear.flagT = Math.max(0, arNear.flagT - dt); arFar.flagT = Math.max(0, arFar.flagT - dt);
        arNear.fy = 0; arFar.fy = 0;
        // coaches pace
        for (int i = 0; i < 2; i++) {
            Official c = coaches[i];
            float cx = (i == 0 ? 36 : 70) + 2.5f * (float) Math.sin(time * 0.3f + i * 2);
            moveOfficial(c, cx, -3.4f, 1.6f, dt);
            c.fy = 0.6f; c.fx = 0.1f;
        }
    }
    static float clampf(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }

    float secondLastX(int goalEnd) {
        // x of second-last defender at the end of the pitch (goalEnd=+1 => right goal)
        Side def = attackerAt(goalEnd) == S[0] ? S[1] : S[0];
        float a = goalEnd > 0 ? -1e9f : 1e9f, b = a;
        for (Pawn p : def.pw) {
            if (p.st == Pawn.SENT) continue;
            if (goalEnd > 0) { if (p.x > a) { b = a; a = p.x; } else if (p.x > b) b = p.x; }
            else { if (p.x < a) { b = a; a = p.x; } else if (p.x < b) b = p.x; }
        }
        return b;
    }

    void moveOfficial(Official o, float tx, float ty, float speed, float dt) {
        float dx = tx - o.x, dy = ty - o.y, d = U.len(dx, dy);
        float vx = 0, vy = 0;
        if (d > 0.4f) { float s = Math.min(speed, d * 2f); vx = dx / d * s; vy = dy / d * s; }
        o.vx += (vx - o.vx) * Math.min(1, dt * 5); o.vy += (vy - o.vy) * Math.min(1, dt * 5);
        o.x += o.vx * dt; o.y += o.vy * dt;
        float sp = U.len(o.vx, o.vy);
        o.phase += sp * dt * 1.5f;
        if (o.type == Official.REF) {
            float lx = bx - o.x, ly = by - o.y, ll = Math.max(0.1f, U.len(lx, ly));
            o.fx += (lx / ll - o.fx) * Math.min(1, dt * 6); o.fy += (ly / ll - o.fy) * Math.min(1, dt * 6);
        } else if (sp > 0.5f) { o.fx = o.vx / sp; if (o.type != Official.COACH) o.fy = 0; }
    }

    // ================= messaging
    void banner(String s, float t, int kind) { banner = s; bannerT = t; bannerKind = kind; }
    void say(String s) { commentary = s; commT = 4.5f; }

    // ================= replay
    void recordReplay(float dt) {
        if (replayActive) return;
        repAcc += dt;
        if (repAcc < 1f / 30f) return;
        repAcc -= 1f / 30f;
        float[] f = repBuf[repHead];
        if (f == null) { f = new float[FRAME_LEN]; repBuf[repHead] = f; }
        fillFrame(f);
        repHead = (repHead + 1) % REP_FRAMES;
        if (repCount < REP_FRAMES) repCount++;
    }
    static final int FRAME_LEN = 8 + 22 * 9 + 5 * 7;

    float[] snapshot() { float[] f = new float[FRAME_LEN]; fillFrame(f); return f; }

    void fillFrame(float[] f) {
        int k = 0;
        f[k++] = bx; f[k++] = by; f[k++] = bz; f[k++] = brx; f[k++] = bry; f[k++] = ballTrail; f[k++] = owner != null ? 1 : 0; f[k++] = 0;
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            f[k++] = p.x; f[k++] = p.y; f[k++] = p.fx; f[k++] = p.fy; f[k++] = p.z; f[k++] = p.phase; f[k++] = p.st; f[k++] = p.stT; f[k++] = p.kickT;
        }
        for (int i = 0; i < 5; i++) {
            Official o = i < officials.size() ? officials.get(i) : null;
            if (o == null) { k += 7; continue; }
            f[k++] = o.x; f[k++] = o.y; f[k++] = o.fx; f[k++] = o.fy; f[k++] = o.phase; f[k++] = o.flagT; f[k++] = o.card;
        }
    }

    float[] replayFrame(int idxFromOldest) {
        int start = (repHead - repCount + REP_FRAMES) % REP_FRAMES;
        return repBuf[(start + idxFromOldest) % REP_FRAMES];
    }

    void applyFrame(float[] f) {
        if (f == null) return;
        int k = 0;
        bx = f[k++]; by = f[k++]; bz = f[k++]; brx = f[k++]; bry = f[k++]; ballTrail = f[k++]; k += 2;
        for (int i = 0; i < 22; i++) {
            Pawn p = all[i];
            p.x = f[k++]; p.y = f[k++]; p.fx = f[k++]; p.fy = f[k++]; p.z = f[k++]; p.phase = f[k++]; p.st = (int) f[k++]; p.stT = f[k++]; p.kickT = f[k++];
        }
        for (int i = 0; i < 5; i++) {
            Official o = i < officials.size() ? officials.get(i) : null;
            if (o == null) { k += 7; continue; }
            o.x = f[k++]; o.y = f[k++]; o.fx = f[k++]; o.fy = f[k++]; o.phase = f[k++]; o.flagT = f[k++]; o.card = (int) f[k++];
        }
    }

    // ================= shootout
    void startShootout() {
        mode = M_PEN; modeT = 0; shootout = true; penRound = 0; penSide = 0;
        S[0].penScore = S[1].penScore = 0; S[0].penTaken = S[1].penTaken = 0;
        S[0].dir = 1; S[1].dir = -1;
        Shootout.begin(this);
    }

    // ================= results
    int possessionPct(int side) {
        float a = S[0].possTime, b = S[1].possTime;
        if (a + b < 1) return 50;
        float pct = (side == 0 ? a : b) / (a + b) * 100;
        return Math.round(pct);
    }
}

/** Human input state. */
final class Input {
    float sx, sy;               // stick
    boolean sprint;
    boolean pass, lob, through, shoot;          // held
    boolean passP, lobP, throughP, shootP;      // pressed this frame
    boolean passR, lobR, throughR, shootR;      // released this frame
    float shootHeld, passHeld, lobHeld, throughHeld;
    boolean switchP;
    void clearEdges() { passP = lobP = throughP = shootP = false; passR = lobR = throughR = shootR = false; switchP = false; }
    boolean anyPress() { return passP || lobP || throughP || shootP; }
    float stickLen() { return U.len(sx, sy); }
}
