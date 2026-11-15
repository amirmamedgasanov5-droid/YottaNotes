package com.yotta.fc;

import android.graphics.Canvas;
import java.util.ArrayList;

/** Application state machine: screens, match flow, input routing. */
final class App implements Ui.Listener {
    interface Host { void vibrate(int ms); void save(String name, String data); String load(String name); void quit(); }

    static final class Settings {
        int matchMin = 6, diff = 1, weather = 0, rain = 0, zoom = 1;
        boolean sound = true, vibr = true, radar = true, replays = true;
        float ctlScale = 1f, ctlOpacity = 1f;
        String serialize() { return matchMin + "," + diff + "," + weather + "," + rain + "," + zoom + "," + (sound ? 1 : 0) + "," + (vibr ? 1 : 0) + "," + (radar ? 1 : 0) + "," + (replays ? 1 : 0) + "," + ctlScale + "," + ctlOpacity; }
        void parse(String s) {
            try {
                String[] a = s.split(",");
                matchMin = Integer.parseInt(a[0]); diff = Integer.parseInt(a[1]); weather = Integer.parseInt(a[2]); rain = Integer.parseInt(a[3]); zoom = Integer.parseInt(a[4]);
                sound = a[5].equals("1"); vibr = a[6].equals("1"); radar = a[7].equals("1"); replays = a[8].equals("1");
                ctlScale = Float.parseFloat(a[9]); ctlOpacity = Float.parseFloat(a[10]);
            } catch (Exception e) { }
        }
    }

    // screens
    static final int S_MENU = 0, S_NEW = 1, S_HUB = 2, S_PREP = 3, S_MATCH = 4, S_RESULT = 5, S_QUICK = 6, S_SETTINGS = 7, S_ABOUT = 8;
    // hub tabs
    static final int T_HOME = 0, T_SQUAD = 1, T_TACT = 2, T_MARKET = 3, T_LEAGUE = 4, T_CLUB = 5;
    // dialogs
    static final int D_NONE = 0, D_PLAYER = 1, D_BID = 2, D_MSG = 3, D_SEASON = 4, D_CONFIRM = 5;

    // actions
    static final int A_CAREER = 1, A_CONTINUE = 2, A_QUICK = 3, A_PEN = 4, A_SETTINGS = 5, A_ABOUT = 6, A_BACK = 7, A_EXIT = 8,
            A_PICK = 10, A_START_CAREER = 11, A_TAB = 20, A_PLAY = 21, A_SIM = 22, A_NEXTWEEK = 23, A_SAVE = 24, A_QUITMENU = 25,
            A_PLAYER = 30, A_CLOSE = 31, A_SELL = 32, A_RELEASE = 33, A_CAPTAIN = 34, A_BUY = 35, A_BIDADJ = 36, A_BIDOK = 37, A_ACCEPT = 38, A_REJECT = 39,
            A_FORM = 40, A_SLOT = 41, A_BENCH = 42, A_STYLE = 43, A_AUTO = 44,
            A_FPOS = 50, A_FOVR = 51, A_FPRICE = 52, A_SORT = 53, A_SRC = 54, A_MTAB = 55, A_FAGE = 56,
            A_LTAB = 60, A_UPG = 61, A_PREPGO = 70,
            A_QHOME = 80, A_QAWAY = 81, A_QSIDE = 82, A_QSTART = 83,
            A_SET = 90,
            A_RESUME = 100, A_PSUBS = 101, A_PTACT = 102, A_PQUIT = 103, A_PQUITYES = 104, A_SUBOUT = 105, A_SUBIN = 106, A_SUBDO = 107, A_HALFGO = 108, A_RSKIP = 109,
            A_RESULTOK = 110, A_SEASONOK = 111, A_PSET = 112, A_CONFYES = 113, A_PBACK = 114;

