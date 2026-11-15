package com.yotta.fc;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** All menu / overlay screens. */
final class Screens {
    final App a;
    final Ui ui;
    Canvas c;
    int vw, vh;
    float cx0 = 300;
    ArrayList<Player> mcache = new ArrayList<Player>();
    String mkey = "";

    Screens(App app) { a = app; ui = app.ui; }

    void draw(Canvas canvas, int screen) {
        c = canvas; vw = ui.vw; vh = ui.vh;
        ui.bg();
        switch (screen) {
            case App.S_MENU: menu(); break;
            case App.S_NEW: newCareer(); break;
            case App.S_HUB: hub(); break;
            case App.S_PREP: prep(); break;
            case App.S_RESULT: result(); break;
            case App.S_QUICK: quick(); break;
            case App.S_SETTINGS: settings(); break;
            case App.S_ABOUT: about(); break;
            default: break;
        }
        if (screen == App.S_HUB && a.dialog != App.D_NONE) dialogs();
    }

    void title(String s, String sub) {
        ui.text(s, 60, 90, 62, Ui.WHITE, 0);
        if (sub != null) ui.text(sub, 62, 128, 26, Ui.GRAY, 0, false);
    }

    void backBtn() { ui.button(vw - 250, 36, 210, 70, "← НАЗАД", App.A_BACK, 0, Ui.ST_SECOND); }

