package com.yotta.fc;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/** League season, cup, transfers, finances, save/load. */
final class Career {
    static final int N_TEAMS = 16, MDS = 30;
    static final class Fix {
        int type; // 0 league 1 cup
        int round, h, a, hg, ag, pg = -1, pa = -1;
        boolean played;
        ArrayList<String> info = new ArrayList<String>();
    }
    static final class Offer { int pid; String from; float fee; }

    ArrayList<Team> teams = new ArrayList<Team>();
    HashMap<Integer, Player> byId = new HashMap<Integer, Player>();
    ArrayList<Player> pool = new ArrayList<Player>();
    ArrayList<Fix> fixtures = new ArrayList<Fix>();
    ArrayList<Fix> cup = new ArrayList<Fix>();
    ArrayList<int[]> calendar = new ArrayList<int[]>();
    ArrayList<Offer> offers = new ArrayList<Offer>();
    ArrayList<String> news = new ArrayList<String>();
    ArrayList<String> history = new ArrayList<String>();
    int user, season = 1, week;
    String manager = "Менеджер";
    int cupWinner = -1;
    float lastIncome, lastExpense;
    int titles, cups;
    boolean seasonOver;

    // ---------------------------------------------------------- creation
    static Career create(int userTeam, String manager) {
        Career c = new Career();
        c.user = userTeam; c.manager = manager;
        for (int i = 0; i < N_TEAMS; i++) {
            Team t = Data.makeClub(i);
            c.teams.add(t);
            for (Player p : t.squad) c.byId.put(p.id, p);
        }
        for (Player p : Data.genMarket(170)) { c.pool.add(p); c.byId.put(p.id, p); }
        for (Player p : Data.genFreeAgents(40)) { c.pool.add(p); c.byId.put(p.id, p); }
        Team ut = c.teams.get(userTeam);
        ut.cash = 30 + (80 - ut.rating) * 4;
        c.newSeasonSchedule();
        c.addNews("Добро пожаловать в " + ut.name + ", " + manager + "! Трансферное окно открыто.");
        return c;
    }

    Team user() { return teams.get(user); }