    final Host host;
    final Settings set = new Settings();
    final Ui ui = new Ui();
    final Hud hud = new Hud(ui);
    final Renderer rend = new Renderer();
    final Sfx sfx = new Sfx();
    Career career;
    Match match;
    Screens scr;
    int screen = S_MENU, hubTab = T_HOME, dialog = D_NONE;
    int vw = 1920, vh = 1080;
    float time, accum;
    String msg = "";
    // ui state
    int selClub = 1; String managerName = "Менеджер";
    Player selPlayer; int selSlot = -1;
    int marketTab = 0, fPos = -1, fOvr = 60, fAge = 40, sortKey = 0, src = 0, leagueTab = 0;
    float fPrice = 60; float bidFee; String bidResult = ""; Player bidPlayer;
    int qHome = 1, qAway = 2, qSide = 0; boolean qPen;
    // match flow
    Career.Fix curFix; boolean careerMatch; int userSide; boolean cupMatch;
    boolean paused; int pauseTab; int subOut = -1; Player subIn; boolean quitConfirm;
    boolean replaying, replayDone; float repPos; int repStart, repEnd; float[] liveSnap; boolean wentHalf;
    boolean resultReady; float resultT;
    int confirmKind;
    int menuTab;
    Pawn passPreview;
    boolean sfxInit;
    int uiPtr = -1;

    App(Host host) {
        this.host = host;
        ui.listener = this;
        scr = new Screens(this);
        String s = host.load("settings");
        if (s != null) set.parse(s);
    }

    boolean hasSave() { String s = host.load("career"); return s != null && s.length() > 10; }

    void saveCareer() {
        if (career == null) return;
        try { host.save("career", career.toJson().toString()); } catch (Exception e) { }
    }
    void saveSettings() { host.save("settings", set.serialize()); applySettings(); }
    void applySettings() { sfx.enabled = set.sound; hud.scale = set.ctlScale; hud.opacity = set.ctlOpacity; hud.showRadar = set.radar; if (hud.vw > 0) hud.layout(hud.vw, hud.vh); }

    boolean loadCareer() {
        try {
            String s = host.load("career");
            if (s == null) return false;
            career = Career.fromJson(new org.json.JSONObject(s));
            return true;
        } catch (Exception e) { career = null; return false; }
    }

    // ================================================================ frame
    void frame(Canvas c, float dt, int w, int h) {
        time += dt;
        float s = h / 1080f;
        vw = Math.round(w / s); vh = 1080;
        c.save();
        c.scale(s, s);
        ui.begin(c, vw, vh, time, dt);
        if (!sfxInit) { sfxInit = true; applySettings(); }
        switch (screen) {
            case S_MATCH: matchFrame(c, dt); break;
            default: scr.draw(c, screen);
        }
        c.restore();
    }

    // ================================================================ match flow
    void startMatch(Team home, Team away, boolean homeHuman, boolean awayHuman, boolean cup, Career.Fix fix, boolean career, boolean shootoutOnly) {
        curFix = fix; careerMatch = career; cupMatch = cup;
        home.repairLineup(); away.repairLineup();
        match = new Match(home, away, homeHuman, awayHuman, set.diff, set.matchMin, cup, false);
        userSide = homeHuman ? 0 : 1;
        match.humanSide = userSide;
        int tod = set.weather == 0 ? (U.rnd() < 0.6f ? 0 : (U.rnd() < 0.5f ? 1 : 2)) : set.weather - 1;
        boolean rain = set.rain == 2 || (set.rain == 1 && U.chance(0.2f));
        match.weatherRain = rain ? 1 : 0;
        rend.setup(vw, vh, match, tod, rain);
        rend.camZoomMul = new float[]{0.95f, 1.15f, 1.4f}[set.zoom];
        rend.zoom = rend.camZoomMul;
        hud.layout(vw, vh);
        applySettings();
        paused = false; replaying = false; replayDone = false; resultReady = false; pauseTab = 0; quitConfirm = false; wentHalf = false;
        screen = S_MATCH;
        accum = 0;
        sfx.startAmbient();
        if (shootoutOnly) { match.period = 4; match.startShootout(); }
    }

