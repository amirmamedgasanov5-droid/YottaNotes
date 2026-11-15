package com.yotta.fc;

import java.util.ArrayList;

/** Static game data and procedural generators. */
final class Data {
    // ---------- formations ----------
    static final String[] FORM_NAME = {"4-4-2", "4-3-3", "4-2-3-1", "4-1-4-1", "3-5-2", "3-4-3", "5-3-2", "4-1-2-1-2", "4-5-1"};
    static final int G = Player.GK, CB = Player.CB, LB = Player.LB, RB = Player.RB, CDM = Player.CDM, CM = Player.CM,
            CAM = Player.CAM, LM = Player.LM, RM = Player.RM, LW = Player.LW, RW = Player.RW, ST = Player.ST;
    static final int[][] FORM_ROLE = {
            {G, LB, CB, CB, RB, LM, CM, CM, RM, ST, ST},
            {G, LB, CB, CB, RB, CM, CDM, CM, LW, ST, RW},
            {G, LB, CB, CB, RB, CDM, CDM, LM, CAM, RM, ST},
            {G, LB, CB, CB, RB, CDM, LM, CM, CM, RM, ST},
            {G, CB, CB, CB, LM, CM, CDM, CM, RM, ST, ST},
            {G, CB, CB, CB, LM, CM, CM, RM, LW, ST, RW},
            {G, LB, CB, CB, CB, RB, CM, CDM, CM, ST, ST},
            {G, LB, CB, CB, RB, CDM, CM, CM, CAM, ST, ST},
            {G, LB, CB, CB, RB, LM, CM, CDM, CM, RM, ST}};
    static final float[][] FORM_U = {
            {.03f, .22f, .20f, .20f, .22f, .50f, .45f, .45f, .50f, .74f, .74f},
            {.03f, .23f, .20f, .20f, .23f, .48f, .40f, .48f, .72f, .78f, .72f},
            {.03f, .23f, .20f, .20f, .23f, .38f, .38f, .60f, .58f, .60f, .78f},
            {.03f, .23f, .20f, .20f, .23f, .36f, .55f, .50f, .50f, .55f, .76f},
            {.03f, .20f, .18f, .20f, .50f, .45f, .36f, .45f, .50f, .74f, .74f},
            {.03f, .20f, .18f, .20f, .50f, .45f, .45f, .50f, .72f, .78f, .72f},
            {.03f, .30f, .20f, .17f, .20f, .30f, .50f, .40f, .50f, .74f, .74f},
            {.03f, .23f, .20f, .20f, .23f, .36f, .48f, .48f, .60f, .76f, .76f},
            {.03f, .23f, .20f, .20f, .23f, .50f, .47f, .38f, .47f, .50f, .76f}};
    static final float[][] FORM_V = {
            {.5f, .10f, .37f, .63f, .90f, .08f, .38f, .62f, .92f, .38f, .62f},
            {.5f, .10f, .37f, .63f, .90f, .30f, .50f, .70f, .12f, .50f, .88f},
            {.5f, .10f, .37f, .63f, .90f, .35f, .65f, .10f, .50f, .90f, .50f},
            {.5f, .10f, .37f, .63f, .90f, .50f, .08f, .35f, .65f, .92f, .50f},
            {.5f, .25f, .50f, .75f, .06f, .35f, .50f, .65f, .94f, .38f, .62f},
            {.5f, .25f, .50f, .75f, .06f, .38f, .62f, .94f, .14f, .50f, .86f},
            {.5f, .06f, .30f, .50f, .70f, .94f, .30f, .50f, .70f, .38f, .62f},
            {.5f, .10f, .37f, .63f, .90f, .50f, .28f, .72f, .50f, .38f, .62f},
            {.5f, .10f, .37f, .63f, .90f, .07f, .33f, .50f, .67f, .93f, .50f}};

