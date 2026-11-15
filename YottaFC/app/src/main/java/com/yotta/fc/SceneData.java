package com.yotta.fc;

/** Snapshot of everything the 3D renderer needs for one frame (written on UI thread, read on GL thread). */
final class SceneData {
    long matchId;
    int tod;                       // 0 day 1 evening 2 night
    boolean rain;
    float time, excite;
    float[] eye = new float[3], target = new float[3];
    float fov = 0.56f;
    float aspect = 2.2f;
    float bx, by, bz, brx, bry;
    // pawns
    final float[] px = new float[22], py = new float[22], pz = new float[22], pfx = new float[22], pfy = new float[22],
            pphase = new float[22], pstT = new float[22], pkick = new float[22], pspeed = new float[22], ph = new float[22];
    final int[] pst = new int[22], prole = new int[22], pside = new int[22], pskin = new int[22], phs = new int[22], phc = new int[22],
            pbeard = new int[22], pboot = new int[22], pnum = new int[22];
    final boolean[] pvisible = new boolean[22];
    // kits
    final int[] c1 = new int[2], c2 = new int[2], c3 = new int[2], pat = new int[2], gk1 = new int[2], gk2 = new int[2];
    // officials
    int nOff;
    final float[] ox = new float[8], oy = new float[8], ofx = new float[8], ofy = new float[8], ophase = new float[8], oflag = new float[8], oarm = new float[8];
    final int[] otype = new int[8], ocard = new int[8], oside = new int[8];

    void capture(Match m, long id) {
        matchId = id;
        bx = m.bx; by = m.by; bz = m.bz; brx = m.brx; bry = m.bry;
        for (int s = 0; s < 2; s++) {
            Match.Side sd = m.S[s];
            c1[s] = sd.c1; c2[s] = sd.c2; c3[s] = sd.c3; pat[s] = sd.pattern; gk1[s] = sd.gk1; gk2[s] = sd.gk2;
        }
        for (int i = 0; i < 22; i++) {
            Pawn p = m.all[i];
            px[i] = p.x; py[i] = p.y; pz[i] = p.z; pfx[i] = p.fx; pfy[i] = p.fy; pphase[i] = p.phase; pstT[i] = p.stT; pkick[i] = p.kickT;
            pspeed[i] = p.speed(); ph[i] = p.h; pst[i] = p.st; prole[i] = p.role; pside[i] = p.side;
            pskin[i] = p.pl.skin; phs[i] = p.pl.hairStyle; phc[i] = p.pl.hairColor; pbeard[i] = p.pl.beard; pnum[i] = p.pl.number;
            pboot[i] = p.pl.id % 6;
            pvisible[i] = p.st != Pawn.SENT;
        }
        nOff = Math.min(8, m.officials.size());
        for (int i = 0; i < nOff; i++) {
            Official o = m.officials.get(i);
            ox[i] = o.x; oy[i] = o.y; ofx[i] = o.fx; ofy[i] = o.fy; ophase[i] = o.phase; oflag[i] = o.flagT; oarm[i] = o.armT;
            otype[i] = o.type; ocard[i] = o.card; oside[i] = o.side;
        }
    }
}