    void matchFrame(Canvas c, float dt) {
        Match m = match;
        if (rend.vw != vw) { rend.vw = vw; rend.vh = vh; hud.layout(vw, vh); }
        // sounds
        for (int i = 0; i < m.sounds.size(); i++) {
            int id = m.sounds.get(i);
            float v = id == Match.S_GOAL ? 1f : (id == Match.S_KICK ? 0.55f : (id == Match.S_BOUNCE ? 0.3f : 0.8f));
            sfx.play(id, v);
            if (id == Match.S_GOAL && set.vibr) host.vibrate(250);
        }
        m.sounds.clear();
        // update
        boolean overlay = paused || (m.halfPending && (m.mode == Match.M_HALF || m.mode == Match.M_ETBREAK));
        if (!overlay && !replaying && m.mode != Match.M_END) {
            accum += Math.min(dt, 0.1f);
            int steps = 0;
            while (accum >= 1f / 60f && steps < 4) { m.update(1f / 60f); accum -= 1f / 60f; steps++; }
            if (steps == 4) accum = 0;
        }
        // goal replay
        if (!replaying && !replayDone && m.mode == Match.M_GOAL && m.modeT > 1.6f && set.replays && m.repCount > 60) startReplay();
        if (m.mode != Match.M_GOAL) replayDone = false;
        if (replaying) updateReplay(dt);
        // ambient crowd
        sfx.ambientLevel(rend.excite);
        // full time
        if (m.mode == Match.M_FULL && m.finished && !resultReady) {
            m.modeT += 0;
            if (m.modeT > 3.5f) { resultReady = true; sfx.stopAmbient(); sfx.play(Match.S_END, 1f); finishMatch(); }
        }
        // camera & draw
        rend.updateCamera(m, dt, replaying);
        rend.drawScene(c, m, time);
        // controlled markers
        rend.figs.marked = m.humanSide >= 0 && !m.autoPlay ? m.S[m.humanSide].ctl : null;
        passPreview = null;
        if (!replaying && !paused && m.mode == Match.M_PLAY && m.owner != null && m.owner.side == m.humanSide && m.owner.role != Player.GK) {
            Input in = m.in;
            passPreview = Ctl.pickTarget(m, m.owner, in.sx, in.sy, Match.K_GROUND);
        }
        rend.figs.target = passPreview;
        if (!replaying) hud.draw(c, m, dt, passPreview);
        else scr.replayOverlay(c);
        if (m.mode == Match.M_FULL && !resultReady) { }
        if (paused) scr.pause(c);
        else if (m.halfPending && (m.mode == Match.M_HALF || m.mode == Match.M_ETBREAK)) scr.halftime(c);
    }

    void startReplay() {
        Match m = match;
        replaying = true; m.replayActive = true;
        liveSnap = m.snapshot();
        repEnd = m.repCount - 1;
        repStart = Math.max(0, repEnd - 36 - 30 * 7);
        repPos = repStart;
        rend.camX = m.bx;
    }

    void updateReplay(float dt) {
        Match m = match;
        repPos += dt * 30f * 0.6f;
        if (repPos >= repEnd) { endReplay(); return; }
        m.applyFrame(m.replayFrame((int) repPos));
    }

    void endReplay() {
        Match m = match;
        m.applyFrame(liveSnap);
        m.replayActive = false;
        replaying = false; replayDone = true;
    }

    void finishMatch() {
        Match m = match;
        screen = S_RESULT;
        resultT = 0;
        if (careerMatch && career != null && curFix != null) {
            career.applyLiveMatch(m, curFix, userSide);
            career.completeWeek();
            saveCareer();
        }
    }

    /** Android back button. */
    void onBack() {
        if (screen == S_MATCH) {
            if (replaying) { endReplay(); return; }
            if (dialog == D_CONFIRM) { dialog = D_NONE; return; }
            if (paused && pauseTab != 0) { pauseTab = 0; return; }
            if (match != null && match.halfPending) return;
            paused = !paused; pauseTab = 0;
            if (paused) hud.releaseAll(match);
            return;
        }
        if (dialog != D_NONE) { dialog = D_NONE; return; }
        if (screen == S_MENU) { host.quit(); return; }
        if (screen == S_HUB) saveCareer();
        if (screen == S_PREP) { screen = S_HUB; return; }
        if (screen == S_RESULT) { afterResult(); return; }
        screen = S_MENU;
    }