    // ---------- nations ----------
    static final String[] NAT_CODE = {"ENG", "ESP", "BRA", "ARG", "FRA", "GER", "ITA", "POR", "NED", "RUS", "TUR", "CRO", "NGA", "SEN", "JPN", "USA", "KOR", "MEX", "BEL", "POL"};
    // flag: type(0 horiz,1 vert,2 cross,3 disc,4 brazil), c1,c2,c3
    static final int[][] NAT_FLAG = {
            {2, 0xFFFFFFFF, 0xFFCE1126, 0}, {0, 0xFFC60B1E, 0xFFFFC400, 0xFFC60B1E}, {4, 0xFF009C3B, 0xFFFFDF00, 0xFF002776},
            {0, 0xFF74ACDF, 0xFFFFFFFF, 0xFF74ACDF}, {1, 0xFF0055A4, 0xFFFFFFFF, 0xFFEF4135}, {0, 0xFF111111, 0xFFDD0000, 0xFFFFCE00},
            {1, 0xFF009246, 0xFFFFFFFF, 0xFFCE2B37}, {1, 0xFF006600, 0xFFFF0000, 0xFFFF0000}, {0, 0xFFAE1C28, 0xFFFFFFFF, 0xFF21468B},
            {0, 0xFFFFFFFF, 0xFF0039A6, 0xFFD52B1E}, {3, 0xFFE30A17, 0xFFFFFFFF, 0}, {0, 0xFFFF0000, 0xFFFFFFFF, 0xFF171796},
            {1, 0xFF008751, 0xFFFFFFFF, 0xFF008751}, {1, 0xFF00853F, 0xFFFDEF42, 0xFFE31B23}, {3, 0xFFFFFFFF, 0xFFBC002D, 0},
            {0, 0xFFB22234, 0xFFFFFFFF, 0xFF3C3B6E}, {3, 0xFFFFFFFF, 0xFFCD2E3A, 0}, {1, 0xFF006847, 0xFFFFFFFF, 0xFFCE1126},
            {1, 0xFF111111, 0xFFFDDA24, 0xFFEF3340}, {0, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFDC143C}};
    // skin tone weights (6 tones) per nation
    static final int[][] NAT_SKIN = {
            {40, 30, 15, 8, 5, 2}, {40, 38, 15, 5, 2, 0}, {10, 20, 25, 20, 15, 10}, {35, 40, 18, 5, 2, 0}, {25, 25, 15, 10, 15, 10},
            {55, 30, 10, 3, 2, 0}, {40, 40, 15, 5, 0, 0}, {35, 40, 18, 5, 2, 0}, {55, 30, 8, 3, 2, 2}, {65, 25, 8, 2, 0, 0},
            {30, 45, 20, 5, 0, 0}, {60, 30, 8, 2, 0, 0}, {0, 0, 0, 10, 35, 55}, {0, 0, 0, 5, 30, 65}, {25, 55, 20, 0, 0, 0},
            {30, 25, 18, 12, 10, 5}, {25, 55, 20, 0, 0, 0}, {10, 40, 35, 15, 0, 0}, {50, 30, 10, 5, 3, 2}, {70, 25, 5, 0, 0, 0}};
    static final String[][] FIRST = {
            {"Jack", "Harry", "Oliver", "Callum", "Mason", "Declan", "Jude", "Luke", "Reece", "Tyler", "Ben", "Jordan"},
            {"Pablo", "Sergio", "Diego", "Álvaro", "Iker", "Marc", "Dani", "Adrián", "Raúl", "Jesús", "Unai", "Mikel"},
            {"Rafael", "Thiago", "Gabriel", "Lucas", "Matheus", "Vinícius", "Endrick", "Caio", "Rodrygo", "Bruno", "Fabinho", "Éder"},
            {"Lautaro", "Julián", "Enzo", "Nicolás", "Matías", "Facundo", "Thiago", "Emiliano", "Franco", "Alexis", "Rodrigo", "Santiago"},
            {"Kylian", "Antoine", "Hugo", "Théo", "Lucas", "Mathis", "Olivier", "Raphaël", "Youssef", "Nolan", "Dayot", "Aurélien"},
            {"Jamal", "Kai", "Leon", "Florian", "Niklas", "Joshua", "Serge", "Timo", "Lukas", "Felix", "Jonas", "Maximilian"},
            {"Federico", "Nicolò", "Lorenzo", "Gianluca", "Marco", "Matteo", "Alessandro", "Davide", "Andrea", "Riccardo", "Sandro", "Luca"},
            {"João", "Rúben", "Bernardo", "Diogo", "Nuno", "Gonçalo", "Rafael", "Vitinha", "Pedro", "Tiago", "André", "Miguel"},
            {"Virgil", "Frenkie", "Memphis", "Cody", "Matthijs", "Denzel", "Xavi", "Teun", "Jurriën", "Tijjani", "Noa", "Sven"},
            {"Артём", "Фёдор", "Никита", "Матвей", "Егор", "Кирилл", "Данила", "Роман", "Максим", "Алексей", "Игорь", "Иван"},
            {"Arda", "Hakan", "Kerem", "Emre", "Burak", "Cenk", "Merih", "Orkun", "Ferdi", "Yusuf", "Zeki", "Kaan"},
            {"Luka", "Ivan", "Mateo", "Josip", "Marcelo", "Andrej", "Borna", "Dominik", "Nikola", "Lovro", "Mario", "Petar"},
            {"Victor", "Samuel", "Wilfred", "Kelechi", "Ademola", "Moses", "Chidera", "Taiwo", "Calvin", "Ola", "Emeka", "Tunde"},
            {"Sadio", "Idrissa", "Kalidou", "Ismaïla", "Cheikhou", "Boulaye", "Pape", "Nampalys", "Abdou", "Moussa", "Famara", "Lamine"},
            {"Takumi", "Kaoru", "Daichi", "Ritsu", "Hiroki", "Kyogo", "Shuto", "Yuto", "Ayase", "Junya", "Takehiro", "Koki"},
            {"Christian", "Weston", "Tyler", "Gio", "Brenden", "Josh", "Ricardo", "Yunus", "Haji", "Jesús", "Cole", "Matt"},
            {"Min-jae", "Heung-min", "Kang-in", "Hee-chan", "Jae-sung", "Woo-yeong", "Seung-ho", "Gue-sung", "Jun-ho", "Tae-hwan", "Dong-jun", "Young-woo"},
            {"Hirving", "Raúl", "Edson", "Santiago", "Luis", "Orbelín", "Jorge", "César", "Johan", "Alexis", "Diego", "Henry"},
            {"Kevin", "Romelu", "Youri", "Jérémy", "Thorgan", "Leandro", "Yannick", "Amadou", "Dodi", "Charles", "Timothy", "Arthur"},
            {"Robert", "Piotr", "Wojciech", "Kamil", "Jakub", "Arkadiusz", "Karol", "Sebastian", "Bartosz", "Krzysztof", "Przemysław", "Nicola"}};
    static final String[][] LAST = {
            {"Walker", "Bellamy", "Foster", "Hartley", "Grealish", "Mount", "Rice", "Stones", "Palmer", "Wilson", "Carter", "Dawson"},
            {"Navarro", "Moreno", "Iglesias", "Ortega", "Cabrera", "Villalba", "Ramos", "Torres", "Garrido", "Sanz", "Herrera", "Pedraza"},
            {"Silva", "Santos", "Oliveira", "Barbosa", "Cardoso", "Nascimento", "Pereira", "Rocha", "Medeiros", "Teixeira", "Araújo", "Fonseca"},
            {"Martínez", "Fernández", "Álvarez", "Paredes", "Romero", "Acuña", "Molina", "Correa", "Lisandro", "Benítez", "Montiel", "Otamendi"},
            {"Mbappé", "Dubois", "Lefèvre", "Moreau", "Laurent", "Girard", "Rousseau", "Fontaine", "Marchand", "Bonnet", "Tchouaméni", "Colin"},
            {"Müller", "Keller", "Hoffmann", "Brandt", "Schäfer", "Richter", "Wagner", "Lehmann", "Krause", "Vogel", "Neumann", "Beck"},
            {"Rossi", "Bianchi", "Conti", "Barella", "Bellini", "Moretti", "De Luca", "Ferraro", "Greco", "Lombardi", "Pellegrini", "Costa"},
            {"Fernandes", "Carvalho", "Mendes", "Ribeiro", "Pinto", "Monteiro", "Gomes", "Coelho", "Nunes", "Leal", "Matos", "Cunha"},
            {"van Dijk", "de Jong", "Bakker", "Visser", "Smit", "Mulder", "de Vries", "Koopmeiners", "Veltman", "Brouwer", "Timber", "Dumfries"},
            {"Соколов", "Морозов", "Волков", "Зайцев", "Орлов", "Лебедев", "Крылов", "Беляев", "Фролов", "Захаров", "Громов", "Назаров"},
            {"Yılmaz", "Demir", "Çelik", "Kaya", "Aydın", "Öztürk", "Şahin", "Yıldız", "Koç", "Arslan", "Çalhanoğlu", "Güler"},
            {"Kovačić", "Perišić", "Brozović", "Gvardiol", "Majer", "Livaja", "Sučić", "Vlašić", "Pašalić", "Ćorluka", "Stanišić", "Budimir"},
            {"Osimhen", "Iheanacho", "Ndidi", "Lookman", "Aribo", "Chukwueze", "Onuachu", "Awoniyi", "Bassey", "Adeyemi", "Okafor", "Eze"},
            {"Diallo", "Gueye", "Mendy", "Sarr", "Koulibaly", "Diatta", "Ndiaye", "Cissé", "Sow", "Mané", "Fall", "Camara"},
            {"Mitoma", "Tanaka", "Kubo", "Endo", "Doan", "Maeda", "Furuhashi", "Itakura", "Minamino", "Sasaki", "Ito", "Kamada"},
            {"Pulisic", "Adams", "McKennie", "Reyna", "Musah", "Weah", "Robinson", "Dest", "Aaronson", "Turner", "Zimmerman", "Ream"},
            {"Kim", "Son", "Lee", "Hwang", "Cho", "Park", "Jung", "Seol", "Hong", "Yoon", "Kwon", "Baek"},
            {"Lozano", "Jiménez", "Álvarez", "Giménez", "Chávez", "Vega", "Antuna", "Pineda", "Guardado", "Montes", "Romo", "Quiñones"},
            {"De Bruyne", "Lukaku", "Tielemans", "Doku", "Onana", "Trossard", "Castagne", "Vertonghen", "Meunier", "Openda", "Witsel", "Mangala"},
            {"Lewandowski", "Zieliński", "Szczęsny", "Milik", "Bednarek", "Krychowiak", "Piątek", "Moder", "Frankowski", "Cash", "Świderski", "Buksa"}};

