package com.yotta.fc;

import org.json.JSONException;
import org.json.JSONObject;

/** Footballer data model. */
final class Player {
    static final int GK = 0, CB = 1, LB = 2, RB = 3, CDM = 4, CM = 5, CAM = 6, LM = 7, RM = 8, LW = 9, RW = 10, ST = 11;
    static final String[] POS = {"ВР", "ЦЗ", "ЛЗ", "ПЗ", "ОПЗ", "ЦП", "АПЗ", "ЛП", "ПП", "ЛФ", "ПФ", "ЦФ"};
    static final String[] POS_LONG = {"Вратарь", "Центр. защитник", "Левый защитник", "Правый защитник", "Опорный п/з",
            "Центр. п/з", "Атакующий п/з", "Левый п/з", "Правый п/з", "Левый вингер", "Правый вингер", "Нападающий"};
    // weights: pac sho pas dri def phy
    static final float[][] W = {
            {0, 0, 0, 0, 0, 0},
            {.10f, .00f, .10f, .05f, .50f, .25f},
            {.22f, .02f, .15f, .15f, .31f, .15f},
            {.22f, .02f, .15f, .15f, .31f, .15f},
            {.08f, .04f, .22f, .12f, .38f, .16f},
            {.10f, .10f, .30f, .22f, .18f, .10f},
            {.10f, .18f, .28f, .30f, .04f, .10f},
            {.24f, .12f, .20f, .28f, .06f, .10f},
            {.24f, .12f, .20f, .28f, .06f, .10f},
            {.27f, .18f, .14f, .31f, .02f, .08f},
            {.27f, .18f, .14f, .31f, .02f, .08f},
            {.16f, .40f, .08f, .18f, .02f, .16f}};
    // ideal attribute profile per position (pac sho pas dri def phy)
    static final int[][] PROFILE = {
            {45, 25, 50, 40, 25, 65}, {62, 38, 58, 52, 82, 80}, {80, 45, 68, 70, 74, 68}, {80, 45, 68, 70, 74, 68},
            {64, 56, 76, 68, 80, 78}, {70, 66, 80, 76, 66, 68}, {74, 76, 82, 82, 46, 58}, {82, 68, 74, 80, 52, 58},
            {82, 68, 74, 80, 52, 58}, {88, 76, 72, 84, 34, 56}, {88, 76, 72, 84, 34, 56}, {82, 86, 62, 78, 34, 76}};

    int id;
    String first, last;
    int nat, age, pos, pos2 = -1;
    int pac, sho, pas, dri, def, phy, gk, sta;
    int pot;
    int teamId = -1;       // >=0 league team, -1 free agent, -2 external club
    String extClub = "";
    int skin, hairStyle, hairColor, beard;
    int height;            // cm
    int number;
    float value, wage;     // M€, K€/week
    int contract = 3;
    int injury;            // weeks
    int ban;               // matches suspended
    int yellows;
    int fitness = 100;
    int form;              // -2..2
    int morale = 70;
    boolean forSale;
    // season stats
    int apps, goals, assists, yel, red, cleanSheets;
    float ratingSum;
    int careerGoals, careerApps;

    int ovr() {
        if (pos == GK) return clampOvr(gk * 0.92f + phy * 0.03f + pas * 0.02f + sta * 0.03f);
        float[] w = W[pos];
        return clampOvr(pac * w[0] + sho * w[1] + pas * w[2] + dri * w[3] + def * w[4] + phy * w[5]);
    }
    private static int clampOvr(float v) { return Math.max(1, Math.min(99, Math.round(v))); }

    /** overall when played at another position. */
    int ovrAt(int p) {
        if (p == pos || p == pos2) return ovr();
        int base;
        if (p == GK || pos == GK) return Math.max(15, ovr() - 55);
        float[] w = W[p];
        base = clampOvr(pac * w[0] + sho * w[1] + pas * w[2] + dri * w[3] + def * w[4] + phy * w[5]);
        int pen = group(p) == group(pos) ? 3 : 9;
        return Math.max(20, base - pen);
    }

