package com.yotta.fc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

/** A club. */
final class Team {
    int id;
    String name, shortName, city, stadium;
    int c1, c2, c3;
    int pattern, crestShape, crestSym;
    int rating;
    int capacity;
    float cash;
    int stadiumLvl = 1, trainLvl = 1, youthLvl = 1;
    ArrayList<Player> squad = new ArrayList<Player>();
    int formation = 1;
    int[] lineup = new int[11];     // player ids
    int[] bench = new int[7];
    int style = 1, press = 1, line = 1, width = 1, tempo = 1; // 0..2
    int captain = -1;
    // table
    int P, Wn, D, L, GF, GA, Pts;
    String form = "";
    int cupRound = 0; boolean cupOut;

    int gd() { return GF - GA; }

    Player byId(int id) {
        for (Player p : squad) if (p.id == id) return p;
        return null;
    }

    int avgOvr(int top) {
        ArrayList<Integer> o = new ArrayList<Integer>();
        for (Player p : squad) o.add(p.ovr());
        java.util.Collections.sort(o, java.util.Collections.reverseOrder());
        int s = 0, n = Math.min(top, o.size());
        for (int i = 0; i < n; i++) s += o.get(i);
        return n == 0 ? 50 : s / n;
    }

    /** Pick best XI for the formation honoring availability. */
    void autoLineup() {
        int[] roles = Data.FORM_ROLE[formation];
        boolean[] used = new boolean[squad.size()];
        int[] pick = new int[11];
        // goalkeeper first, then by scarcity
        int[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int oi = 0; oi < 11; oi++) {
            int s = order[oi];
            int best = -1, bo = -1;
            for (int i = 0; i < squad.size(); i++) {
                Player p = squad.get(i);
                if (used[i] || !p.available()) continue;
                if ((roles[s] == Player.GK) != (p.pos == Player.GK)) continue;
                int o = p.ovrAt(roles[s]) + (p.fitness < 60 ? -4 : 0) + p.form;
                if (o > bo) { bo = o; best = i; }
            }
            if (best < 0) { // fallback: anyone
                for (int i = 0; i < squad.size(); i++) if (!used[i]) { best = i; break; }
            }
            if (best >= 0) { used[best] = true; pick[s] = squad.get(best).id; }
        }
        System.arraycopy(pick, 0, lineup, 0, 11);
        // bench: best remaining, ensure a GK
        int bi = 0;
        boolean hasGk = false;
        for (int i = 0; i < squad.size() && !hasGk; i++) {
            Player p = squad.get(i);
            if (!used[i] && p.pos == Player.GK && p.available()) { bench[bi++] = p.id; used[i] = true; hasGk = true; }
        }
        while (bi < 7) {
            int best = -1, bo = -1;
            for (int i = 0; i < squad.size(); i++) {
                Player p = squad.get(i);
                if (used[i] || !p.available()) continue;
                if (p.ovr() > bo) { bo = p.ovr(); best = i; }
            }
            if (best < 0) { bench[bi++] = -1; continue; }
            used[best] = true; bench[bi++] = squad.get(best).id;
        }
        captain = -1;
        int bo = -1;
        for (int i = 0; i < 11; i++) {
            Player p = byId(lineup[i]);
            if (p != null && p.age >= 24 && p.ovr() + p.age / 4 > bo) { bo = p.ovr() + p.age / 4; captain = p.id; }
        }
    }

    boolean lineupValid() {
        for (int i = 0; i < 11; i++) {
            Player p = byId(lineup[i]);
            if (p == null || !p.available()) return false;
        }
        return true;
    }

    /** Fix lineup holes caused by injuries/transfers using auto-pick for those slots. */
    void repairLineup() {
        boolean ok = lineupValid();
        for (int i = 0; i < 7 && ok; i++) if (bench[i] >= 0 && byId(bench[i]) == null) ok = false;
        if (!ok) autoLineup();
    }

    void autoLineupIfBroken() { if (!lineupValid()) autoLineup(); else repairLineup(); }

    void resetTable() { P = Wn = D = L = GF = GA = Pts = 0; form = ""; }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id).put("nm", name).put("sn", shortName).put("ci", city).put("st", stadium);
        o.put("c1", c1).put("c2", c2).put("c3", c3).put("pt", pattern).put("cs", crestShape).put("cy", crestSym);
        o.put("rt", rating).put("cap", capacity).put("cash", cash).put("sl", stadiumLvl).put("tl", trainLvl).put("yl", youthLvl);
        o.put("fm", formation).put("sty", style).put("pr", press).put("ln", line).put("wd", width).put("tp", tempo);
        o.put("cp", captain);
        o.put("lu", new JSONArray(lineup)).put("bn", new JSONArray(bench));
        o.put("P", P).put("W", Wn).put("D", D).put("L", L).put("GF", GF).put("GA", GA).put("Pts", Pts).put("frm", form);
        o.put("cr", cupRound).put("co", cupOut);
        JSONArray a = new JSONArray();
        for (Player p : squad) a.put(p.id);
        o.put("sq", a);
        return o;
    }

    static Team fromJson(JSONObject o) throws JSONException {
        Team t = new Team();
        t.id = o.getInt("id"); t.name = o.getString("nm"); t.shortName = o.getString("sn"); t.city = o.getString("ci");
        t.stadium = o.getString("st"); t.c1 = o.getInt("c1"); t.c2 = o.getInt("c2"); t.c3 = o.getInt("c3");
        t.pattern = o.getInt("pt"); t.crestShape = o.getInt("cs"); t.crestSym = o.getInt("cy");
        t.rating = o.getInt("rt"); t.capacity = o.getInt("cap"); t.cash = (float) o.getDouble("cash");
        t.stadiumLvl = o.getInt("sl"); t.trainLvl = o.getInt("tl"); t.youthLvl = o.getInt("yl");
        t.formation = o.getInt("fm"); t.style = o.getInt("sty"); t.press = o.getInt("pr"); t.line = o.getInt("ln");
        t.width = o.getInt("wd"); t.tempo = o.getInt("tp"); t.captain = o.getInt("cp");
        JSONArray lu = o.getJSONArray("lu"), bn = o.getJSONArray("bn");
        for (int i = 0; i < 11; i++) t.lineup[i] = lu.getInt(i);
        for (int i = 0; i < 7; i++) t.bench[i] = bn.getInt(i);
        t.P = o.getInt("P"); t.Wn = o.getInt("W"); t.D = o.getInt("D"); t.L = o.getInt("L");
        t.GF = o.getInt("GF"); t.GA = o.getInt("GA"); t.Pts = o.getInt("Pts"); t.form = o.optString("frm", "");
        t.cupRound = o.optInt("cr"); t.cupOut = o.optBoolean("co");
        return t;
    }
}