    // ---------- clubs ----------
    static final String[] CLUB_NAME = {"Yotta FC", "Northgate United", "Valmora FC", "Kronberg Athletic", "Solano CF", "Iron Harbor",
            "Mistral Lions", "Astoria Rovers", "Redstone Dynamo", "Lumen Stars", "Cobalt Wanderers", "Emberfall", "Verdant Hills",
            "Skyline SC", "Obsidian FC", "Rosewood Town"};
    static final String[] CLUB_SHORT = {"YOT", "NGU", "VAL", "KRO", "SOL", "IRH", "MIS", "AST", "RED", "LUM", "COB", "EMB", "VER", "SKY", "OBS", "ROS"};
    static final String[] CLUB_CITY = {"Yottagrad", "Northgate", "Valmora", "Kronberg", "Solano", "Iron Harbor", "Mistral", "Astoria", "Redstone", "Lumen",
            "Cobalt Bay", "Emberfall", "Verdant", "Skyline", "Obsidian", "Rosewood"};
    static final String[] CLUB_STAD = {"Yotta Arena", "Northgate Park", "Estadio Valmora", "Kronberg Dome", "Solano Sol", "Harbor Yard",
            "Mistral Stadium", "Astoria Ground", "Dynamo Arena", "Star Lights", "Cobalt Field", "Ember Cauldron", "Hills Park", "Skyline Bowl", "Obsidian Pit", "Rosewood Lane"};
    static final int[][] CLUB_COL = {
            {0xFF8A3FFC, 0xFF22D3EE, 0xFFFFFFFF}, {0xFFE11D2E, 0xFFFFFFFF, 0xFF0B1F4D}, {0xFF6CB4EE, 0xFFFFFFFF, 0xFF0B1F4D},
            {0xFF141414, 0xFFFACC15, 0xFFFFFFFF}, {0xFFF97316, 0xFF111111, 0xFFFFFFFF}, {0xFF1E3A8A, 0xFF94A3B8, 0xFFFFFFFF},
            {0xFF16A34A, 0xFFFFFFFF, 0xFF064E3B}, {0xFF7F1D1D, 0xFF7DD3FC, 0xFFFFFFFF}, {0xFFDC2626, 0xFF111111, 0xFFFFFFFF},
            {0xFFFDE047, 0xFF1D4ED8, 0xFF111111}, {0xFF2563EB, 0xFFFFFFFF, 0xFF0B1F4D}, {0xFFC2410C, 0xFF0F172A, 0xFFFFFFFF},
            {0xFF059669, 0xFFFBBF24, 0xFFFFFFFF}, {0xFFF8FAFC, 0xFF38BDF8, 0xFF0B1F4D}, {0xFF0A0A0A, 0xFFC0C0C8, 0xFFFFFFFF}, {0xFFF472B6, 0xFF6D28D9, 0xFFFFFFFF}};
    static final int[] CLUB_PAT = {1, 0, 1, 2, 0, 3, 0, 3, 1, 0, 4, 5, 2, 1, 0, 2};
    static final int[] CLUB_RATING = {76, 82, 84, 83, 79, 77, 78, 75, 80, 76, 74, 75, 73, 79, 77, 72};
    static final int[] CLUB_CAP = {42000, 68000, 72000, 61000, 48000, 38000, 44000, 33000, 55000, 36000, 31000, 34000, 29000, 46000, 40000, 27000};
    static final String[] EXT_CLUBS = {"Zenit Alpha", "Dinamo Vega", "Sporting Nova", "Atlético Mar", "Lokomotiv Sud", "Borussia Nord", "Inter Alba", "Hajduk Plava",
            "Ajax Delta", "Galata Gold", "Celtic Verde", "Benfica Rio", "Santos Bay", "Boca Azul", "River Plata", "Flamengo Sol", "Marseille Port", "Lyon Rhône",
            "Sevilla Sol", "Feyenoord Zuid", "Anderlecht Brux", "Red Star Nis", "Shakhtar Don", "Besiktas Bos", "Cruz Azul MX", "Seoul Han", "Kawasaki Blue", "Al Nasr"};
    static final String[] SPONSORS = {"YOTTA", "NOVA BANK", "AEROLINE", "VOLT", "TERRA CAR", "PULSE", "ZENITH", "KINETIC", "ORBIT", "FLUX"};