    static int group(int p) {
        if (p == GK) return 0;
        if (p == CB || p == LB || p == RB) return 1;
        if (p == CDM || p == CM || p == CAM || p == LM || p == RM) return 2;
        return 3;
    }

    String shortName() {
        return first.length() > 0 ? first.charAt(0) + ". " + last : last;
    }
    String fullName() { return first + " " + last; }

    float avgRating() { return apps == 0 ? 0 : ratingSum / apps; }

    boolean available() { return injury <= 0 && ban <= 0; }

    void recalcValue() {
        int o = ovr();
        float v = (float) Math.exp(0.18 * (o - 60));
        if (age <= 21) v *= 1f + Math.max(0, pot - o) * 0.045f;
        else if (age <= 24) v *= 1f + Math.max(0, pot - o) * 0.02f;
        if (age >= 30) v *= 1f - (age - 29) * 0.09f;
        if (pos == GK) v *= 0.7f;
        v = Math.max(0.05f, v);
        value = Math.round(v * 100) / 100f;
        wage = Math.max(2f, Math.round((3f + 6f * (float) Math.pow(v, 0.85)) * 10) / 10f);
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id).put("f", first).put("l", last).put("n", nat).put("a", age).put("p", pos).put("p2", pos2);
        o.put("at", new org.json.JSONArray(new int[]{pac, sho, pas, dri, def, phy, gk, sta}));
        o.put("pot", pot).put("t", teamId).put("ec", extClub).put("sk", skin).put("hs", hairStyle).put("hc", hairColor);
        o.put("bd", beard).put("h", height).put("no", number).put("v", value).put("w", wage).put("ct", contract);
        o.put("inj", injury).put("ban", ban).put("yc", yellows).put("fit", fitness).put("fm", form).put("mo", morale);
        o.put("fs", forSale).put("ap", apps).put("g", goals).put("as", assists).put("y", yel).put("r", red);
        o.put("cs", cleanSheets).put("rs", ratingSum).put("cg", careerGoals).put("ca", careerApps);
        return o;
    }

    static Player fromJson(JSONObject o) throws JSONException {
        Player p = new Player();
        p.id = o.getInt("id"); p.first = o.getString("f"); p.last = o.getString("l"); p.nat = o.getInt("n");
        p.age = o.getInt("a"); p.pos = o.getInt("p"); p.pos2 = o.optInt("p2", -1);
        org.json.JSONArray a = o.getJSONArray("at");
        p.pac = a.getInt(0); p.sho = a.getInt(1); p.pas = a.getInt(2); p.dri = a.getInt(3);
        p.def = a.getInt(4); p.phy = a.getInt(5); p.gk = a.getInt(6); p.sta = a.getInt(7);
        p.pot = o.getInt("pot"); p.teamId = o.getInt("t"); p.extClub = o.optString("ec", "");
        p.skin = o.getInt("sk"); p.hairStyle = o.getInt("hs"); p.hairColor = o.getInt("hc"); p.beard = o.getInt("bd");
        p.height = o.getInt("h"); p.number = o.getInt("no"); p.value = (float) o.getDouble("v");
        p.wage = (float) o.getDouble("w"); p.contract = o.getInt("ct"); p.injury = o.getInt("inj");
        p.ban = o.getInt("ban"); p.yellows = o.getInt("yc"); p.fitness = o.getInt("fit"); p.form = o.getInt("fm");
        p.morale = o.optInt("mo", 70); p.forSale = o.optBoolean("fs");
        p.apps = o.getInt("ap"); p.goals = o.getInt("g"); p.assists = o.getInt("as"); p.yel = o.getInt("y");
        p.red = o.getInt("r"); p.cleanSheets = o.optInt("cs"); p.ratingSum = (float) o.getDouble("rs");
        p.careerGoals = o.optInt("cg"); p.careerApps = o.optInt("ca");
        return p;
    }
}