    // ================================================================ MENU
    void menu() {
        float t = a.time;
        // light beams
        for (int i = 0; i < 4; i++) {
            float bx = vw * (0.15f + i * 0.28f), sw = (float) Math.sin(t * 0.4f + i * 1.7f) * 160;
            ui.fill.setShader(new LinearGradient(bx, 0, bx + sw, vh, 0x2AFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            ui.path.reset(); ui.path.moveTo(bx - 20, 0); ui.path.lineTo(bx + 20, 0); ui.path.lineTo(bx + sw + 220, vh); ui.path.lineTo(bx + sw - 220, vh); ui.path.close();
            c.drawPath(ui.path, ui.fill);
        }
        ui.fill.setShader(null);
        // pitch glow at bottom
        ui.fill.setShader(new LinearGradient(0, vh * 0.7f, 0, vh, 0x00228B3A, 0xAA1C7A32, Shader.TileMode.CLAMP));
        c.drawRect(0, vh * 0.7f, vw, vh, ui.fill);
        ui.fill.setShader(null);
        // marquee of crests
        for (int i = 0; i < 16; i++) {
            float x = ((i * 190 + t * 40) % (16 * 190)) - 190;
            Team tm = a.quickTeam(i);
            ui.crest(tm, x + 60, vh - 90 + (float) Math.sin(t * 2 + i) * 6, 46);
        }
        // title
        float tx = 90;
        ui.text("YOTTA", tx, 300, 170, Ui.WHITE, 0);
        ui.text("FC 26", tx + 6, 450, 190, Ui.ACCENT, 0);
        ui.text("ULTIMATE FOOTBALL", tx + 10, 505, 40, Ui.CYAN, 0);
        // spinning ball
        float bx = 560, by = 640, br = 80 + (float) Math.sin(t * 3) * 3;
        ui.fill.setColor(0x44000000); ui.rc.set(bx - br, by + br * 1.0f, bx + br, by + br * 1.25f); c.drawOval(ui.rc, ui.fill);
        ui.fill.setShader(new RadialGradient(bx - br * 0.3f, by - br * 0.35f, br * 1.5f, 0xFFFFFFFF, 0xFFA7ADBB, Shader.TileMode.CLAMP));
        c.drawCircle(bx, by, br, ui.fill); ui.fill.setShader(null);
        ui.fill.setColor(0xFF22262E);
        for (int i = 0; i < 6; i++) {
            float ang = t * 1.4f + i * 1.0472f;
            float px = bx + (float) Math.cos(ang) * br * 0.62f, py = by + (float) Math.sin(ang * 0.8f) * br * 0.55f;
            float k = 0.5f + 0.5f * (float) Math.cos(ang);
            ui.rc.set(px - br * 0.24f * k, py - br * 0.24f, px + br * 0.24f * k, py + br * 0.24f); c.drawOval(ui.rc, ui.fill);
        }
        // buttons
        float x = vw - 700, y = 130, w = 600, h = 104, gap = 20;
        boolean save = a.hasSave();
        if (save) { ui.button(x, y, w, h, "ПРОДОЛЖИТЬ КАРЬЕРУ", App.A_CONTINUE, 0, Ui.ST_PRIMARY, 40); y += h + gap; }
        ui.button(x, y, w, h, save ? "НОВАЯ КАРЬЕРА" : "КАРЬЕРА", App.A_CAREER, 0, save ? Ui.ST_SECOND : Ui.ST_PRIMARY, 40); y += h + gap;
        ui.button(x, y, w, h, "БЫСТРЫЙ МАТЧ", App.A_QUICK, 0, Ui.ST_SECOND, 40); y += h + gap;
        ui.button(x, y, w, h, "СЕРИЯ ПЕНАЛЬТИ", App.A_PEN, 0, Ui.ST_SECOND, 40); y += h + gap;
        ui.button(x, y, w / 2 - 10, 90, "НАСТРОЙКИ", App.A_SETTINGS, 0, Ui.ST_GHOST, 32);
        ui.button(x + w / 2 + 10, y, w / 2 - 10, 90, "ОБ ИГРЕ", App.A_ABOUT, 0, Ui.ST_GHOST, 32);
        ui.text("v1.0 • Нативный Android", 90, 560, 22, Ui.GRAY, 0, false);
    }

    // ================================================================ NEW CAREER
    void newCareer() {
        title("НОВАЯ КАРЬЕРА", "Выберите клуб, который приведёте к славе");
        backBtn();
        float gx = 60, gy = 160, cw = 300, ch = 190, gap = 16;
        for (int i = 0; i < 16; i++) {
            int col = i % 4, row = i / 4;
            float x = gx + col * (cw + gap), y = gy + row * (ch + gap);
            Team t = a.quickTeam(i);
            boolean sel = a.selClub == i;
            ui.glass(x, y, cw, ch, 22);
            if (sel) { ui.stroke.setColor(Ui.GOLD); ui.stroke.setStrokeWidth(5); ui.rc.set(x, y, x + cw, y + ch); c.drawRoundRect(ui.rc, 22, 22, ui.stroke); }
            ui.crest(t, x + 70, y + 80, 48);
            ui.textFit(t.name, x + cw / 2 + 38, y + 64, 170, 28, Ui.WHITE, 1);
            ui.text(t.city, x + cw / 2 + 38, y + 92, 20, Ui.GRAY, 1, false);
            ui.text("★ " + t.rating, x + 24, y + ch - 28, 30, Ui.GOLD, 0);
            ui.text(U.money(Math.max(10, 10 + (t.rating - 70) * 6)), x + cw - 24, y + ch - 28, 24, Ui.GREEN, 2);
            ui.hit(x, y, x + cw, y + ch, App.A_PICK, i);
        }
        // detail panel
        float px = gx + 4 * (cw + gap) + 10, pw = vw - px - 50;
        Team t = a.quickTeam(a.selClub);
        ui.glass(px, 160, pw, 800, 26);
        ui.crest(t, px + pw / 2, 290, 90);
        ui.textFit(t.name, px + pw / 2, 440, pw - 40, 44, Ui.WHITE, 1);
        ui.text(t.stadium + " • " + (t.capacity / 1000) + " тыс.", px + pw / 2, 480, 24, Ui.GRAY, 1, false);
        ui.text("Рейтинг команды: " + t.rating, px + pw / 2, 530, 30, Ui.GOLD, 1);
        ArrayList<Player> top = new ArrayList<Player>(t.squad);
        Collections.sort(top, new Comparator<Player>() { public int compare(Player p, Player q) { return q.ovr() - p.ovr(); } });
        for (int i = 0; i < 3 && i < top.size(); i++) {
            Player p = top.get(i);
            float fx = px + pw / 2 + (i - 1) * 150;
            ui.face(p, fx, 640, 52, t.c1);
            ui.ovrBadge(fx + 44, 690, 22, p.ovr());
            ui.textFit(p.last, fx, 750, 140, 22, Ui.WHITE, 1);
        }
        String exp = t.rating >= 82 ? "Цель: чемпионство" : (t.rating >= 77 ? "Цель: топ-4" : (t.rating >= 74 ? "Цель: середина таблицы" : "Цель: не вылететь из топ-12"));
        ui.text(exp, px + pw / 2, 810, 28, Ui.CYAN, 1);
        ui.text("Бюджет: " + U.money(30 + (80 - t.rating) * 4), px + pw / 2, 850, 26, Ui.GREEN, 1);
        ui.button(px + 30, 880, pw - 60, 90, "НАЧАТЬ КАРЬЕРУ", App.A_START_CAREER, 0, Ui.ST_GOOD, 38);
    }

    // ================================================================ HUB
    void hub() {
        Career cr = a.career;
        Team ut = cr.user();
        // sidebar
        ui.glass(20, 20, 250, vh - 40, 26);
        ui.crest(ut, 145, 110, 54);
        ui.textFit(ut.name, 145, 205, 220, 30, Ui.WHITE, 1);
        ui.text(U.money(ut.cash), 145, 243, 32, Ui.GREEN, 1);
        ui.text("Сезон " + cr.season + " • место " + cr.position(ut), 145, 276, 20, Ui.GRAY, 1, false);
        String[] names = {"ГЛАВНАЯ", "СОСТАВ", "ТАКТИКА", "ТРАНСФЕРЫ", "ЛИГА", "КЛУБ"};
        for (int i = 0; i < names.length; i++) {
            ui.button(36, 310 + i * 86, 218, 76, names[i], App.A_TAB, i, a.hubTab == i ? Ui.ST_TABON : Ui.ST_TAB, 28);
        }
        ui.button(36, vh - 130, 218, 76, "МЕНЮ", App.A_BACK, 0, Ui.ST_GHOST, 26);
        switch (a.hubTab) {
            case App.T_HOME: home(); break;
            case App.T_SQUAD: squad(); break;
            case App.T_TACT: tactics(); break;
            case App.T_MARKET: market(); break;
            case App.T_LEAGUE: league(); break;
            default: club();
        }
    }

    float cw() { return vw - cx0 - 30; }

    void home() {
        Career cr = a.career;
        Career.Fix f = cr.userFix();
        float x = cx0, y = 30, w = 820;
        ui.glass(x, y, w, 470, 28);
        ui.text(cr.weekName().toUpperCase(), x + 36, y + 56, 30, Ui.CYAN, 0);
        ui.text(cr.windowOpen() ? "Трансферное окно ОТКРЫТО" : "Окно закрыто", x + w - 36, y + 56, 22, cr.windowOpen() ? Ui.GREEN : Ui.GRAY, 2);
        if (f != null) {
            Team h = cr.teams.get(f.h), aw = cr.teams.get(f.a);
            ui.crest(h, x + 170, y + 190, 70); ui.crest(aw, x + w - 170, y + 190, 70);
            ui.textFit(h.name, x + 170, y + 300, 270, 32, Ui.WHITE, 1); ui.textFit(aw.name, x + w - 170, y + 300, 270, 32, Ui.WHITE, 1);
            ui.text("VS", x + w / 2, y + 205, 64, Ui.GOLD, 1);
            ui.text(h.stadium, x + w / 2, y + 250, 22, Ui.GRAY, 1, false);
            ui.text(f.type == 1 ? "Кубок" : "Лига", x + w / 2, y + 290, 24, Ui.WHITE, 1, false);
            formDots(h, x + 170, y + 335); formDots(aw, x + w - 170, y + 335);
            ui.text("OVR " + (int) cr.strength(h), x + 170, y + 395, 24, Ui.GRAY, 1);
            ui.text("OVR " + (int) cr.strength(aw), x + w - 170, y + 395, 24, Ui.GRAY, 1);
            ui.button(x + w / 2 - 150, y + 318, 300, 78, "▶ ИГРАТЬ", App.A_PLAY, 0, Ui.ST_GOOD, 38);
            ui.button(x + w / 2 - 150, y + 404, 300, 52, "Симулировать", App.A_SIM, 0, Ui.ST_SECOND, 24);
        } else {
            ui.text("В этом туре ваша команда не играет", x + w / 2, y + 210, 36, Ui.WHITE, 1);
            if (cr.userFix() == null && cr.curWeek() != null && cr.curWeek()[0] == 1) ui.text("Вы вылетели из Кубка", x + w / 2, y + 260, 26, Ui.GRAY, 1, false);
            ui.button(x + w / 2 - 190, y + 320, 380, 84, "Следующая неделя ▶", App.A_NEXTWEEK, 0, Ui.ST_PRIMARY, 34);
        }
        // news
        float nx = x + w + 24, nw = vw - nx - 30;
        ui.glass(nx, 30, nw, 470, 28);
        ui.text("НОВОСТИ", nx + 30, 82, 30, Ui.WHITE, 0);
        float off = ui.scrollBegin(100, nx, 100, nw, 390, cr.news.size() * 74 + 10);
        for (int i = 0; i < cr.news.size(); i++) {
            float ny = 100 + i * 74;
            ui.fill.setColor(i % 2 == 0 ? 0x14FFFFFF : 0x08FFFFFF); ui.rc.set(nx + 14, ny + 4, nx + nw - 14, ny + 68); c.drawRoundRect(ui.rc, 12, 12, ui.fill);
            ui.textFit(cr.news.get(i), nx + 30, ny + 44, nw - 60, 24, Ui.WHITE, 0);
        }
        ui.scrollEnd();
        // table
        float ty = 530;
        ui.glass(x, ty, vw - x - 30, 500, 28);
        ui.text("ТАБЛИЦА", x + 30, ty + 56, 30, Ui.WHITE, 0);
        tableRows(x + 20, ty + 76, vw - x - 70, 8, true);
    }

    void formDots(Team t, float cx, float y) {
        String f = t.form;
        for (int i = 0; i < 5; i++) {
            float x = cx - 80 + i * 40;
            int col = 0x33FFFFFF; String s = "";
            if (i < f.length()) { char ch = f.charAt(i); col = ch == 'W' ? Ui.GREEN : (ch == 'L' ? Ui.RED : 0xFFF59E0B); s = ch == 'W' ? "В" : (ch == 'L' ? "П" : "Н"); }
            ui.fill.setColor(col); c.drawCircle(x, y, 16, ui.fill);
            ui.text(s, x, y + 8, 20, 0xFF10152A, 1);
        }
    }

    void tableRows(float x, float y, float w, int rows, boolean around) {
        Career cr = a.career;
        ArrayList<Team> tb = cr.table();
        ui.text("#", x + 20, y + 22, 20, Ui.GRAY, 1); ui.text("Клуб", x + 90, y + 22, 20, Ui.GRAY, 0);
        String[] hd = {"И", "В", "Н", "П", "РМ", "О"};
        for (int k = 0; k < 6; k++) ui.text(hd[k], x + w - 360 + k * 64 + (k == 4 ? 20 : 0) + 20, y + 22, 20, Ui.GRAY, 1);
        int start = 0;
        for (int i = 0; i < rows && i < tb.size(); i++) {
            Team t = tb.get(i);
            float ry = y + 36 + i * 52;
            if (t.id == cr.user) { ui.fill.setColor(0x44FFD54A); ui.rc.set(x, ry, x + w, ry + 48); c.drawRoundRect(ui.rc, 12, 12, ui.fill); }
            else if (i % 2 == 0) { ui.fill.setColor(0x0DFFFFFF); ui.rc.set(x, ry, x + w, ry + 48); c.drawRoundRect(ui.rc, 12, 12, ui.fill); }
            int pc = i < 4 ? Ui.GREEN : (i >= tb.size() - 3 ? Ui.RED : 0x00000000);
            if (pc != 0) { ui.fill.setColor(pc); ui.rc.set(x + 2, ry + 6, x + 8, ry + 42); c.drawRoundRect(ui.rc, 3, 3, ui.fill); }
            ui.text(String.valueOf(i + 1), x + 36, ry + 34, 26, Ui.WHITE, 1);
            ui.crest(t, x + 80, ry + 24, 17);
            ui.textFit(t.name, x + 112, ry + 33, w - 520, 26, Ui.WHITE, 0);
            ui.text(String.valueOf(t.P), x + w - 340, ry + 34, 24, Ui.WHITE, 1); ui.text(String.valueOf(t.Wn), x + w - 276, ry + 34, 24, Ui.WHITE, 1);
            ui.text(String.valueOf(t.D), x + w - 212, ry + 34, 24, Ui.WHITE, 1); ui.text(String.valueOf(t.L), x + w - 148, ry + 34, 24, Ui.WHITE, 1);
            ui.text(t.GF + ":" + t.GA, x + w - 64 - 20, ry + 34, 24, Ui.GRAY, 1); ui.text(String.valueOf(t.Pts), x + w - 28, ry + 34, 28, Ui.GOLD, 1);
        }
    }

    // ---------------------------------------------------------------- SQUAD
    int posOrder(Player p) { return Player.group(p.pos) * 100 + (11 - p.pos) * 0; }

    ArrayList<Player> sortedSquad(Team t) {
        ArrayList<Player> l = new ArrayList<Player>(t.squad);
        Collections.sort(l, new Comparator<Player>() {
            public int compare(Player p, Player q) {
                int gp = Player.group(p.pos), gq = Player.group(q.pos);
                if (gp != gq) return gp - gq;
                return q.ovr() - p.ovr();
            }
        });
        return l;
    }

    void playerRow(Player p, float x, float y, float w, float h, int action, int arg, int shirt) {
        ui.fill.setColor(0x12FFFFFF); ui.rc.set(x, y, x + w, y + h - 6); c.drawRoundRect(ui.rc, 16, 16, ui.fill);
        ui.face(p, x + 48, y + h / 2 - 3, 34, shirt);
        ui.textFit(p.fullName(), x + 100, y + h / 2 - 6, 330, 28, Ui.WHITE, 0);
        ui.flag(p.nat, x + 100, y + h / 2 + 8, 30);
        ui.text(Player.POS[p.pos] + " • " + p.age + " лет" + (p.number > 0 ? " • №" + p.number : ""), x + 140, y + h / 2 + 30, 20, Ui.GRAY, 0, false);
        ui.ovrBadge(x + 500, y + h / 2 - 3, 26, p.ovr());
        ui.text("/" + p.pot, x + 540, y + h / 2 + 6, 20, Ui.GRAY, 0, false);
        // form arrow
        String ar = p.form > 0 ? "▲" : (p.form < 0 ? "▼" : "●");
        ui.text(ar, x + 620, y + h / 2 + 8, 24, p.form > 0 ? Ui.GREEN : (p.form < 0 ? Ui.RED : Ui.GRAY), 1);
        ui.bar(x + 660, y + h / 2 - 6, 110, 12, p.fitness / 100f, p.fitness > 70 ? Ui.GREEN : (p.fitness > 50 ? Ui.ORANGE : Ui.RED));
        if (p.injury > 0) ui.chip(x + 800, y + h / 2 - 22, "ТРАВМА " + p.injury + "н", Ui.RED, 20);
        else if (p.ban > 0) ui.chip(x + 800, y + h / 2 - 22, "ДИСКВ.", Ui.ORANGE, 20);
        else if (p.forSale) ui.chip(x + 800, y + h / 2 - 22, "НА ПРОДАЖЕ", Ui.ACCENT, 20);
        ui.text(p.goals + " ⚽  " + p.assists + " 🅰", x + w - 250, y + h / 2 + 8, 22, Ui.WHITE, 2, false);
        ui.text(U.money(p.value), x + w - 30, y + h / 2 + 8, 26, Ui.GREEN, 2);
        ui.hit(x, y, x + w, y + h - 6, action, arg);
    }

    void squad() {
        Career cr = a.career; Team t = cr.user();
        ui.text("СОСТАВ", cx0, 80, 54, Ui.WHITE, 0);
        ui.text(t.squad.size() + " игроков • ср. рейтинг " + t.avgOvr(14), cx0 + 330, 78, 26, Ui.GRAY, 0, false);
        ArrayList<Player> l = sortedSquad(t);
        float w = cw();
        float off = ui.scrollBegin(200, cx0, 110, w, vh - 130, l.size() * 92 + 20);
        for (int i = 0; i < l.size(); i++) playerRow(l.get(i), cx0, 110 + i * 92, w, 92, App.A_PLAYER, l.get(i).id, t.c1);
        ui.scrollEnd();
    }

    // ---------------------------------------------------------------- TACTICS
    void tactics() {
        Career cr = a.career; Team t = cr.user();
        ui.text("ТАКТИКА И СОСТАВ", cx0, 80, 54, Ui.WHITE, 0);
        // formations
        for (int i = 0; i < Data.FORM_NAME.length; i++) {
            ui.button(cx0 + i * 98, 104, 92, 56, Data.FORM_NAME[i], App.A_FORM, i, t.formation == i ? Ui.ST_TABON : Ui.ST_SECOND, 19);
        }
        // pitch
        float px = cx0, py = 180, pw = 880, ph = 640;
        ui.fill.setShader(new LinearGradient(px, py, px + pw, py, 0xFF2A8A3A, 0xFF1F6E2E, Shader.TileMode.CLAMP));
        ui.rc.set(px, py, px + pw, py + ph); c.drawRoundRect(ui.rc, 24, 24, ui.fill); ui.fill.setShader(null);
        for (int i = 0; i < 8; i++) { if (i % 2 == 0) { ui.fill.setColor(0x14000000); c.drawRect(px + i * pw / 8, py, px + (i + 1) * pw / 8, py + ph, ui.fill); } }
        ui.stroke.setColor(0x88FFFFFF); ui.stroke.setStrokeWidth(3);
        c.drawRect(px + 14, py + 14, px + pw - 14, py + ph - 14, ui.stroke);
        c.drawLine(px + pw / 2, py + 14, px + pw / 2, py + ph - 14, ui.stroke);
        c.drawCircle(px + pw / 2, py + ph / 2, 70, ui.stroke);
        c.drawRect(px + 14, py + ph / 2 - 150, px + 150, py + ph / 2 + 150, ui.stroke);
        c.drawRect(px + pw - 150, py + ph / 2 - 150, px + pw - 14, py + ph / 2 + 150, ui.stroke);
        float[] U1 = Data.FORM_U[t.formation], V1 = Data.FORM_V[t.formation];
        int[] roles = Data.FORM_ROLE[t.formation];
        for (int i = 0; i < 11; i++) {
            Player p = t.byId(t.lineup[i]);
            if (p == null) continue;
            float sx = px + 40 + U1[i] * (pw - 120), sy = py + 50 + V1[i] * (ph - 100);
            boolean sel = a.selSlot == i;
            int fit = p.ovrAt(roles[i]) - p.ovr();
            ui.fill.setColor(sel ? 0xFFFFD54A : (fit < -7 ? Ui.RED : (fit < 0 ? Ui.ORANGE : 0x55FFFFFF)));
            c.drawCircle(sx, sy, 40, ui.fill);
            ui.face(p, sx, sy, 35, t.c1);
            ui.ovrBadge(sx + 32, sy - 30, 17, p.ovrAt(roles[i]));
            ui.fill.setColor(0xAA000000); ui.rc.set(sx - 56, sy + 40, sx + 56, sy + 68); c.drawRoundRect(ui.rc, 10, 10, ui.fill);
            ui.textFit(p.last, sx, sy + 62, 104, 22, Ui.WHITE, 1);
            ui.text(Player.POS[roles[i]], sx - 30, sy - 38, 18, Ui.WHITE, 1);
            ui.hit(sx - 56, sy - 44, sx + 56, sy + 68, App.A_SLOT, i);
        }
        // style controls
        float sx = px, sy = py + ph + 26;
        String[] kn = {"Стиль", "Прессинг", "Линия", "Ширина", "Темп"};
        String[][] vn = {{"Оборон.", "Баланс", "Атака"}, {"Низкий", "Средний", "Высокий"}, {"Низкая", "Средняя", "Высокая"}, {"Узко", "Норм.", "Широко"}, {"Медл.", "Норм.", "Быстро"}};
        int[] cur = {t.style, t.press, t.line, t.width, t.tempo};
        for (int k = 0; k < 5; k++) {
            float bx = sx + k * 178;
            ui.text(kn[k], bx + 80, sy + 18, 20, Ui.GRAY, 1);
            for (int v = 0; v < 3; v++) ui.button(bx + v * 56 - 0, sy + 28, 54, 44, v == 0 ? "-" : (v == 1 ? "•" : "+"), App.A_STYLE, k * 10 + v, cur[k] == v ? Ui.ST_TABON : Ui.ST_SECOND, 22);
            ui.text(vn[k][cur[k]], bx + 80, sy + 98, 20, Ui.WHITE, 1, false);
        }
        // bench / reserves
        float rx = px + pw + 24, rw = vw - rx - 30;
        ui.glass(rx, 110, rw, vh - 140, 26);
        ui.text(a.selSlot >= 0 ? "Выберите замену" : "Нажмите на позицию слева", rx + 24, 156, 28, a.selSlot >= 0 ? Ui.GOLD : Ui.GRAY, 0);
        ui.button(rx + rw - 190, 120, 170, 56, "АВТО", App.A_AUTO, 0, Ui.ST_SECOND, 24);
        ArrayList<Player> pool = new ArrayList<Player>();
        for (int id : t.bench) { Player p = t.byId(id); if (p != null) pool.add(p); }
        ArrayList<Player> rest = sortedSquad(t);
        for (Player p : rest) { boolean in = false; for (int id : t.lineup) if (id == p.id) in = true; if (!in && !pool.contains(p)) pool.add(p); }
        for (int id : t.bench) { }
        ui.scrollBegin(210, rx, 190, rw, vh - 230, pool.size() * 76 + 20);
        for (int i = 0; i < pool.size(); i++) {
            Player p = pool.get(i);
            float y = 190 + i * 76;
            boolean bench = false; for (int id : t.bench) if (id == p.id) bench = true;
            ui.fill.setColor(bench ? 0x22FFFFFF : 0x10FFFFFF); ui.rc.set(rx + 12, y, rx + rw - 12, y + 70); c.drawRoundRect(ui.rc, 14, 14, ui.fill);
            ui.face(p, rx + 54, y + 35, 28, t.c1);
            ui.textFit(p.shortName(), rx + 96, y + 32, rw - 260, 24, Ui.WHITE, 0);
            ui.text(Player.POS[p.pos] + (bench ? " • скамейка" : ""), rx + 96, y + 58, 18, Ui.GRAY, 0, false);
            if (!p.available()) ui.chip(rx + rw - 230, y + 18, p.injury > 0 ? "ТРАВМА" : "ДИСКВ.", Ui.RED, 16);
            ui.ovrBadge(rx + rw - 60, y + 35, 22, a.selSlot >= 0 ? p.ovrAt(roles[a.selSlot]) : p.ovr());
            ui.hit(rx + 12, y, rx + rw - 12, y + 70, App.A_BENCH, p.id);
        }
        ui.scrollEnd();
    }

    // ---------------------------------------------------------------- MARKET
    void marketList() {
        Career cr = a.career;
        String key = a.fPos + "|" + a.fOvr + "|" + a.fAge + "|" + a.fPrice + "|" + a.sortKey + "|" + a.src + "|" + cr.week + "|" + cr.season + "|" + cr.pool.size() + "|" + cr.user().squad.size();
        if (key.equals(mkey)) return;
        mkey = key;
        mcache.clear();
        for (Team t : cr.teams) if (t.id != cr.user && a.src != 2) for (Player p : t.squad) mcache.add(p);
        if (a.src != 1) for (Player p : cr.pool) if (a.src == 0 || p.teamId == -1 || a.src == 2) { if (a.src == 2 && p.teamId != -1) continue; mcache.add(p); }
        if (a.src == 1) for (Player p : cr.pool) if (p.teamId == -2) mcache.add(p);
        for (int i = mcache.size() - 1; i >= 0; i--) {
            Player p = mcache.get(i);
            float ask = p.teamId == -1 ? 0 : cr.askingPrice(p);
            if ((a.fPos >= 0 && p.pos != a.fPos && p.pos2 != a.fPos) || p.ovr() < a.fOvr || p.age > a.fAge || ask > a.fPrice) mcache.remove(i);
        }
        final int sk = a.sortKey;
        Collections.sort(mcache, new Comparator<Player>() {
            public int compare(Player p, Player q) {
                switch (sk) { case 1: return Float.compare(p.value, q.value); case 2: return p.age - q.age; case 3: return q.pot - p.pot; default: return q.ovr() - p.ovr(); }
            }
        });
    }

    void market() {
        Career cr = a.career;
        ui.text("ТРАНСФЕРЫ", cx0, 80, 54, Ui.WHITE, 0);
        ui.text(cr.windowOpen() ? "Окно открыто" : "Окно закрыто (доступны свободные агенты)", cx0 + 400, 78, 24, cr.windowOpen() ? Ui.GREEN : Ui.ORANGE, 0, false);
        ui.text("Бюджет " + U.money(cr.user().cash), vw - 40, 78, 34, Ui.GREEN, 2);
        ui.button(cx0, 100, 220, 56, "Рынок", App.A_MTAB, 0, a.marketTab == 0 ? Ui.ST_TABON : Ui.ST_SECOND, 24);
        ui.button(cx0 + 232, 100, 260, 56, "Предложения (" + cr.offers.size() + ")", App.A_MTAB, 1, a.marketTab == 1 ? Ui.ST_TABON : Ui.ST_SECOND, 24);
        if (a.marketTab == 1) { offers(); return; }
        marketList();
        float fy = 170, w = cw();
        String[] srcN = {"Все клубы", "Мировой рынок", "Свободные"};
        String[] sortN = {"по OVR", "по цене", "по возрасту", "по потенциалу"};
        ui.button(cx0, fy, 210, 54, srcN[a.src], App.A_SRC, 0, Ui.ST_SECOND, 22);
        ui.button(cx0 + 222, fy, 170, 54, a.fPos < 0 ? "Поз: ВСЕ" : "Поз: " + Player.POS[a.fPos], App.A_FPOS, 0, Ui.ST_SECOND, 22);
        ui.button(cx0 + 404, fy, 60, 54, "−", App.A_FOVR, -1, Ui.ST_SECOND, 28); ui.text("OVR ≥ " + a.fOvr, cx0 + 534, fy + 36, 22, Ui.WHITE, 1); ui.button(cx0 + 604, fy, 60, 54, "+", App.A_FOVR, 1, Ui.ST_SECOND, 28);
        ui.button(cx0 + 676, fy, 60, 54, "−", App.A_FPRICE, -1, Ui.ST_SECOND, 28); ui.text("до " + U.money(a.fPrice), cx0 + 780, fy + 36, 22, Ui.WHITE, 1); ui.button(cx0 + 836, fy, 60, 54, "+", App.A_FPRICE, 1, Ui.ST_SECOND, 28);
        ui.button(cx0 + 908, fy, 60, 54, "−", App.A_FAGE, -1, Ui.ST_SECOND, 28); ui.text("≤ " + a.fAge + " л.", cx0 + 1000, fy + 36, 22, Ui.WHITE, 1); ui.button(cx0 + 1030, fy, 60, 54, "+", App.A_FAGE, 1, Ui.ST_SECOND, 28);
        ui.button(cx0 + 1106, fy, 210, 54, sortN[a.sortKey], App.A_SORT, 0, Ui.ST_PRIMARY, 22);
        ui.text("Найдено: " + mcache.size(), vw - 40, fy + 36, 22, Ui.GRAY, 2, false);
        float ly = 240;
        ui.scrollBegin(300, cx0, ly, w, vh - ly - 10, Math.min(mcache.size(), 120) * 92 + 20);
        for (int i = 0; i < Math.min(mcache.size(), 120); i++) {
            Player p = mcache.get(i);
            float y = ly + i * 92;
            playerRowMarket(p, cx0, y, w);
        }
        ui.scrollEnd();
    }

    void playerRowMarket(Player p, float x, float y, float w) {
        Career cr = a.career;
        ui.fill.setColor(0x12FFFFFF); ui.rc.set(x, y, x + w, y + 86); c.drawRoundRect(ui.rc, 16, 16, ui.fill);
        int shirt = p.teamId >= 0 ? cr.teams.get(p.teamId).c1 : 0xFF475569;
        ui.face(p, x + 48, y + 40, 34, shirt);
        ui.textFit(p.fullName(), x + 100, y + 36, 320, 28, Ui.WHITE, 0);
        ui.flag(p.nat, x + 100, y + 48, 30);
        String club = p.teamId >= 0 ? cr.teams.get(p.teamId).name : (p.teamId == -1 ? "Свободный агент" : p.extClub);
        ui.textFit(Player.POS[p.pos] + " • " + p.age + " л. • " + club, x + 140, y + 68, 330, 20, Ui.GRAY, 0);
        ui.ovrBadge(x + 520, y + 40, 26, p.ovr());
        ui.text("пот. " + p.pot, x + 565, y + 48, 22, Ui.GRAY, 0, false);
        ui.text("ПАС " + p.pac + "  УДР " + p.sho + "  ПЕР " + p.pas + "  ДРИ " + p.dri + "  ЗАЩ " + p.def + "  ФИЗ " + p.phy, x + 690, y + 48, 20, Ui.GRAY, 0, false);
        float ask = p.teamId == -1 ? 0 : cr.askingPrice(p);
        ui.text(p.teamId == -1 ? "БЕСПЛАТНО" : U.money(ask), x + w - 200, y + 50, 30, p.teamId == -1 ? Ui.CYAN : Ui.GREEN, 2);
        ui.button(x + w - 180, y + 14, 160, 58, "КУПИТЬ", App.A_BUY, p.id, Ui.ST_GOOD, 24);
    }

    void offers() {
        Career cr = a.career;
        float y0 = 190;
        if (cr.offers.isEmpty()) { ui.text("Предложений пока нет. Выставьте игроков на трансфер в разделе «Состав».", cx0 + 40, y0 + 60, 28, Ui.GRAY, 0, false); return; }
        for (int i = 0; i < cr.offers.size(); i++) {
            Career.Offer o = cr.offers.get(i);
            Player p = cr.byId.get(o.pid);
            if (p == null) continue;
            float y = y0 + i * 112;
            ui.glass(cx0, y, cw(), 100, 20);
            ui.face(p, cx0 + 60, y + 50, 38, cr.user().c1);
            ui.text(p.fullName(), cx0 + 120, y + 44, 30, Ui.WHITE, 0);
            ui.text(o.from + " предлагает " + U.money(o.fee) + " (рын. цена " + U.money(p.value) + ")", cx0 + 120, y + 78, 22, Ui.GRAY, 0, false);
            ui.button(cx0 + cw() - 400, y + 18, 180, 64, "ПРОДАТЬ", App.A_ACCEPT, i, Ui.ST_GOOD, 26);
            ui.button(cx0 + cw() - 200, y + 18, 180, 64, "ОТКАЗ", App.A_REJECT, i, Ui.ST_DANGER, 26);
        }
    }

    // ---------------------------------------------------------------- LEAGUE
    void league() {
        Career cr = a.career;
        ui.text("ЛИГА И КУБОК", cx0, 80, 54, Ui.WHITE, 0);
        String[] tn = {"Таблица", "Мои матчи", "Бомбардиры", "Кубок"};
        for (int i = 0; i < 4; i++) ui.button(cx0 + i * 232, 100, 222, 56, tn[i], App.A_LTAB, i, a.leagueTab == i ? Ui.ST_TABON : Ui.ST_SECOND, 24);
        float w = cw();
        ui.glass(cx0, 176, w, vh - 206, 26);
        switch (a.leagueTab) {
            case 0: tableRows(cx0 + 20, 190, w - 40, 16, false); break;
            case 1: {
                ArrayList<Career.Fix> mine = new ArrayList<Career.Fix>();
                for (Career.Fix f : cr.fixtures) if (f.h == cr.user || f.a == cr.user) mine.add(f);
                Collections.sort(mine, new Comparator<Career.Fix>() { public int compare(Career.Fix p, Career.Fix q) { return p.round - q.round; } });
                ui.scrollBegin(400, cx0, 190, w, vh - 230, mine.size() * 62 + 20);
                for (int i = 0; i < mine.size(); i++) {
                    Career.Fix f = mine.get(i); float y = 190 + i * 62;
                    Team h = cr.teams.get(f.h), aw = cr.teams.get(f.a);
                    if (i % 2 == 0) { ui.fill.setColor(0x0DFFFFFF); ui.rc.set(cx0 + 16, y, cx0 + w - 16, y + 58); c.drawRoundRect(ui.rc, 10, 10, ui.fill); }
                    ui.text("Тур " + (f.round + 1), cx0 + 50, y + 38, 22, Ui.GRAY, 0);
                    ui.crest(h, cx0 + 250, y + 29, 20); ui.textFit(h.name, cx0 + 280, y + 38, 300, 26, Ui.WHITE, 0);
                    ui.text(f.played ? f.hg + " : " + f.ag : "—", cx0 + w / 2 + 40, y + 40, 30, f.played ? Ui.GOLD : Ui.GRAY, 1);
                    ui.crest(aw, cx0 + w / 2 + 160, y + 29, 20); ui.textFit(aw.name, cx0 + w / 2 + 192, y + 38, 300, 26, Ui.WHITE, 0);
                }
                ui.scrollEnd();
                break;
            }
            case 2: {
                ArrayList<Player> ts = cr.topScorers(12);
                for (int i = 0; i < ts.size(); i++) {
                    Player p = ts.get(i); float y = 200 + i * 66;
                    Team t = cr.teams.get(p.teamId >= 0 ? p.teamId : 0);
                    ui.text(String.valueOf(i + 1), cx0 + 50, y + 40, 28, Ui.WHITE, 1);
                    ui.face(p, cx0 + 120, y + 30, 26, t.c1);
                    ui.text(p.fullName(), cx0 + 170, y + 36, 28, Ui.WHITE, 0);
                    ui.text(t.name, cx0 + 560, y + 36, 22, Ui.GRAY, 0, false);
                    ui.text(p.goals + " голов", cx0 + w - 260, y + 38, 28, Ui.GOLD, 2);
                    ui.text(p.assists + " пас.", cx0 + w - 60, y + 38, 24, Ui.GRAY, 2);
                }
                if (ts.isEmpty()) ui.text("Пока нет забитых голов", cx0 + 60, 260, 30, Ui.GRAY, 0, false);
                break;
            }
            default: {
                String[] rn = {"1/8 финала", "1/4 финала", "Полуфинал", "Финал"};
                float colW = (w - 40) / 4f;
                for (int r = 0; r < 4; r++) {
                    ui.text(rn[r], cx0 + 20 + r * colW + colW / 2, 226, 26, Ui.CYAN, 1);
                    int k = 0;
                    for (Career.Fix f : cr.cup) {
                        if (f.round != r) continue;
                        float y = 250 + k * (r == 0 ? 96 : r == 1 ? 192 : r == 2 ? 384 : 0) + (r == 1 ? 48 : r == 2 ? 144 : r == 3 ? 340 : 0) * 1;
                        float x = cx0 + 30 + r * colW;
                        Team h = cr.teams.get(f.h), aw = cr.teams.get(f.a);
                        ui.fill.setColor((f.h == cr.user || f.a == cr.user) ? 0x44FFD54A : 0x16FFFFFF); ui.rc.set(x, y, x + colW - 30, y + 84); c.drawRoundRect(ui.rc, 12, 12, ui.fill);
                        ui.crest(h, x + 28, y + 24, 14); ui.textFit(h.shortName, x + 52, y + 31, colW - 150, 22, Ui.WHITE, 0); ui.text(f.played ? String.valueOf(f.hg) : "", x + colW - 52, y + 31, 22, Ui.GOLD, 2);
                        ui.crest(aw, x + 28, y + 62, 14); ui.textFit(aw.shortName, x + 52, y + 69, colW - 150, 22, Ui.WHITE, 0); ui.text(f.played ? String.valueOf(f.ag) : "", x + colW - 52, y + 69, 22, Ui.GOLD, 2);
                        k++;
                    }
                }
                if (cr.cupWinner >= 0) ui.text("Победитель: " + cr.teams.get(cr.cupWinner).name, cx0 + w / 2, vh - 60, 32, Ui.GOLD, 1);
            }
        }
    }

    // ---------------------------------------------------------------- CLUB
    void club() {
        Career cr = a.career; Team t = cr.user();
        ui.text("КЛУБ", cx0, 80, 54, Ui.WHITE, 0);
        float w = cw();
        ui.glass(cx0, 110, 760, 430, 26);
        ui.text("ФИНАНСЫ", cx0 + 30, 160, 30, Ui.CYAN, 0);
        float wages = 0; for (Player p : t.squad) wages += p.wage / 1000f;
        ui.text("Баланс", cx0 + 30, 220, 28, Ui.GRAY, 0, false); ui.text(U.money(t.cash), cx0 + 730, 220, 34, Ui.GREEN, 2);
        ui.text("Зарплаты в неделю", cx0 + 30, 270, 28, Ui.GRAY, 0, false); ui.text(U.money(wages), cx0 + 730, 270, 30, Ui.RED, 2);
        ui.text("Доход (посл. неделя)", cx0 + 30, 320, 28, Ui.GRAY, 0, false); ui.text(U.money(cr.lastIncome), cx0 + 730, 320, 30, Ui.GREEN, 2);
        ui.text("Стадион", cx0 + 30, 370, 28, Ui.GRAY, 0, false); ui.text(t.stadium + " (" + t.capacity + ")", cx0 + 730, 370, 26, Ui.WHITE, 2);
        ui.text("Титулов: " + cr.titles + "   Кубков: " + cr.cups, cx0 + 30, 430, 28, Ui.GOLD, 0);
        ui.text("Сезон " + cr.season + " • " + cr.weekName(), cx0 + 30, 480, 24, Ui.GRAY, 0, false);
        // upgrades
        float ux = cx0 + 790;
        ui.glass(ux, 110, vw - ux - 30, 430, 26);
        ui.text("РАЗВИТИЕ КЛУБА", ux + 30, 160, 30, Ui.CYAN, 0);
        String[] un = {"Стадион (доход)", "Тренировочная база (рост игроков)", "Академия (молодёжь)"};
        int[] lv = {t.stadiumLvl, t.trainLvl, t.youthLvl};
        for (int i = 0; i < 3; i++) {
            float y = 190 + i * 112;
            ui.text(un[i], ux + 30, y + 30, 26, Ui.WHITE, 0);
            for (int k = 0; k < 5; k++) { ui.fill.setColor(k < lv[i] ? Ui.GOLD : 0x33FFFFFF); ui.rc.set(ux + 30 + k * 52, y + 46, ux + 74 + k * 52, y + 66); c.drawRoundRect(ui.rc, 6, 6, ui.fill); }
            if (lv[i] < 5) ui.button(ux + 320, y + 34, vw - ux - 380, 60, "Улучшить " + U.money(cr.upgradeCost(i)), App.A_UPG, i, Ui.ST_GOOD, 22);
            else ui.text("МАКС.", ux + 340, y + 78, 28, Ui.GOLD, 0);
        }
        // history
        ui.glass(cx0, 560, w, 360, 26);
        ui.text("ИСТОРИЯ", cx0 + 30, 610, 30, Ui.CYAN, 0);
        for (int i = 0; i < Math.min(5, cr.history.size()); i++) ui.textFit(cr.history.get(cr.history.size() - 1 - i), cx0 + 30, 660 + i * 46, w - 60, 26, Ui.WHITE, 0);
        if (cr.history.isEmpty()) ui.text("Первый сезон ещё впереди", cx0 + 30, 670, 26, Ui.GRAY, 0, false);
        ui.button(cx0, 950, 340, 80, "СОХРАНИТЬ", App.A_SAVE, 0, Ui.ST_PRIMARY, 30);
        ui.button(cx0 + 360, 950, 340, 80, "В ГЛАВНОЕ МЕНЮ", App.A_QUITMENU, 0, Ui.ST_SECOND, 28);
    }

    // ================================================================ DIALOGS
    void dialogs() {
        ui.modalBegin();
        ui.fill.setColor(0xB0000000); c.drawRect(0, 0, vw, vh, ui.fill);
        Career cr = a.career;
        switch (a.dialog) {
            case App.D_PLAYER: playerDialog(); break;
            case App.D_BID: bidDialog(); break;
            case App.D_SEASON: seasonDialog(); break;
            case App.D_CONFIRM: {
                float w = 700, h = 330, x = vw / 2f - w / 2, y = vh / 2f - h / 2;
                ui.glass(x, y, w, h, 28);
                ui.text(a.confirmKind == 1 ? "Расторгнуть контракт?" : "Выйти из матча?", x + w / 2, y + 100, 44, Ui.WHITE, 1);
                ui.text(a.confirmKind == 1 && a.selPlayer != null ? a.selPlayer.fullName() : "Результат не будет сохранён", x + w / 2, y + 150, 26, Ui.GRAY, 1, false);
                ui.button(x + 40, y + 200, 300, 90, "ДА", App.A_CONFYES, 0, Ui.ST_DANGER, 34);
                ui.button(x + w - 340, y + 200, 300, 90, "ОТМЕНА", App.A_CLOSE, 0, Ui.ST_SECOND, 34);
                break;
            }
            default: {
                float w = 760, h = 300, x = vw / 2f - w / 2, y = vh / 2f - h / 2;
                ui.glass(x, y, w, h, 28);
                ui.textFit(a.msg, x + w / 2, y + 130, w - 60, 34, Ui.WHITE, 1);
                ui.button(x + w / 2 - 150, y + 190, 300, 80, "ОК", App.A_CLOSE, 0, Ui.ST_PRIMARY, 34);
            }
        }
    }

    void seasonDialog() {
        Career cr = a.career;
        float w = 1000, h = 640, x = vw / 2f - w / 2, y = vh / 2f - h / 2;
        ui.glass(x, y, w, h, 30);
        ui.text("СЕЗОН ЗАВЕРШЁН", x + w / 2, y + 80, 56, Ui.GOLD, 1);
        ArrayList<Team> tb = cr.table();
        ui.crest(tb.get(0), x + w / 2, y + 190, 60);
        ui.text("Чемпион: " + tb.get(0).name, x + w / 2, y + 300, 38, Ui.WHITE, 1);
        ui.text("Ваше место: " + (tb.indexOf(cr.user()) + 1), x + w / 2, y + 360, 38, Ui.CYAN, 1);
        ui.text("Игроки повзрослели, контракты продлены, пришла молодёжь.", x + w / 2, y + 420, 24, Ui.GRAY, 1, false);
        ui.text("Призовые начислены. Баланс: " + U.money(cr.user().cash), x + w / 2, y + 460, 26, Ui.GREEN, 1);
        ui.button(x + w / 2 - 230, y + 510, 460, 96, "НОВЫЙ СЕЗОН ▶", App.A_SEASONOK, 0, Ui.ST_GOOD, 38);
    }

    void playerDialog() {
        Player p = a.selPlayer; if (p == null) { a.dialog = App.D_NONE; return; }
        Career cr = a.career;
        float w = 1240, h = 800, x = vw / 2f - w / 2, y = vh / 2f - h / 2;
        ui.glass(x, y, w, h, 30);
        int shirt = p.teamId >= 0 ? cr.teams.get(p.teamId).c1 : 0xFF475569;
        ui.face(p, x + 150, y + 160, 100, shirt);
        ui.text(p.fullName(), x + 290, y + 90, 50, Ui.WHITE, 0);
        ui.flag(p.nat, x + 290, y + 110, 52); ui.text(Data.NAT_CODE[p.nat] + " • " + Player.POS_LONG[p.pos] + " • " + p.age + " лет • " + p.height + " см", x + 360, y + 148, 26, Ui.GRAY, 0, false);
        String club = p.teamId >= 0 ? cr.teams.get(p.teamId).name : (p.teamId == -1 ? "Свободный агент" : p.extClub);
        ui.text(club + (p.number > 0 ? " • №" + p.number : ""), x + 290, y + 190, 28, Ui.CYAN, 0);
        ui.ovrBadge(x + w - 150, y + 110, 56, p.ovr()); ui.text("ПОТЕНЦИАЛ " + p.pot, x + w - 150, y + 200, 24, Ui.GRAY, 1);
        // attributes
        String[] an = {"Скорость", "Удары", "Пас", "Дриблинг", "Защита", "Физика", "Вратарь", "Выносл."};
        int[] av = {p.pac, p.sho, p.pas, p.dri, p.def, p.phy, p.gk, p.sta};
        int shown = p.pos == Player.GK ? 8 : 7;
        for (int i = 0, k = 0; i < 8; i++) {
            if (i == 6 && p.pos != Player.GK) continue;
            float yy = y + 250 + k * 56; k++;
            ui.text(an[i], x + 40, yy + 28, 26, Ui.GRAY, 0, false);
            ui.bar(x + 230, yy + 10, 340, 22, av[i] / 99f, ui.ovrColor(av[i]));
            ui.text(String.valueOf(av[i]), x + 600, yy + 30, 28, Ui.WHITE, 2);
        }
        // info column
        float ix = x + 680;
        ui.text("Рыночная цена", ix, y + 280, 24, Ui.GRAY, 0, false); ui.text(U.money(p.value), ix + 480, y + 282, 30, Ui.GREEN, 2);
        ui.text("Зарплата / нед.", ix, y + 330, 24, Ui.GRAY, 0, false); ui.text("€" + (int) p.wage + "K", ix + 480, y + 332, 28, Ui.WHITE, 2);
        ui.text("Контракт (лет)", ix, y + 380, 24, Ui.GRAY, 0, false); ui.text(String.valueOf(p.contract), ix + 480, y + 382, 28, Ui.WHITE, 2);
        ui.text("Форма / Мораль", ix, y + 430, 24, Ui.GRAY, 0, false); ui.text((p.form >= 0 ? "+" : "") + p.form + " / " + p.morale, ix + 480, y + 432, 28, Ui.WHITE, 2);
        ui.text("Сезон: " + p.apps + " матчей, " + p.goals + " гол., " + p.assists + " пас.", ix, y + 485, 26, Ui.WHITE, 0);
        ui.text("Ср. оценка " + (p.apps > 0 ? String.format("%.1f", p.avgRating()) : "—") + " • ЖК " + p.yel + " КК " + p.red, ix, y + 525, 24, Ui.GRAY, 0, false);
        ui.text("Карьера: " + p.careerApps + " матчей, " + p.careerGoals + " голов", ix, y + 565, 24, Ui.GRAY, 0, false);
        if (p.injury > 0) ui.chip(ix, y + 595, "ТРАВМА: " + p.injury + " нед.", Ui.RED, 24);
        // buttons
        float by = y + h - 110;
        if (p.teamId == cr.user) {
            ui.button(x + 40, by, 300, 80, p.forSale ? "СНЯТЬ С ПРОДАЖИ" : "ВЫСТАВИТЬ НА ТРАНСФЕР", App.A_SELL, 0, Ui.ST_SECOND, 24);
            ui.button(x + 360, by, 260, 80, "КАПИТАН", App.A_CAPTAIN, 0, Ui.ST_SECOND, 28);
            ui.button(x + 640, by, 300, 80, "РАСТОРГНУТЬ", App.A_RELEASE, 0, Ui.ST_DANGER, 28);
        } else ui.button(x + 40, by, 360, 80, "КУПИТЬ", App.A_BUY, p.id, Ui.ST_GOOD, 32);
        ui.button(x + w - 280, by, 240, 80, "ЗАКРЫТЬ", App.A_CLOSE, 0, Ui.ST_PRIMARY, 30);
    }

    void bidDialog() {
        Player p = a.bidPlayer; if (p == null) { a.dialog = App.D_NONE; return; }
        Career cr = a.career;
        float w = 980, h = 640, x = vw / 2f - w / 2, y = vh / 2f - h / 2;
        ui.glass(x, y, w, h, 30);
        ui.face(p, x + 120, y + 120, 70, p.teamId >= 0 ? cr.teams.get(p.teamId).c1 : 0xFF475569);
        ui.text(p.fullName(), x + 220, y + 90, 44, Ui.WHITE, 0);
        ui.text(Player.POS[p.pos] + " • " + p.age + " л. • OVR " + p.ovr() + " • " + (p.teamId >= 0 ? cr.teams.get(p.teamId).name : (p.teamId == -1 ? "Свободный агент" : p.extClub)), x + 220, y + 130, 24, Ui.GRAY, 0, false);
        ui.text("Ваше предложение", x + w / 2, y + 220, 28, Ui.GRAY, 1, false);
        ui.text(p.teamId == -1 ? "БЕСПЛАТНО" : U.money(a.bidFee), x + w / 2, y + 310, 84, Ui.GOLD, 1);
        if (p.teamId != -1) {
            ui.button(x + 60, y + 250, 120, 80, "−5M", App.A_BIDADJ, -50, Ui.ST_SECOND, 28);
            ui.button(x + 190, y + 250, 120, 80, "−1M", App.A_BIDADJ, -10, Ui.ST_SECOND, 28);
            ui.button(x + w - 310, y + 250, 120, 80, "+1M", App.A_BIDADJ, 10, Ui.ST_SECOND, 28);
            ui.button(x + w - 180, y + 250, 120, 80, "+5M", App.A_BIDADJ, 50, Ui.ST_SECOND, 28);
        }
        ui.text("Ваш бюджет: " + U.money(cr.user().cash) + " • Зарплата игрока: €" + (int) p.wage + "K/нед.", x + w / 2, y + 380, 26, Ui.GREEN, 1, false);
        if (a.bidResult.length() > 0) ui.textFit(a.bidResult, x + w / 2, y + 440, w - 80, 28, a.bidResult.startsWith("✔") ? Ui.GREEN : Ui.ORANGE, 1);
        ui.button(x + 60, y + h - 130, 400, 90, "ПРЕДЛОЖИТЬ", App.A_BIDOK, 0, Ui.ST_GOOD, 34);
        ui.button(x + w - 460, y + h - 130, 400, 90, "ЗАКРЫТЬ", App.A_CLOSE, 0, Ui.ST_SECOND, 34);
    }

    // ================================================================ PREP
    void prep() {
        Career cr = a.career;
        Career.Fix f = cr.userFix();
        if (f == null) { a.screen = App.S_HUB; return; }
        Team h = cr.teams.get(f.h), aw = cr.teams.get(f.a);
        title(f.type == 1 ? "МАТЧ КУБКА" : "МАТЧ ЛИГИ", cr.weekName());
        ui.button(vw - 250, 36, 210, 70, "← НАЗАД", App.A_BACK + 100 * 0 + 0, 0, Ui.ST_SECOND);
        for (int s = 0; s < 2; s++) {
            Team t = s == 0 ? h : aw;
            float x = s == 0 ? 80 : vw / 2f + 60, w = vw / 2f - 140;
            ui.glass(x, 170, w, 700, 30);
            ui.crest(t, x + 100, 260, 56);
            ui.text(t.name, x + 180, 255, 44, Ui.WHITE, 0);
            ui.text(Data.FORM_NAME[t.formation] + " • OVR " + (int) cr.strength(t), x + 180, 295, 26, Ui.CYAN, 0);
            // XI list
            for (int i = 0; i < 11; i++) {
                Player p = t.byId(t.lineup[i]);
                if (p == null) continue;
                float yy = 340 + i * 46;
                ui.text(String.valueOf(p.number), x + 50, yy + 30, 24, Ui.GRAY, 1);
                ui.text(Player.POS[Data.FORM_ROLE[t.formation][i]], x + 110, yy + 30, 22, Ui.CYAN, 1);
                ui.textFit(p.fullName(), x + 160, yy + 30, w - 320, 26, Ui.WHITE, 0);
                ui.ovrBadge(x + w - 60, yy + 22, 18, p.ovr());
            }
        }
        ui.text("VS", vw / 2f, 520, 70, Ui.GOLD, 1);
        ui.text(h.stadium, vw / 2f, 570, 22, Ui.GRAY, 1, false);
        ui.button(vw / 2f - 330, 900, 320, 100, "ТАКТИКА", App.A_TAB, App.T_TACT + 100, Ui.ST_SECOND, 34);
        ui.button(vw / 2f + 10, 900, 320, 100, "▶ НАЧАТЬ", App.A_PREPGO, 0, Ui.ST_GOOD, 40);
    }

    // ================================================================ QUICK
    void quick() {
        title(a.qPen ? "СЕРИЯ ПЕНАЛЬТИ" : "БЫСТРЫЙ МАТЧ", "Выберите команды и сторону");
        backBtn();
        for (int s = 0; s < 2; s++) {
            float x = s == 0 ? 60 : vw / 2f + 20, w = vw / 2f - 80;
            ui.text(s == 0 ? "ХОЗЯЕВА" : "ГОСТИ", x + w / 2, 175, 30, Ui.CYAN, 1);
            ui.scrollBegin(500 + s, x, 190, w, 650, 16 * 76 + 10);
            for (int i = 0; i < 16; i++) {
                Team t = a.quickTeam(i); float y = 190 + i * 76;
                boolean sel = (s == 0 ? a.qHome : a.qAway) == i;
                ui.fill.setColor(sel ? 0x558A3FFC : 0x14FFFFFF); ui.rc.set(x, y, x + w, y + 70); c.drawRoundRect(ui.rc, 16, 16, ui.fill);
                ui.crest(t, x + 50, y + 35, 24); ui.text(t.name, x + 100, y + 44, 30, Ui.WHITE, 0); ui.text("★" + t.rating, x + w - 24, y + 44, 28, Ui.GOLD, 2);
                ui.hit(x, y, x + w, y + 70, s == 0 ? App.A_QHOME : App.A_QAWAY, i);
            }
            ui.scrollEnd();
        }
        ui.text("Вы играете за:", 60, 920, 28, Ui.GRAY, 0, false);
        ui.button(320, 880, 280, 80, "ХОЗЯЕВА", App.A_QSIDE, 0, a.qSide == 0 ? Ui.ST_TABON : Ui.ST_SECOND, 28);
        ui.button(610, 880, 280, 80, "ГОСТИ", App.A_QSIDE, 1, a.qSide == 1 ? Ui.ST_TABON : Ui.ST_SECOND, 28);
        ui.text("Длина матча: " + a.set.matchMin + " мин • Сложность: " + new String[]{"Новичок", "Профи", "Мировой класс", "Легенда"}[a.set.diff], 940, 930, 24, Ui.GRAY, 0, false);
        ui.button(vw - 460, 880, 400, 100, a.qHome == a.qAway ? "ВЫБЕРИТЕ РАЗНЫЕ" : "▶ ИГРАТЬ", App.A_QSTART, 0, a.qHome == a.qAway ? Ui.ST_SECOND : Ui.ST_GOOD, 36);
    }

    // ================================================================ SETTINGS
    void settings() {
        title("НАСТРОЙКИ", null);
        backBtn();
        App.Settings s = a.set;
        float y = 190;
        row("Длина матча (мин)", y, new String[]{"3", "5", "6", "8", "10"}, idx(new int[]{3, 5, 6, 8, 10}, s.matchMin), 0); y += 100;
        row("Сложность", y, new String[]{"Новичок", "Профи", "Мировой класс", "Легенда"}, s.diff, 1); y += 100;
        row("Время суток", y, new String[]{"Авто", "День", "Вечер", "Ночь"}, s.weather, 2); y += 100;
        row("Дождь", y, new String[]{"Нет", "Иногда", "Всегда"}, s.rain, 3); y += 100;
        row("Камера", y, new String[]{"Далеко", "Средне", "Близко"}, s.zoom, 4); y += 100;
        row("Звук", y, new String[]{"Выкл", "Вкл"}, s.sound ? 1 : 0, 5); y += 100;
        row("Вибрация", y, new String[]{"Выкл", "Вкл"}, s.vibr ? 1 : 0, 6); y += 100;
        float cx = vw / 2f + 60;
        float y2 = 190;
        row2("Радар", cx, y2, new String[]{"Выкл", "Вкл"}, s.radar ? 1 : 0, 7); y2 += 100;
        row2("Повторы голов", cx, y2, new String[]{"Выкл", "Вкл"}, s.replays ? 1 : 0, 8); y2 += 100;
        row2("Размер кнопок", cx, y2, new String[]{"S", "M", "L", "XL"}, s.ctlScale < 0.9f ? 0 : (s.ctlScale < 1.05f ? 1 : (s.ctlScale < 1.2f ? 2 : 3)), 9); y2 += 100;
        row2("Прозрачность кнопок", cx, y2, new String[]{"50%", "75%", "100%"}, s.ctlOpacity < 0.6f ? 0 : (s.ctlOpacity < 0.9f ? 1 : 2), 10);
    }
    int idx(int[] a, int v) { for (int i = 0; i < a.length; i++) if (a[i] == v) return i; return 0; }
    void row(String label, float y, String[] opts, int cur, int key) { row2(label, 60, y, opts, cur, key); }
    void row2(String label, float x, float y, String[] opts, int cur, int key) {
        ui.text(label, x, y + 40, 30, Ui.WHITE, 0);
        float bw = Math.min(190, (vw / 2f - 60 - 380) / opts.length);
        for (int i = 0; i < opts.length; i++) ui.button(x + 390 + i * (bw + 8), y, bw, 62, opts[i], App.A_SET, key * 10 + i, cur == i ? Ui.ST_TABON : Ui.ST_SECOND, 22);
    }

    void about() {
        title("ОБ ИГРЕ", null);
        backBtn();
        ui.glass(60, 170, vw - 120, 780, 30);
        String[] lines = {
                "YOTTA FC 26 — футбольный симулятор для Android.",
                "• Карьера: лига на 16 клубов, кубок, трансферные окна, финансы, развитие игроков.",
                "• Матч: 2.5D-камера, судьи и помощники, офсайды, фолы, карточки, пенальти, угловые.",
                "• Управление: левый стик — движение, УДАР (удерживать — сила), ПАС, ПРОРЕЗ, НАВЕС, БЕГ.",
                "• В обороне: ОТБОР, ПОДКАТ, СМЕНА игрока, ПРЕСС.",
                "• Повторы голов, замены, смена схем прямо в матче, серия пенальти.",
                "• Все клубы и игроки вымышлены."};
        for (int i = 0; i < lines.length; i++) ui.text(lines[i], 100, 250 + i * 80, 30, i == 0 ? Ui.GOLD : Ui.WHITE, 0, i != 0);
    }

    // ================================================================ MATCH OVERLAYS
    void replayOverlay(Canvas canvas) {
        c = canvas; vw = ui.vw; vh = ui.vh;
        ui.fill.setColor(0xCC000000); c.drawRect(0, 0, vw, 90, ui.fill); c.drawRect(0, vh - 90, vw, vh, ui.fill);
        ui.text("ПОВТОР", 80, 62, 48, Ui.GOLD, 0);
        ui.button(vw - 330, vh - 82, 280, 70, "ПРОПУСТИТЬ", App.A_RSKIP, 0, Ui.ST_SECOND, 28);
    }

    void pause(Canvas canvas) {
        c = canvas; vw = ui.vw; vh = ui.vh;
        ui.modalBegin();
        ui.fill.setColor(0xCC000000); c.drawRect(0, 0, vw, vh, ui.fill);
        Match m = a.match;
        float w = 760, x = vw / 2f - w / 2;
        if (a.dialog == App.D_CONFIRM) { dialogsMatch(); return; }
        if (a.pauseTab == 0) {
            ui.text("ПАУЗА", vw / 2f, 170, 72, Ui.WHITE, 1);
            ui.text(m.S[0].name + " " + m.S[0].score + " : " + m.S[1].score + " " + m.S[1].name, vw / 2f, 230, 34, Ui.GOLD, 1);
            float y = 280;
            ui.button(x, y, w, 100, "ПРОДОЛЖИТЬ", App.A_RESUME, 0, Ui.ST_GOOD, 38); y += 118;
            ui.button(x, y, w, 100, "ЗАМЕНЫ (" + (5 - m.S[a.userSide].subs) + ")", App.A_PSUBS, 0, Ui.ST_SECOND, 36); y += 118;
            ui.button(x, y, w, 100, "ТАКТИКА И СХЕМА", App.A_PTACT, 0, Ui.ST_SECOND, 36); y += 118;
            ui.button(x, y, w, 100, "НАСТРОЙКИ", App.A_PSET, 0, Ui.ST_SECOND, 36); y += 118;
            ui.button(x, y, w, 100, "ВЫЙТИ ИЗ МАТЧА", App.A_PQUIT, 0, Ui.ST_DANGER, 36);
        } else if (a.pauseTab == 1) subs(m);
        else if (a.pauseTab == 2) liveTactics(m);
        else {
            ui.text("НАСТРОЙКИ", vw / 2f, 130, 60, Ui.WHITE, 1);
            App.Settings s = a.set;
            row2("Звук", vw / 2f - 450, 200, new String[]{"Выкл", "Вкл"}, s.sound ? 1 : 0, 5);
            row2("Вибрация", vw / 2f - 450, 300, new String[]{"Выкл", "Вкл"}, s.vibr ? 1 : 0, 6);
            row2("Камера", vw / 2f - 450, 400, new String[]{"Далеко", "Средне", "Близко"}, s.zoom, 4);
            row2("Радар", vw / 2f - 450, 500, new String[]{"Выкл", "Вкл"}, s.radar ? 1 : 0, 7);
            ui.button(vw / 2f - 200, 700, 400, 90, "НАЗАД", App.A_PBACK, 0, Ui.ST_PRIMARY, 34);
        }
    }

    void dialogsMatch() {
        float w = 700, h = 330, x = vw / 2f - w / 2, y = vh / 2f - h / 2;
        ui.glass(x, y, w, h, 28);
        ui.text("Выйти из матча?", x + w / 2, y + 100, 44, Ui.WHITE, 1);
        ui.text("Результат не будет засчитан", x + w / 2, y + 150, 26, Ui.GRAY, 1, false);
        ui.button(x + 40, y + 200, 300, 90, "ДА", App.A_CONFYES, 0, Ui.ST_DANGER, 34);
        ui.button(x + w - 340, y + 200, 300, 90, "ОТМЕНА", App.A_CLOSE, 0, Ui.ST_SECOND, 34);
    }

    void subs(Match m) {
        Match.Side sd = m.S[a.userSide];
        ui.text("ЗАМЕНЫ", vw / 2f, 100, 60, Ui.WHITE, 1);
        ui.text("Осталось замен: " + (5 - sd.subs) + " • выберите игрока на поле и запасного", vw / 2f, 146, 26, Ui.GRAY, 1, false);
        float lx = 120, w = vw / 2f - 180;
        ui.text("НА ПОЛЕ", lx + w / 2, 200, 28, Ui.CYAN, 1);
        ui.scrollBegin(600, lx, 215, w, 640, 11 * 62 + 10);
        for (int i = 0; i < 11; i++) {
            Pawn p = sd.pw[i]; float y = 215 + i * 62;
            boolean sel = a.subOut == i;
            ui.fill.setColor(sel ? 0x66FFD54A : 0x16FFFFFF); ui.rc.set(lx, y, lx + w, y + 56); c.drawRoundRect(ui.rc, 14, 14, ui.fill);
            ui.text(Player.POS[p.role], lx + 50, y + 38, 22, Ui.CYAN, 1);
            ui.textFit(p.pl.shortName(), lx + 100, y + 38, w - 300, 26, Ui.WHITE, 0);
            ui.bar(lx + w - 200, y + 22, 90, 12, p.stamina, p.stamina > 0.5f ? Ui.GREEN : (p.stamina > 0.35f ? Ui.ORANGE : Ui.RED));
            ui.ovrBadge(lx + w - 50, y + 28, 20, p.pl.ovr());
            if (p.st == Pawn.SENT) ui.text("УДАЛЁН", lx + w - 240, y + 38, 20, Ui.RED, 2);
            ui.hit(lx, y, lx + w, y + 56, App.A_SUBOUT, i);
        }
        ui.scrollEnd();
        float rx = vw / 2f + 60;
        ui.text("СКАМЕЙКА", rx + w / 2, 200, 28, Ui.CYAN, 1);
        for (int i = 0; i < sd.bench.size(); i++) {
            Player p = sd.bench.get(i); float y = 215 + i * 62;
            boolean sel = a.subIn == p;
            ui.fill.setColor(sel ? 0x6622C55E : 0x16FFFFFF); ui.rc.set(rx, y, rx + w, y + 56); c.drawRoundRect(ui.rc, 14, 14, ui.fill);
            ui.text(Player.POS[p.pos], rx + 50, y + 38, 22, Ui.CYAN, 1);
            ui.textFit(p.shortName(), rx + 100, y + 38, w - 220, 26, Ui.WHITE, 0);
            ui.ovrBadge(rx + w - 50, y + 28, 20, p.ovr());
            ui.hit(rx, y, rx + w, y + 56, App.A_SUBIN, p.id);
        }
        ui.button(vw / 2f - 480, 920, 440, 90, "СДЕЛАТЬ ЗАМЕНУ", App.A_SUBDO, 0, a.subOut >= 0 && a.subIn != null ? Ui.ST_GOOD : Ui.ST_SECOND, 32);
        ui.button(vw / 2f + 40, 920, 440, 90, "НАЗАД", App.A_PBACK, 0, Ui.ST_PRIMARY, 32);
    }

    void liveTactics(Match m) {
        Match.Side sd = m.S[a.userSide];
        ui.text("СХЕМА И ТАКТИКА", vw / 2f, 110, 60, Ui.WHITE, 1);
        for (int i = 0; i < Data.FORM_NAME.length; i++) {
            ui.button(vw / 2f - 560 + i * 125, 170, 118, 62, Data.FORM_NAME[i], App.A_FORM + 1000, i, sd.formation == i ? Ui.ST_TABON : Ui.ST_SECOND, 22);
        }
        String[] kn = {"Стиль", "Прессинг", "Линия защиты", "Ширина", "Темп"};
        String[][] vn = {{"Оборона", "Баланс", "Атака"}, {"Низкий", "Средний", "Высокий"}, {"Низкая", "Средняя", "Высокая"}, {"Узко", "Нормально", "Широко"}, {"Медленно", "Нормально", "Быстро"}};
        int[] cur = {sd.style, sd.press, sd.line, sd.width, sd.tempo};
        for (int k = 0; k < 5; k++) {
            float y = 290 + k * 100;
            ui.text(kn[k], vw / 2f - 520, y + 44, 32, Ui.WHITE, 0);
            for (int v = 0; v < 3; v++) ui.button(vw / 2f - 120 + v * 230, y, 220, 70, vn[k][v], App.A_STYLE + 1000, k * 10 + v, cur[k] == v ? Ui.ST_TABON : Ui.ST_SECOND, 26);
        }
        ui.button(vw / 2f - 200, 850, 400, 90, "НАЗАД", App.A_PBACK, 0, Ui.ST_PRIMARY, 34);
    }

    void halftime(Canvas canvas) {
        c = canvas; vw = ui.vw; vh = ui.vh;
        ui.modalBegin();
        ui.fill.setColor(0xDD000000); c.drawRect(0, 0, vw, vh, ui.fill);
        Match m = a.match;
        String title = m.period == 0 ? "ПЕРЕРЫВ" : (m.period == 1 ? "ДОПОЛНИТЕЛЬНОЕ ВРЕМЯ" : (m.period == 2 ? "ПЕРЕРЫВ ДОП. ВРЕМЕНИ" : "СЕРИЯ ПЕНАЛЬТИ"));
        if (a.pauseTab == 1) { subs(m); return; }
        if (a.pauseTab == 2) { liveTactics(m); return; }
        ui.text(title, vw / 2f, 130, 72, Ui.WHITE, 1);
        ui.text(m.S[0].name + "  " + m.S[0].score + " : " + m.S[1].score + "  " + m.S[1].name, vw / 2f, 200, 44, Ui.GOLD, 1);
        statsTable(m, vw / 2f - 520, 250, 1040);
        ui.button(vw / 2f - 560, 900, 360, 90, "ЗАМЕНЫ", App.A_PSUBS, 0, Ui.ST_SECOND, 32);
        ui.button(vw / 2f - 180, 900, 360, 90, "ТАКТИКА", App.A_PTACT, 0, Ui.ST_SECOND, 32);
        ui.button(vw / 2f + 200, 900, 360, 90, "▶ ПРОДОЛЖИТЬ", App.A_HALFGO, 0, Ui.ST_GOOD, 32);
    }

    void statsTable(Match m, float x, float y, float w) {
        String[] names = {"Владение", "Удары", "В створ", "Угловые", "Фолы", "Офсайды", "Жёлтые", "Точность пасов"};
        Match.Side a0 = m.S[0], b0 = m.S[1];
        int pa = m.possessionPct(0);
        int[][] v = {{pa, 100 - pa}, {a0.shots, b0.shots}, {a0.sot, b0.sot}, {a0.corners, b0.corners}, {a0.fouls, b0.fouls}, {a0.offsides, b0.offsides}, {a0.yellows, b0.yellows},
                {a0.passes == 0 ? 0 : a0.passOK * 100 / a0.passes, b0.passes == 0 ? 0 : b0.passOK * 100 / b0.passes}};
        for (int i = 0; i < names.length; i++) {
            float yy = y + i * 74;
            ui.text(names[i], x + w / 2, yy + 26, 26, Ui.GRAY, 1, false);
            ui.text(v[i][0] + (i == 0 || i == 7 ? "%" : ""), x + 20, yy + 32, 36, Ui.WHITE, 0);
            ui.text(v[i][1] + (i == 0 || i == 7 ? "%" : ""), x + w - 20, yy + 32, 36, Ui.WHITE, 2);
            float tot = Math.max(1, v[i][0] + v[i][1]);
            float bw = w - 40;
            ui.fill.setColor(0x22FFFFFF); ui.rc.set(x + 20, yy + 42, x + 20 + bw, yy + 52); c.drawRoundRect(ui.rc, 5, 5, ui.fill);
            ui.fill.setColor(a0.c1); ui.rc.set(x + 20, yy + 42, x + 20 + bw * v[i][0] / tot, yy + 52); c.drawRoundRect(ui.rc, 5, 5, ui.fill);
            ui.fill.setColor(b0.c1); ui.rc.set(x + 20 + bw * v[i][0] / tot, yy + 42, x + 20 + bw, yy + 52); c.drawRoundRect(ui.rc, 5, 5, ui.fill);
        }
    }

    // ================================================================ RESULT
    void result() {
        Match m = a.match;
        if (m == null) { a.screen = App.S_MENU; return; }
        title("ПОЛНОЕ ВРЕМЯ", a.careerMatch && a.curFix != null ? a.career.weekName() : "Товарищеский матч");
        Match.Side h = m.S[0], aw = m.S[1];
        float cxm = vw / 2f;
        ui.glass(60, 150, vw - 120, 250, 30);
        ui.crest(h.team, 260, 270, 70); ui.crest(aw.team, vw - 260, 270, 70);
        ui.textFit(h.name, 260, 380, 360, 34, Ui.WHITE, 1); ui.textFit(aw.name, vw - 260, 380, 360, 34, Ui.WHITE, 1);
        ui.text(h.score + " : " + aw.score, cxm, 300, 130, Ui.GOLD, 1);
        if (m.shootout) ui.text("по пенальти " + h.penScore + ":" + aw.penScore, cxm, 345, 30, Ui.CYAN, 1);
        else if (m.period >= 3) ui.text("после доп. времени", cxm, 345, 26, Ui.GRAY, 1, false);
        // scorers
        float sy = 440;
        ui.glass(60, sy - 20, 560, 560, 28);
        ui.text("СОБЫТИЯ", 100, sy + 24, 28, Ui.CYAN, 0);
        int k = 0;
        for (Match.Evt e : m.events) {
            if (e.type == Match.E_SUB || e.type == Match.E_PEN) continue;
            if (k > 11) break;
            String ic = e.type == Match.E_GOAL ? "⚽" : (e.type == Match.E_OWN ? "⚽" : (e.type == Match.E_YEL ? "🟨" : (e.type == Match.E_RED ? "🟥" : "✚")));
            float y = sy + 70 + k * 40;
            ui.text(e.min + "'", 90, y, 24, Ui.GRAY, 0, false);
            ui.textFit((e.type == Match.E_YEL ? "ЖК " : e.type == Match.E_RED ? "КК " : e.type == Match.E_INJ ? "Травма " : "") + e.text, 160, y, 440, 24, e.side == 0 ? Ui.WHITE : Ui.CYAN, 0);
            k++;
        }
        statsTable(m, 660, sy - 20, 700 + (vw - 1920) * 0);
        // ratings
        float rx = 1400 + (vw - 1920);
        ui.glass(rx, sy - 20, vw - rx - 60, 560, 28);
        ui.text("ЛУЧШИЕ", rx + 30, sy + 24, 28, Ui.CYAN, 0);
        ArrayList<Stat> all = new ArrayList<Stat>();
        for (Match.Side s : m.S) for (Stat st : s.stats.values()) if (st.played) all.add(st);
        Collections.sort(all, new Comparator<Stat>() { public int compare(Stat p, Stat q) { return Float.compare(q.rating, p.rating); } });
        for (int i = 0; i < Math.min(6, all.size()); i++) {
            Stat st = all.get(i); float y = sy + 52 + i * 78;
            Match.Side s = m.S[m.S[0].stats.containsKey(st.pl) ? 0 : 1];
            ui.face(st.pl, rx + 62, y + 34, 28, s.c1);
            ui.textFit(st.pl.shortName(), rx + 106, y + 32, vw - rx - 330, 24, Ui.WHITE, 0);
            ui.text(st.goals > 0 ? st.goals + "⚽" : "", rx + 106, y + 58, 20, Ui.GOLD, 0);
            ui.fill.setColor(i == 0 ? Ui.GOLD : 0x55FFFFFF); ui.rc.set(vw - 60 - 100, y + 8, vw - 80, y + 56); c.drawRoundRect(ui.rc, 14, 14, ui.fill);
            ui.text(String.format("%.1f", st.rating), vw - 130, y + 44, 30, i == 0 ? 0xFF10152A : Ui.WHITE, 1);
            if (i == 0) ui.text("ЛУЧШИЙ", rx + vw - rx - 130 - 110 + 60, y + 16, 14, Ui.GOLD, 1);
        }
        ui.button(vw / 2f - 220, 990, 440, 80, "ПРОДОЛЖИТЬ", App.A_RESULTOK, 0, Ui.ST_GOOD, 34);
    }
}