    // ---------- generation ----------
    static int nextId = 1;

    static int pickSkin(int nat) {
        int[] w = NAT_SKIN[nat];
        int tot = 0;
        for (int v : w) tot += v;
        int r = U.R.nextInt(Math.max(1, tot)), a = 0;
        for (int i = 0; i < 6; i++) { a += w[i]; if (r < a) return i; }
        return 0;
    }

    static Player makePlayer(int nat, int pos, int ovr, int age) {
        Player p = new Player();
        p.id = nextId++;
        p.nat = nat;
        p.first = FIRST[nat][U.R.nextInt(FIRST[nat].length)];
        p.last = LAST[nat][U.R.nextInt(LAST[nat].length)];
        p.age = age;
        p.pos = pos;
        if (pos == Player.LB && U.chance(.25f)) p.pos2 = Player.RB;
        else if (pos == Player.RB && U.chance(.25f)) p.pos2 = Player.LB;
        else if (pos == Player.CM && U.chance(.3f)) p.pos2 = U.chance(.5f) ? Player.CDM : Player.CAM;
        else if (pos == Player.LW && U.chance(.3f)) p.pos2 = Player.LM;
        else if (pos == Player.RW && U.chance(.3f)) p.pos2 = Player.RM;
        else if (pos == Player.ST && U.chance(.2f)) p.pos2 = Player.CAM;
        else if (pos == Player.CAM && U.chance(.3f)) p.pos2 = Player.CM;
        shapeAttrs(p, ovr);
        p.pot = age < 24 ? Math.min(96, ovr + U.ri(0, (24 - age) * 3)) : Math.min(95, ovr + U.ri(0, 2));
        p.skin = pickSkin(nat);
        p.hairStyle = U.R.nextInt(8);
        if (p.skin >= 4 && U.chance(.5f)) p.hairStyle = U.pick(new Integer[]{0, 1, 1, 5, 6});
        p.hairColor = p.skin >= 4 ? U.pick(new Integer[]{0, 0, 0, 1}) : U.pick(new Integer[]{0, 1, 1, 2, 2, 3, 4, 5});
        p.beard = U.chance(.22f) ? 1 : 0;
        p.height = pos == Player.GK ? U.ri(184, 200) : (pos == Player.CB ? U.ri(178, 196) : U.ri(166, 190));
        p.form = 0;
        p.fitness = 100;
        p.morale = 70;
        p.contract = U.ri(1, 5);
        p.recalcValue();
        return p;
    }