    void newSeasonSchedule() {
        fixtures.clear(); cup.clear(); calendar.clear(); week = 0; seasonOver = false; cupWinner = -1;
        for (Team t : teams) { t.resetTable(); t.cupOut = false; t.cupRound = 0; }
        int n = N_TEAMS;
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i;
        // shuffle order
        for (int i = n - 1; i > 0; i--) { int j = U.R.nextInt(i + 1); int t = arr[i]; arr[i] = arr[j]; arr[j] = t; }
        for (int r = 0; r < n - 1; r++) {
            for (int i = 0; i < n / 2; i++) {
                int a = arr[i], b = arr[n - 1 - i];
                Fix f1 = new Fix(); f1.type = 0; f1.round = r; f1.h = (r % 2 == 0) ? a : b; f1.a = (r % 2 == 0) ? b : a;
                Fix f2 = new Fix(); f2.type = 0; f2.round = r + n - 1; f2.h = f1.a; f2.a = f1.h;
                fixtures.add(f1); fixtures.add(f2);
            }
            int last = arr[n - 1];
            for (int i = n - 1; i > 1; i--) arr[i] = arr[i - 1];
            arr[1] = last;
        }
        for (int md = 0; md < MDS; md++) {
            calendar.add(new int[]{0, md});
            if (md == 3) calendar.add(new int[]{1, 0});
            if (md == 9) calendar.add(new int[]{1, 1});
            if (md == 17) calendar.add(new int[]{1, 2});
            if (md == 25) calendar.add(new int[]{1, 3});
        }
        // cup draw
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) ids.add(i);
        Collections.shuffle(ids, U.R);
        for (int i = 0; i < 8; i++) { Fix f = new Fix(); f.type = 1; f.round = 0; f.h = ids.get(i * 2); f.a = ids.get(i * 2 + 1); cup.add(f); }
        for (Player p : byId.values()) { p.apps = p.goals = p.assists = p.yel = p.red = p.cleanSheets = 0; p.ratingSum = 0; p.yellows = 0; }
    }

    int[] curWeek() { return week < calendar.size() ? calendar.get(week) : null; }

    String weekName() {
        int[] w = curWeek();
        if (w == null) return "Конец сезона";
        if (w[0] == 0) return "Тур " + (w[1] + 1) + " из " + MDS;
        String[] n = {"1/8 финала Кубка", "1/4 финала Кубка", "Полуфинал Кубка", "ФИНАЛ КУБКА"};
        return n[w[1]];
    }

    /** Fixture of the user this week (or null). */
    Fix userFix() {
        int[] w = curWeek();
        if (w == null) return null;
        if (w[0] == 0) {
            for (Fix f : fixtures) if (f.round == w[1] && (f.h == user || f.a == user)) return f;
        } else {
            for (Fix f : cup) if (f.round == w[1] && (f.h == user || f.a == user)) return f;
        }
        return null;
    }

    boolean windowOpen() {
        int[] w = curWeek();
        if (w == null) return true;
        if (w[0] == 0 && (w[1] <= 3 || (w[1] >= 14 && w[1] <= 17))) return true;
        return week <= 4 && season >= 1 && w[1] <= 3;
    }

    // ----------------------------------------------------------- standings
    ArrayList<Team> table() {
        ArrayList<Team> l = new ArrayList<Team>(teams);
        Collections.sort(l, new Comparator<Team>() {
            public int compare(Team a, Team b) {
                if (a.Pts != b.Pts) return b.Pts - a.Pts;
                if (a.gd() != b.gd()) return b.gd() - a.gd();
                if (a.GF != b.GF) return b.GF - a.GF;
                return a.id - b.id;
            }
        });
        return l;
    }

    int position(Team t) { return table().indexOf(t) + 1; }

    void applyResult(Fix f) {
        f.played = true;
        if (f.type != 0) return;
        Team h = teams.get(f.h), a = teams.get(f.a);
        h.P++; a.P++; h.GF += f.hg; h.GA += f.ag; a.GF += f.ag; a.GA += f.hg;
        if (f.hg > f.ag) { h.Wn++; a.L++; h.Pts += 3; h.form += "W"; a.form += "L"; }
        else if (f.hg < f.ag) { a.Wn++; h.L++; a.Pts += 3; h.form += "L"; a.form += "W"; }
        else { h.D++; a.D++; h.Pts++; a.Pts++; h.form += "D"; a.form += "D"; }
        if (h.form.length() > 5) h.form = h.form.substring(h.form.length() - 5);
        if (a.form.length() > 5) a.form = a.form.substring(a.form.length() - 5);
    }

    // ---------------------------------------------------------- quick sim
    float poisson(float lambda) {
        float L = (float) Math.exp(-lambda), p = 1; int k = 0;
        do { k++; p *= U.rnd(); } while (p > L);
        return k - 1;
    }

    void quickSim(Fix f) {
        Team h = teams.get(f.h), a = teams.get(f.a);
        h.repairLineup(); a.repairLineup();
        float sh = strength(h), sa = strength(a);
        float lh = 1.45f * (float) Math.exp((sh - sa) * 0.05f) * 1.08f, la = 1.12f * (float) Math.exp((sa - sh) * 0.05f);
        f.hg = (int) poisson(lh); f.ag = (int) poisson(la);
        f.played = true;
        if (f.type == 1 && f.hg == f.ag) {
            // penalties
            float ph = 0.5f + (sh - sa) * 0.01f;
            if (U.chance(ph)) { f.hg++; } else { f.ag++; }
            f.pg = 1; f.pa = 0; // marker (decided on penalties) handled in text
            f.info.add("по пенальти");
        }
        statsFor(h, f.hg, f.ag, f);
        statsFor(a, f.ag, f.hg, f);
        applyResult(f);
    }

    float strength(Team t) {
        float s = 0; int n = 0;
        for (int i = 0; i < 11; i++) { Player p = t.byId(t.lineup[i]); if (p != null) { s += p.ovr() + p.form * 0.5f; n++; } }
        return n == 0 ? 60 : s / n;
    }

    void statsFor(Team t, int gf, int ga, Fix f) {
        ArrayList<Player> xi = new ArrayList<Player>();
        for (int i = 0; i < 11; i++) { Player p = t.byId(t.lineup[i]); if (p != null) xi.add(p); }
        for (Player p : xi) {
            p.apps++; p.careerApps++; p.ratingSum += 6.0f + U.rnd(0, 1.3f) + (gf > ga ? 0.4f : (gf < ga ? -0.3f : 0));
            if (p.pos == Player.GK && ga == 0) p.cleanSheets++;
            p.fitness = Math.max(40, p.fitness - U.ri(8, 18));
            if (U.chance(0.005f)) { p.injury = U.ri(1, 5); }
            if (U.chance(0.08f)) { p.yel++; p.yellows++; if (p.yellows >= 5) { p.ban = 1; p.yellows = 0; } }
            if (U.chance(0.006f)) { p.red++; p.ban = U.ri(1, 3); }
        }
        for (int g = 0; g < gf; g++) {
            float tot = 0;
            for (Player p : xi) tot += scoreWeight(p);
            float r = U.rnd(0, tot), acc = 0;
            Player sc = xi.get(0);
            for (Player p : xi) { acc += scoreWeight(p); if (r <= acc) { sc = p; break; } }
            sc.goals++; sc.careerGoals++; sc.ratingSum += 0.8f;
            Player as = xi.get(U.R.nextInt(xi.size()));
            if (as != sc && as.pos != Player.GK && U.chance(0.7f)) { as.assists++; as.ratingSum += 0.4f; }
        }
    }

    float scoreWeight(Player p) {
        switch (p.pos) {
            case Player.ST: return 10 + p.sho * 0.1f; case Player.LW: case Player.RW: return 6; case Player.CAM: return 5;
            case Player.LM: case Player.RM: case Player.CM: return 3; case Player.CDM: return 1.2f; case Player.GK: return 0;
            default: return 0.8f;
        }
    }

    /** Copy the outcome of a live match into the career. */
    void applyLiveMatch(Match m, Fix f, int userSide) {
        f.hg = m.S[0].score; f.ag = m.S[1].score;
        if (m.shootout) { f.pg = m.S[0].penScore; f.pa = m.S[1].penScore; f.info.add("пен. " + f.pg + ":" + f.pa); }
        f.played = true;
        for (int s = 0; s < 2; s++) {
            Team t = teams.get(s == 0 ? f.h : f.a);
            for (Stat st : m.S[s].stats.values()) {
                if (!st.played) continue;
                Player p = st.pl;
                p.apps++; p.careerApps++; p.goals += st.goals; p.careerGoals += st.goals; p.assists += st.assists;
                p.yel += st.yel; p.red += st.red; p.ratingSum += st.rating;
                if (p.pos == Player.GK && m.S[1 - s].score == 0) p.cleanSheets++;
                p.fitness = Math.max(35, p.fitness - (st.subbedOn ? 8 : 16));
                if (st.yel > 0) { p.yellows++; if (p.yellows >= 5) { p.ban = 1; p.yellows = 0; } }
                if (st.red > 0) p.ban = Math.max(p.ban, 1 + (st.yel >= 2 ? 0 : U.ri(1, 2)));
            }
        }
        applyResult(f);
    }

    // ------------------------------------------------------ week advance
    /** Called after the user's fixture has been resolved (or when they have none). */
    void completeWeek() {
        int[] w = curWeek();
        if (w == null) return;
        ArrayList<Fix> list = w[0] == 0 ? fixtures : cup;
        for (Fix f : list) {
            if (f.round != w[1] || f.played) continue;
            if (f.h == user || f.a == user) { if (!f.played) { quickSim(f); } continue; }
            quickSim(f);
        }
        if (w[0] == 1) advanceCup(w[1]);
        // news
        Fix uf = null;
        for (Fix f : list) if (f.round == w[1] && (f.h == user || f.a == user)) uf = f;
        if (uf != null) {
            Team h = teams.get(uf.h), a = teams.get(uf.a);
            addNews(h.shortName + " " + uf.hg + ":" + uf.ag + " " + a.shortName + (uf.info.isEmpty() ? "" : " (" + uf.info.get(0) + ")"));
        }
        // finances
        Team ut = user();
        lastExpense = 0; lastIncome = 0;
        for (Team t : teams) {
            float wages = 0;
            for (Player p : t.squad) wages += p.wage / 1000f;
            float inc = 0.5f + (t.rating - 70) * 0.04f + t.capacity / 100000f * t.stadiumLvl * 0.4f;
            boolean home = false;
            for (Fix f : list) if (f.round == w[1] && f.h == t.id) home = true;
            if (home) inc += t.capacity * 0.85f * 48f / 1e6f * (1 + 0.12f * (t.stadiumLvl - 1));
            t.cash += inc - wages;
            if (t == ut) { lastIncome = inc; lastExpense = wages; }
        }
        // recovery
        for (Player p : byId.values()) {
            if (p.teamId < 0) continue;
            if (p.injury > 0) p.injury--;
            if (p.ban > 0) p.ban--;
            p.fitness = Math.min(100, p.fitness + 14 + teams.get(p.teamId).trainLvl * 2);
            p.form = U.clampi(p.form + (U.chance(0.3f) ? U.ri(-1, 1) : 0), -2, 2);
            if (p.injury == 0 && p.fitness < 60) p.fitness = 60;
        }
        for (Team t : teams) { if (t.id != user) t.autoLineup(); else t.repairLineup(); }
        week++;
        generateOffers();
        aiTransfers();
        int[] nw = curWeek();
        if (nw != null && nw[0] == 0 && nw[1] == 14) addNews("Зимнее трансферное окно открыто!");
        if (nw != null && nw[0] == 0 && nw[1] == 4 && w[0] == 0 && w[1] == 3) addNews("Летнее трансферное окно закрыто.");
        if (week >= calendar.size()) { seasonOver = true; endSeason(); }
    }

    void advanceCup(int round) {
        ArrayList<Fix> cur = new ArrayList<Fix>();
        for (Fix f : cup) if (f.round == round) cur.add(f);
        ArrayList<Integer> winners = new ArrayList<Integer>();
        for (Fix f : cur) {
            int w = f.hg > f.ag ? f.h : f.a;
            winners.add(w);
            int loser = w == f.h ? f.a : f.h;
            teams.get(loser).cupOut = true;
            teams.get(w).cupRound = round + 1;
        }
        if (round == 3) {
            cupWinner = winners.get(0);
            addNews("Обладатель Кубка: " + teams.get(cupWinner).name + "!");
            if (cupWinner == user) cups++;
            teams.get(cupWinner).cash += 8;
            return;
        }
        for (int i = 0; i < winners.size() / 2; i++) {
            Fix f = new Fix(); f.type = 1; f.round = round + 1; f.h = winners.get(i * 2); f.a = winners.get(i * 2 + 1);
            cup.add(f);
        }
    }

    // ------------------------------------------------------------- season
    void endSeason() {
        ArrayList<Team> tb = table();
        Team champ = tb.get(0);
        int pos = tb.indexOf(user()) + 1;
        String msg = "Сезон " + season + ": чемпион — " + champ.name + ". " + user().name + " — " + pos + " место.";
        history.add(msg);
        addNews(msg);
        if (champ.id == user) titles++;
        float[] prize = {40, 28, 20, 15, 12, 10, 8, 7, 6, 5, 4, 3.5f, 3, 2.5f, 2, 1.5f};
        for (int i = 0; i < tb.size(); i++) tb.get(i).cash += prize[i];
        // ageing
        ArrayList<Player> retire = new ArrayList<Player>();
        for (Player p : new ArrayList<Player>(byId.values())) {
            p.age++;
            int tl = p.teamId >= 0 ? teams.get(p.teamId).trainLvl : 1;
            if (p.age <= 23) { develop(p, U.ri(1, 4) + (tl > 2 ? 1 : 0), true); }
            else if (p.age <= 28) { develop(p, U.ri(-1, 2), true); }
            else if (p.age <= 31) { develop(p, U.ri(-2, 1), false); }
            else develop(p, -U.ri(1, 4), false);
            p.contract--;
            if (p.teamId >= 0 && p.contract <= 0) { p.contract = U.ri(2, 4); p.recalcValue(); }
            if (p.teamId < 0 && p.contract <= 0) p.contract = 1;
            p.fitness = 100; p.injury = 0; p.ban = 0; p.form = 0; p.yellows = 0;
            p.recalcValue();
            if (p.age >= 36 && U.chance(0.5f) || p.age >= 39) retire.add(p);
        }
        for (Player p : retire) {
            if (p.teamId >= 0) teams.get(p.teamId).squad.remove(p);
            pool.remove(p);
            byId.remove(p.id);
            if (p.teamId == user) addNews(p.fullName() + " завершил карьеру.");
        }
        // youth intake
        for (Team t : teams) {
            for (int i = 0; i < 2 + (t.id == user ? t.youthLvl - 1 : 0); i++) {
                int pos2 = Data.SQUAD_POS[U.R.nextInt(Data.SQUAD_POS.length)];
                Player y = Data.makeYouth(t, pos2, t.youthLvl);
                y.teamId = t.id; t.squad.add(y); byId.put(y.id, y);
                if (t.id == user) addNews("Из академии: " + y.shortName() + " (" + Player.POS[y.pos] + ", " + y.ovr() + "/" + y.pot + ")");
            }
            // trim
            while (t.squad.size() > 28) {
                Player w = weakest(t);
                t.squad.remove(w); w.teamId = -1; pool.add(w);
                if (t.id == user) addNews(w.fullName() + " отправлен в свободные агенты.");
            }
            Data.assignNumbers(t);
        }
        // refresh pool
        for (Player p : Data.genMarket(40)) { pool.add(p); byId.put(p.id, p); }
        for (Player p : Data.genFreeAgents(15)) { pool.add(p); byId.put(p.id, p); }
        for (Team t : teams) { t.rating = t.avgOvr(14); t.autoLineup(); }
        season++;
        seasonOverShown = true;
    }
    boolean seasonOverShown;

    void startNextSeason() {
        seasonOverShown = false;
        newSeasonSchedule();
        addNews("Новый сезон " + season + " начался! Трансферное окно открыто.");
        if (user().cash < 5) user().cash += 10;
    }

    void develop(Player p, int delta, boolean toward) {
        if (delta == 0) return;
        int[] a = {p.pac, p.sho, p.pas, p.dri, p.def, p.phy, p.sta};
        if (delta > 0 && p.ovr() >= p.pot) delta = Math.min(delta, 1) * (U.chance(0.4f) ? 1 : 0);
        for (int k = 0; k < Math.abs(delta); k++) {
            int i = U.R.nextInt(7);
            // bias by position weights
            float[] w = Player.W[p.pos];
            if (p.pos != Player.GK && i < 6 && w[i] < 0.08f && U.chance(0.6f)) i = U.R.nextInt(6);
            int step = delta > 0 ? 1 : -1;
            if (delta < 0 && (i == 0 || i == 5)) step = -2;
            a[i] = U.clampi(a[i] + step * U.ri(1, 2), 25, 97);
        }
        p.pac = a[0]; p.sho = a[1]; p.pas = a[2]; p.dri = a[3]; p.def = a[4]; p.phy = a[5]; p.sta = a[6];
        if (p.pos == Player.GK) p.gk = U.clampi(p.gk + (delta > 0 ? U.ri(0, delta + 1) : -U.ri(0, -delta)), 30, 97);
    }

    Player weakest(Team t) {
        Player w = null;
        for (Player p : t.squad) if (w == null || p.ovr() + (p.age < 22 ? 4 : 0) < w.ovr() + (w.age < 22 ? 4 : 0)) w = p;
        return w;
    }

    // ----------------------------------------------------------- transfers
    float askingPrice(Player p) {
        long seed = p.id * 31L + season * 17L;
        float r = 0.95f + ((seed * 2654435761L) & 1023) / 1023f * 0.25f;
        float imp = 1f;
        if (p.teamId >= 0) {
            Team t = teams.get(p.teamId);
            int rank = 0;
            for (Player q : t.squad) if (q.ovr() > p.ovr()) rank++;
            if (rank < 5) imp = 1.25f; else if (rank < 11) imp = 1.1f;
        }
        if (p.teamId == -1) return 0;
        return Math.round(p.value * r * imp * 100) / 100f;
    }

    /** 0 ok,1 rejected,2 no funds,3 window closed,4 squad full,5 own player */
    int lastResult; float lastAsk;
    int buy(Player p, float fee) {
        Team ut = user();
        if (p.teamId == user) return lastResult = 5;
        if (p.teamId != -1 && !windowOpen()) return lastResult = 3;
        if (ut.squad.size() >= 30) return lastResult = 4;
        float ask = askingPrice(p);
        lastAsk = ask;
        if (p.teamId == -1) fee = 0;
        if (fee > ut.cash) return lastResult = 2;
        if (fee < ask) return lastResult = 1;
        // execute
        if (p.teamId >= 0) { Team s = teams.get(p.teamId); s.squad.remove(p); s.cash += fee; fillSquad(s); }
        else if (p.teamId == -2) pool.remove(p);
        else pool.remove(p);
        ut.cash -= fee;
        p.teamId = user; p.extClub = ""; p.contract = U.ri(3, 5); p.number = 0; p.forSale = false;
        ut.squad.add(p);
        Data.assignNumbers(ut);
        ut.autoLineupIfBroken();
        addNews("Трансфер: " + p.fullName() + " (" + p.ovr() + ") — " + (fee > 0 ? U.money(fee) : "свободный агент"));
        return lastResult = 0;
    }

    void sell(Player p, float fee, String toName) {
        Team ut = user();
        ut.squad.remove(p);
        ut.cash += fee;
        p.teamId = -2; p.extClub = toName; p.forSale = false; pool.add(p);
        ut.autoLineupIfBroken();
        addNews("Продан: " + p.fullName() + " в " + toName + " за " + U.money(fee));
    }

    void release(Player p) {
        Team ut = user();
        if (ut.squad.size() <= 16) return;
        ut.squad.remove(p); p.teamId = -1; pool.add(p);
        ut.autoLineupIfBroken();
        addNews("Контракт расторгнут: " + p.fullName());
    }

    void fillSquad(Team t) {
        // AI signs best fit from pool
        while (t.squad.size() < 22 && !pool.isEmpty()) {
            int[] cnt = new int[12];
            for (Player p : t.squad) cnt[p.pos]++;
            int need = Player.CB;
            int[] want = {3, 4, 2, 2, 2, 3, 2, 1, 1, 1, 1, 3};
            for (int i = 0; i < 12; i++) if (cnt[i] < want[i]) { need = i; break; }
            Player best = null;
            for (Player p : pool) if (p.pos == need && (best == null || p.ovr() > best.ovr()) && p.value < t.cash + 5) best = p;
            if (best == null) { for (Player p : pool) if (best == null || p.ovr() > best.ovr()) best = p; }
            if (best == null) break;
            pool.remove(best); best.teamId = t.id; best.extClub = ""; t.squad.add(best);
            t.cash -= Math.min(best.value * 0.5f, t.cash * 0.3f);
        }
        Data.assignNumbers(t);
    }

    void aiTransfers() {
        int[] w = curWeek();
        boolean open = windowOpen();
        if (!open) return;
        for (int k = 0; k < 2; k++) {
            Team t = teams.get(U.R.nextInt(N_TEAMS));
            if (t.id == user || pool.isEmpty() || t.squad.size() >= 27) continue;
            Player cand = pool.get(U.R.nextInt(pool.size()));
            if (cand.value > t.cash * 0.5f) continue;
            // only if better than the weakest at pos
            Player weakest = null;
            for (Player p : t.squad) if (p.pos == cand.pos && (weakest == null || p.ovr() < weakest.ovr())) weakest = p;
            if (weakest != null && cand.ovr() <= weakest.ovr() + 1) continue;
            pool.remove(cand); cand.teamId = t.id; cand.extClub = ""; t.squad.add(cand); t.cash -= cand.value * 0.9f;
            if (weakest != null && t.squad.size() > 24) { t.squad.remove(weakest); weakest.teamId = -2; weakest.extClub = "Свободен"; pool.add(weakest); t.cash += weakest.value * 0.5f; }
            Data.assignNumbers(t);
            if (cand.ovr() >= 76) addNews(t.shortName + " подписал " + cand.fullName() + " (" + cand.ovr() + ")");
        }
    }

    void generateOffers() {
        Team ut = user();
        // offers for players listed for sale and random interest in stars
        if (!windowOpen()) { offers.clear(); return; }
        for (Player p : ut.squad) {
            float chance = p.forSale ? 0.45f : (p.ovr() >= 78 ? 0.05f : 0.015f);
            if (U.chance(chance)) {
                Offer o = new Offer(); o.pid = p.id; o.from = Data.EXT_CLUBS[U.R.nextInt(Data.EXT_CLUBS.length)];
                o.fee = Math.round(p.value * U.rnd(0.85f, 1.35f) * 100) / 100f;
                boolean dup = false;
                for (Offer x : offers) if (x.pid == p.id) dup = true;
                if (!dup) { offers.add(o); addNews("Предложение за " + p.shortName() + ": " + U.money(o.fee) + " от " + o.from); }
            }
        }
        for (int i = offers.size() - 1; i >= 0; i--) {
            Player p = byId.get(offers.get(i).pid);
            if (p == null || p.teamId != user || U.chance(0.25f)) offers.remove(i);
        }
    }

    void addNews(String s) {
        news.add(0, "С" + season + " • " + s);
        while (news.size() > 40) news.remove(news.size() - 1);
    }

    // upgrades
    float upgradeCost(int kind) {
        Team t = user();
        int lvl = kind == 0 ? t.stadiumLvl : (kind == 1 ? t.trainLvl : t.youthLvl);
        return 8 + lvl * 12;
    }
    boolean upgrade(int kind) {
        Team t = user();
        int lvl = kind == 0 ? t.stadiumLvl : (kind == 1 ? t.trainLvl : t.youthLvl);
        if (lvl >= 5) return false;
        float c = upgradeCost(kind);
        if (t.cash < c) return false;
        t.cash -= c;
        if (kind == 0) { t.stadiumLvl++; t.capacity = (int) (t.capacity * 1.12f); }
        else if (kind == 1) t.trainLvl++; else t.youthLvl++;
        return true;
    }

    // --------------------------------------------------------- top lists
    ArrayList<Player> topScorers(int n) {
        ArrayList<Player> l = new ArrayList<Player>();
        for (Team t : teams) for (Player p : t.squad) if (p.goals > 0) l.add(p);
        Collections.sort(l, new Comparator<Player>() { public int compare(Player a, Player b) { return b.goals != a.goals ? b.goals - a.goals : b.assists - a.assists; } });
        return new ArrayList<Player>(l.subList(0, Math.min(n, l.size())));
    }

    // ---------------------------------------------------------- save/load
    JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("user", user).put("season", season).put("week", week).put("mgr", manager).put("cw", cupWinner).put("titles", titles).put("cups", cups);
        o.put("nid", Data.nextId);
        JSONArray ta = new JSONArray(); for (Team t : teams) ta.put(t.toJson()); o.put("teams", ta);
        JSONArray pa = new JSONArray(); for (Player p : byId.values()) pa.put(p.toJson()); o.put("players", pa);
        JSONArray fa = new JSONArray();
        for (Fix f : fixtures) fa.put(fixJson(f)); o.put("fix", fa);
        JSONArray ca = new JSONArray();
        for (Fix f : cup) ca.put(fixJson(f)); o.put("cup", ca);
        JSONArray na = new JSONArray(); for (String s : news) na.put(s); o.put("news", na);
        JSONArray ha = new JSONArray(); for (String s : history) ha.put(s); o.put("hist", ha);
        JSONArray cal = new JSONArray(); for (int[] c : calendar) cal.put(new JSONArray(c)); o.put("cal", cal);
        JSONArray of = new JSONArray(); for (Offer f : offers) of.put(new JSONObject().put("p", f.pid).put("f", f.from).put("v", f.fee)); o.put("off", of);
        return o;
    }
    JSONObject fixJson(Fix f) throws Exception {
        JSONObject j = new JSONObject();
        j.put("t", f.type).put("r", f.round).put("h", f.h).put("a", f.a).put("hg", f.hg).put("ag", f.ag).put("pl", f.played).put("pg", f.pg).put("pa", f.pa);
        JSONArray i = new JSONArray(); for (String s : f.info) i.put(s); j.put("i", i);
        return j;
    }
    Fix fixFrom(JSONObject j) throws Exception {
        Fix f = new Fix();
        f.type = j.getInt("t"); f.round = j.getInt("r"); f.h = j.getInt("h"); f.a = j.getInt("a"); f.hg = j.getInt("hg"); f.ag = j.getInt("ag");
        f.played = j.getBoolean("pl"); f.pg = j.optInt("pg", -1); f.pa = j.optInt("pa", -1);
        JSONArray i = j.optJSONArray("i"); if (i != null) for (int k = 0; k < i.length(); k++) f.info.add(i.getString(k));
        return f;
    }

    static Career fromJson(JSONObject o) throws Exception {
        Career c = new Career();
        c.user = o.getInt("user"); c.season = o.getInt("season"); c.week = o.getInt("week"); c.manager = o.getString("mgr");
        c.cupWinner = o.optInt("cw", -1); c.titles = o.optInt("titles"); c.cups = o.optInt("cups");
        Data.nextId = o.getInt("nid");
        JSONArray pa = o.getJSONArray("players");
        for (int i = 0; i < pa.length(); i++) { Player p = Player.fromJson(pa.getJSONObject(i)); c.byId.put(p.id, p); }
        JSONArray ta = o.getJSONArray("teams");
        for (int i = 0; i < ta.length(); i++) {
            JSONObject to = ta.getJSONObject(i);
            Team t = Team.fromJson(to);
            JSONArray sq = to.getJSONArray("sq");
            for (int k = 0; k < sq.length(); k++) { Player p = c.byId.get(sq.getInt(k)); if (p != null) t.squad.add(p); }
            c.teams.add(t);
        }
        for (Player p : c.byId.values()) if (p.teamId < 0) c.pool.add(p);
        JSONArray fa = o.getJSONArray("fix"); for (int i = 0; i < fa.length(); i++) c.fixtures.add(c.fixFrom(fa.getJSONObject(i)));
        JSONArray ca = o.getJSONArray("cup"); for (int i = 0; i < ca.length(); i++) c.cup.add(c.fixFrom(ca.getJSONObject(i)));
        JSONArray na = o.getJSONArray("news"); for (int i = 0; i < na.length(); i++) c.news.add(na.getString(i));
        JSONArray ha = o.optJSONArray("hist"); if (ha != null) for (int i = 0; i < ha.length(); i++) c.history.add(ha.getString(i));
        JSONArray cal = o.getJSONArray("cal"); for (int i = 0; i < cal.length(); i++) { JSONArray r = cal.getJSONArray(i); c.calendar.add(new int[]{r.getInt(0), r.getInt(1)}); }
        JSONArray of = o.optJSONArray("off");
        if (of != null) for (int i = 0; i < of.length(); i++) { JSONObject j = of.getJSONObject(i); Offer f = new Offer(); f.pid = j.getInt("p"); f.from = j.getString("f"); f.fee = (float) j.getDouble("v"); c.offers.add(f); }
        return c;
    }
}