    // ================================================================ input
    void touch(int action, int ptr, float rx, float ry, int w, int h) {
        float s = h / 1080f;
        float x = rx / s, y = ry / s;
        if (screen == S_MATCH && !paused && !replaying && !(match.halfPending && (match.mode == Match.M_HALF || match.mode == Match.M_ETBREAK))) {
            Match m = match;
            if (action == 0) {
                hud.down(ptr, x, y, m);
            } else if (action == 2) hud.move(ptr, x, y, m);
            else {
                boolean pause = hud.up(ptr, x, y, m);
                if (pause) { paused = true; pauseTab = 0; quitConfirm = false; hud.releaseAll(m); }
            }
            return;
        }
        if (screen == S_MATCH && replaying) { if (action == 1) endReplay(); return; }
        if (action == 0) { if (uiPtr != -1) return; uiPtr = ptr; }
        else if (ptr != uiPtr) return;
        if (action == 1) uiPtr = -1;
        ui.touch(action, x, y);
    }

    // ================================================================ actions
    @Override public void onAction(int id, int arg) {
        if (id < 0) return;
        sfx.play(Sfx.CLICK, 0.5f);
        if (id >= 1000 && match != null) {
            Match.Side sd = match.S[userSide];
            int base = id - 1000;
            if (base == A_FORM) { sd.formation = arg; int[] roles = Data.FORM_ROLE[arg]; for (int i = 0; i < 11; i++) sd.pw[i].role = roles[i]; }
            else if (base == A_STYLE) { int k = arg / 10, v = arg % 10; if (k == 0) sd.style = v; else if (k == 1) sd.press = v; else if (k == 2) sd.line = v; else if (k == 3) sd.width = v; else sd.tempo = v; }
            return;
        }
        if (id == A_TAB && arg >= 100) { screen = S_HUB; hubTab = arg - 100; return; }
        if (id == A_CLOSE && paused && dialog == D_CONFIRM) { dialog = D_NONE; return; }
        switch (id) {
            case A_CAREER: screen = S_NEW; break;
            case A_CONTINUE: if (loadCareer()) { screen = S_HUB; hubTab = T_HOME; } break;
            case A_QUICK: screen = S_QUICK; qPen = false; break;
            case A_PEN: screen = S_QUICK; qPen = true; break;
            case A_SETTINGS: screen = S_SETTINGS; break;
            case A_ABOUT: screen = S_ABOUT; break;
            case A_EXIT: host.quit(); break;
            case A_BACK:
                if (screen == S_HUB) { saveCareer(); }
                screen = S_MENU; dialog = D_NONE; break;
            case A_PICK: selClub = arg; break;
            case A_START_CAREER:
                career = Career.create(selClub, managerName);
                saveCareer(); screen = S_HUB; hubTab = T_HOME; dialog = D_NONE; break;
            case A_TAB: hubTab = arg; dialog = D_NONE; selSlot = -1; break;
            case A_PLAY: screen = S_PREP; break;
            case A_SIM: simulateWeek(); break;
            case A_NEXTWEEK: simulateWeek(); break;
            case A_SAVE: saveCareer(); showMsg("Игра сохранена"); break;
            case A_QUITMENU: saveCareer(); screen = S_MENU; break;
            case A_PLAYER: selPlayer = career.byId.get(arg); dialog = D_PLAYER; break;
            case A_CLOSE: dialog = D_NONE; break;
            case A_SELL: if (selPlayer != null) { selPlayer.forSale = !selPlayer.forSale; showMsg(selPlayer.forSale ? "Игрок выставлен на трансфер. Ждите предложения." : "Игрок снят с продажи"); } break;
            case A_RELEASE: if (selPlayer != null) { confirmKind = 1; dialog = D_CONFIRM; } break;
            case A_CONFYES:
                if (confirmKind == 1 && selPlayer != null) { career.release(selPlayer); dialog = D_NONE; }
                else if (confirmKind == 2) { dialog = D_NONE; sfx.stopAmbient(); screen = careerMatch ? S_HUB : S_MENU; paused = false; match = null; }
                break;
            case A_CAPTAIN: if (selPlayer != null) { career.user().captain = selPlayer.id; dialog = D_NONE; } break;
            case A_BUY: bidPlayer = career.byId.get(arg); bidFee = Math.max(0, career.askingPrice(bidPlayer)); bidResult = ""; dialog = D_BID; break;
            case A_BIDADJ: bidFee = Math.max(0, bidFee + arg / 10f); break;
            case A_BIDOK: doBid(); break;
            case A_ACCEPT: {
                Career.Offer o = career.offers.get(arg);
                Player p = career.byId.get(o.pid);
                if (p != null && p.teamId == career.user && career.user().squad.size() > 16) { career.sell(p, o.fee, o.from); career.offers.remove(arg); saveCareer(); }
                else showMsg("В составе должно остаться минимум 16 игроков");
                break;
            }
            case A_REJECT: career.offers.remove(arg); break;
            case A_FORM: career.user().formation = arg; career.user().autoLineup(); break;
            case A_SLOT: selSlot = (selSlot == arg) ? -1 : arg; break;
            case A_BENCH: tacticSwap(arg); break;
            case A_STYLE: { int k = arg / 10, v = arg % 10; Team t = career.user(); if (k == 0) t.style = v; else if (k == 1) t.press = v; else if (k == 2) t.line = v; else if (k == 3) t.width = v; else t.tempo = v; break; }
            case A_AUTO: career.user().autoLineup(); break;
            case A_FPOS: fPos = fPos >= 11 ? -1 : fPos + 1; break;
            case A_FOVR: fOvr = arg > 0 ? Math.min(85, fOvr + 3) : Math.max(40, fOvr - 3); break;
            case A_FPRICE: fPrice = arg > 0 ? Math.min(300, fPrice * 1.5f) : Math.max(1, fPrice / 1.5f); break;
            case A_FAGE: fAge = arg > 0 ? Math.min(40, fAge + 2) : Math.max(18, fAge - 2); break;
            case A_SORT: sortKey = (sortKey + 1) % 4; ui.scrollReset(300); break;
            case A_SRC: src = (src + 1) % 3; ui.scrollReset(300); break;
            case A_MTAB: marketTab = arg; break;
            case A_LTAB: leagueTab = arg; break;
            case A_UPG: if (!career.upgrade(arg)) showMsg("Недостаточно средств или максимальный уровень"); break;
            case A_PREPGO: startCareerMatch(); break;
            case A_QHOME: qHome = arg; break;
            case A_QAWAY: qAway = arg; break;
            case A_QSIDE: qSide = arg; break;
            case A_QSTART: startQuick(); break;
            case A_SET: applySetting(arg / 10, arg % 10); break;
            case A_RESUME: paused = false; break;
            case A_PSUBS: pauseTab = 1; subOut = -1; subIn = null; break;
            case A_PTACT: pauseTab = 2; break;
            case A_PSET: pauseTab = 3; break;
            case A_PBACK: pauseTab = 0; break;
            case A_PQUIT: confirmKind = 2; dialog = D_CONFIRM; break;
            case A_SUBOUT: subOut = arg; break;
            case A_SUBIN: subIn = career != null ? career.byId.get(arg) : findPlayer(arg); break;
            case A_SUBDO: doSub(); break;
            case A_HALFGO: match.continueFromBreak(); sfx.play(Match.S_WHISTLE, 0.8f); break;
            case A_RSKIP: if (replaying) endReplay(); break;
            case A_RESULTOK: afterResult(); break;
            case A_SEASONOK: career.startNextSeason(); dialog = D_NONE; saveCareer(); break;
            default: break;
        }
    }