    static void shapeAttrs(Player p, int ovr) {
        int[] pr = Player.PROFILE[p.pos];
        int[] a = new int[6];
        for (int i = 0; i < 6; i++) a[i] = pr[i] + U.ri(-5, 5);
        // scale to target ovr
        for (int it = 0; it < 8; it++) {
            float[] w = Player.W[p.pos];
            float cur = 0;
            if (p.pos == Player.GK) cur = 0; else for (int i = 0; i < 6; i++) cur += a[i] * w[i];
            if (p.pos == Player.GK) break;
            float d = ovr - cur;
            if (Math.abs(d) < 0.6f) break;
            for (int i = 0; i < 6; i++) a[i] = U.clampi(Math.round(a[i] + d * (0.8f + 0.5f * w[i] * 4) / 1.2f), 25, 97);
        }
        p.pac = a[0]; p.sho = a[1]; p.pas = a[2]; p.dri = a[3]; p.def = a[4]; p.phy = a[5];
        if (p.pos == Player.GK) {
            p.gk = U.clampi(Math.round(ovr / 0.92f - 1), 30, 97);
            p.pac = U.clampi(ovr - 25 + U.ri(-6, 6), 25, 80);
            p.sho = U.clampi(ovr - 40 + U.ri(-6, 6), 20, 60);
            p.pas = U.clampi(ovr - 20 + U.ri(-8, 8), 25, 80);
            p.dri = U.clampi(ovr - 35 + U.ri(-8, 8), 20, 70);
            p.def = U.clampi(ovr - 30, 20, 70);
            p.phy = U.clampi(ovr - 10 + U.ri(-8, 8), 40, 90);
        } else {
            p.gk = U.ri(8, 22);
        }
        p.sta = U.clampi(p.phy + U.ri(-12, 14) + (p.age < 30 ? 4 : -2), 35, 97);
    }

