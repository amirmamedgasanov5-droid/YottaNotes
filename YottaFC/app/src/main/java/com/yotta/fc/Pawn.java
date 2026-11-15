package com.yotta.fc;

/** A player on the pitch. */
final class Pawn {
    static final int N = 0, SLIDE = 1, FALL = 2, STUMBLE = 3, DIVE = 4, CELEB = 5, DOWN = 6, JUMP = 7, THROW = 8, GETUP = 9, SENT = 10, SIT = 11;

    Player pl;
    Stat stat;
    int side, slot, role;
    float x, y, vx, vy, fx = 1, fy = 0, z;
    float wvx, wvy;             // desired velocity
    boolean wantSprint;
    float stamina = 1f;
    int st;                     // state
    float stT;                  // time in state
    float phase;                // run cycle
    float kickT;                // kick animation timer
    float lock;                 // cannot take ball until 0
    float tackleCd;
    float tx, ty;               // movement target
    float ax, ay;               // formation anchor
    float thinkT;               // decision timer
    float diveDx, diveDy;       // dive direction
    float celebX, celebY;
    float h;                    // height in metres
    boolean offPos;             // was in offside position at last pass
    float offT;
    boolean human;
    float pressT;               // forced press time (human call)
    int planKind;               // carrier plan
    Pawn planTarget;
    float planX, planY;
    float aimYaw;               // keeper/shot misc
    float runT;                 // support-run timer
    float runX, runY;
    boolean injured;
    float fatigueDrain;

    Pawn(Player pl, Stat st, int side, int slot, int role) {
        this.pl = pl; this.stat = st; this.side = side; this.slot = slot; this.role = role;
        this.h = 1.60f + (pl.height - 160) * 0.0112f;
        stamina = Math.max(0.5f, pl.fitness / 100f);
    }

    float maxSpeed() {
        float s = 5.4f + (pl.pac - 40) * 0.0625f;
        if (role == Player.GK) s = Math.min(s, 6.6f);
        return Math.max(4.8f, s);
    }
    float speed() { return U.len(vx, vy); }
}

/** Per-match stats for one footballer. */
final class Stat {
    Player pl;
    int goals, assists, shots, sot, passes, passOK, tackles, tackleWon, fouls, yel, red, saves, inter, keyPass;
    float rating = 6.0f;
    boolean played, subbedOn, subbedOff, injured;
    int minOn, minOff = 999;
    Stat(Player p) { pl = p; }
}

/** Referee, assistant or coach. */
final class Official {
    static final int REF = 0, AR_NEAR = 1, AR_FAR = 2, COACH = 3;
    int type;
    float x, y, vx, vy, fx = 1, fy = 0, phase;
    float tx, ty;
    float flagT;       // flag raised timer
    float armT;        // pointing/whistle arm
    int card;          // 0 none 1 yellow 2 red
    float cardT;
    float h = 1.82f;
    int side = -1;     // coach team side
    Official(int type) { this.type = type; }
}