    Player findPlayer(int id) {
        if (match == null) return null;
        for (Match.Side s : match.S) for (Player p : s.stats.keySet()) if (p.id == id) return p;
        for (Match.Side s : match.S) for (Player p : s.bench) if (p.id == id) return p;
        return null;
    }

    void showMsg(String s) { msg = s; dialog = D_MSG; }

    void applySetting(int k, int v) {
        switch (k) {
            case 0: set.matchMin = new int[]{3, 5, 6, 8, 10}[v]; break;
            case 1: set.diff = v; break;
            case 2: set.weather = v; break;
            case 3: set.rain = v; break;
            case 4: set.zoom = v; break;
            case 5: set.sound = v == 1; break;
            case 6: set.vibr = v == 1; break;
            case 7: set.radar = v == 1; break;
            case 8: set.replays = v == 1; break;
            case 9: set.ctlScale = new float[]{0.85f, 1f, 1.15f, 1.3f}[v]; break;
            case 10: set.ctlOpacity = new float[]{0.5f, 0.75f, 1f}[v]; break;
            default: break;
        }
        saveSettings();
        if (match != null && screen == S_MATCH) { rend.camZoomMul = new float[]{0.95f, 1.15f, 1.4f}[set.zoom]; }
    }

    void doBid() {
        if (bidPlayer == null) return;
        int r = career.buy(bidPlayer, bidFee);
        switch (r) {
            case 0: bidResult = "✔ Сделка закрыта! " + bidPlayer.fullName() + " теперь в вашем клубе."; saveCareer(); break;
            case 1: bidResult = "✘ Клуб отклонил предложение. Они хотят около " + U.money(career.lastAsk) + "."; break;
            case 2: bidResult = "✘ Недостаточно средств."; break;
            case 3: bidResult = "✘ Трансферное окно закрыто."; break;
            case 4: bidResult = "✘ В составе уже 30 игроков."; break;
            default: bidResult = "✘ Сделка невозможна.";
        }
    }