    static final int[] SQUAD_POS = {Player.GK, Player.GK, Player.GK, Player.CB, Player.CB, Player.CB, Player.CB, Player.LB, Player.LB, Player.RB, Player.RB,
            Player.CDM, Player.CDM, Player.CM, Player.CM, Player.CM, Player.CAM, Player.CAM, Player.LM, Player.RM, Player.LW, Player.RW, Player.ST, Player.ST, Player.ST};

    static void genSquad(Team t) {
        int[] cnt = new int[12];
        for (int i = 0; i < SQUAD_POS.length; i++) {
            int pos = SQUAD_POS[i];
            int idx = cnt[pos]++;
            int natW = U.R.nextInt(100);
            int nat = natW < 40 ? ((t.id * 7) % 20 + U.ri(-1, 1) + 20) % 20 : U.R.nextInt(20);
            int ovr = t.rating + U.ri(-3, 3) - idx * (pos == Player.GK ? 6 : 4) - (i > 18 ? 1 : 0);
            if (pos == Player.GK) ovr -= 1;
            int age = idx >= 2 || (pos != Player.GK && U.chance(.2f)) ? U.ri(18, 23) : U.ri(23, 34);
            ovr = U.clampi(ovr, 48, 91);
            Player p = makePlayer(nat, pos, ovr, age);
            p.teamId = t.id;
            t.squad.add(p);
        }
        assignNumbers(t);
        t.autoLineup();
    }

    static void assignNumbers(Team t) {
        boolean[] used = new boolean[100];
        for (Player p : t.squad) if (p.number > 0 && p.number < 100) used[p.number] = true;
        for (Player p : t.squad) {
            if (p.number > 0) continue;
            int[] pref;
            switch (p.pos) {
                case Player.GK: pref = new int[]{1, 12, 13, 25}; break;
                case Player.CB: pref = new int[]{4, 5, 3, 15, 23, 24}; break;
                case Player.LB: pref = new int[]{3, 13, 18, 32}; break;
                case Player.RB: pref = new int[]{2, 12, 22, 33}; break;
                case Player.CDM: pref = new int[]{6, 14, 16, 21}; break;
                case Player.CM: pref = new int[]{8, 14, 16, 20, 28}; break;
                case Player.CAM: pref = new int[]{10, 8, 17, 21}; break;
                case Player.LM: case Player.LW: pref = new int[]{11, 7, 17, 19, 29}; break;
                case Player.RM: case Player.RW: pref = new int[]{7, 11, 19, 27, 30}; break;
                default: pref = new int[]{9, 19, 18, 20, 23, 14};
            }
            int n = -1;
            for (int c : pref) if (!used[c]) { n = c; break; }
            if (n < 0) { n = U.ri(2, 99); while (used[n]) n = U.ri(2, 99); }
            used[n] = true;
            p.number = n;
        }
    }

    static Team makeClub(int i) {
        Team t = new Team();
        t.id = i;
        t.name = CLUB_NAME[i]; t.shortName = CLUB_SHORT[i]; t.city = CLUB_CITY[i]; t.stadium = CLUB_STAD[i];
        t.c1 = CLUB_COL[i][0]; t.c2 = CLUB_COL[i][1]; t.c3 = CLUB_COL[i][2];
        t.pattern = CLUB_PAT[i]; t.crestShape = i % 4; t.crestSym = (i * 3 + 1) % 6;
        t.rating = CLUB_RATING[i];
        t.capacity = CLUB_CAP[i];
        t.cash = 10 + (t.rating - 70) * 6;
        t.formation = new int[]{1, 0, 2, 1, 3, 1, 2, 0, 1, 4, 0, 2, 1, 7, 6, 8}[i];
        genSquad(t);
        t.autoLineup();
        return t;
    }

    static ArrayList<Player> genMarket(int n) {
        ArrayList<Player> list = new ArrayList<Player>();
        for (int i = 0; i < n; i++) {
            int pos = SQUAD_POS[U.R.nextInt(SQUAD_POS.length)];
            int age = U.ri(18, 35);
            int ovr = U.clampi((int) (66 + U.gauss() * 6), 52, 88);
            Player p = makePlayer(U.R.nextInt(20), pos, ovr, age);
            p.teamId = -2;
            p.extClub = EXT_CLUBS[U.R.nextInt(EXT_CLUBS.length)];
            list.add(p);
        }
        return list;
    }

    static ArrayList<Player> genFreeAgents(int n) {
        ArrayList<Player> list = new ArrayList<Player>();
        for (int i = 0; i < n; i++) {
            int pos = SQUAD_POS[U.R.nextInt(SQUAD_POS.length)];
            int age = U.ri(19, 37);
            int ovr = U.clampi((int) (62 + U.gauss() * 6), 48, 80);
            Player p = makePlayer(U.R.nextInt(20), pos, ovr, age);
            p.teamId = -1;
            p.value = Math.round(p.value * 0.4f * 100) / 100f;
            list.add(p);
        }
        return list;
    }

    static Player makeYouth(Team t, int pos, int lvl) {
        int ovr = U.clampi(48 + lvl * 3 + U.ri(0, 8), 45, 70);
        Player p = makePlayer((t.id * 7) % 20, pos, ovr, U.ri(16, 18));
        p.pot = U.clampi(ovr + U.ri(8, 18 + lvl * 3), 60, 94);
        p.contract = 3;
        p.recalcValue();
        return p;
    }
}