    void tacticSwap(int pid) {
        Team t = career.user();
        if (selSlot < 0) { // pick player to put into first matching slot? just select
            return;
        }
        Player in = career.byId.get(pid);
        if (in == null || !in.available()) { showMsg("Игрок недоступен (травма/дисквалификация)"); return; }
        int out = t.lineup[selSlot];
        // if "in" is in lineup elsewhere swap them
        for (int i = 0; i < 11; i++) if (t.lineup[i] == pid) { t.lineup[i] = out; t.lineup[selSlot] = pid; selSlot = -1; return; }
        for (int i = 0; i < 7; i++) if (t.bench[i] == pid) { t.bench[i] = out; }
        t.lineup[selSlot] = pid;
        // ensure not in bench twice
        selSlot = -1;
    }

    void simulateWeek() {
        Career.Fix f = career.userFix();
        if (f != null && !f.played) career.quickSim(f);
        career.completeWeek();
        saveCareer();
        if (career.seasonOverShown) { dialog = D_SEASON; }
    }

    void startCareerMatch() {
        Career.Fix f = career.userFix();
        if (f == null) { screen = S_HUB; return; }
        Team h = career.teams.get(f.h), a = career.teams.get(f.a);
        for (Team t : career.teams) if (t.id != career.user) t.autoLineup();
        career.user().repairLineup();
        startMatch(h, a, f.h == career.user, f.a == career.user, f.type == 1, f, true, false);
    }

    void startQuick() {
        if (qHome == qAway) return;
        Career tmp = null;
        Team h = quickTeam(qHome), a = quickTeam(qAway);
        startMatch(h, a, qSide == 0, qSide == 1, qPen, null, false, qPen);
        if (qPen) { match.cup = true; }
    }

    ArrayList<Team> quickTeams;
    Team quickTeam(int i) {
        if (quickTeams == null) { quickTeams = new ArrayList<Team>(); for (int k = 0; k < 16; k++) quickTeams.add(Data.makeClub(k)); }
        return quickTeams.get(i);
    }

    void doSub() {
        if (match == null || subOut < 0 || subIn == null) return;
        Match.Side sd = match.S[userSide];
        if (sd.subs >= 5) { showMsg("Лимит замен (5) исчерпан"); return; }
        if (match.substitute(userSide, sd.pw[subOut], subIn)) { subOut = -1; subIn = null; sfx.play(Match.S_CARD, 0.4f); }
    }

    void afterResult() {
        if (careerMatch && career != null) {
            screen = S_HUB; hubTab = T_HOME;
            if (career.seasonOverShown) dialog = D_SEASON; else dialog = D_NONE;
        } else screen = S_MENU;
        match = null; resultReady = false;
    }
}
