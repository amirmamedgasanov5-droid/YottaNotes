#!/usr/bin/env python3
"""Script v3: 'Амир и орёл'. Fast, action-first. (key, who, ru, az, pause_after)"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
SCENES = [
 (0.8, 'prolog', [('x0', 'X', 'Пародия. Все персонажи выдуманы.', 'Parodiya. Bütün personajlar uydurmadır.', .2)]),
 (6.0, 'home', [
  ('v1', 'V', 'Бой века! Хабиб против Конора! Билеты кончились, попкорн кончился, терпение кончилось!', 'Əsrin döyüşü! Xabib Conora qarşı! Biletlər bitdi, popkorn bitdi, səbr bitdi!', .4),
  ('a1', 'A', 'Мам, я иду на бой!', 'Ana, mən döyüşə gedirəm!', .2),
  ('m1', 'M', 'Куда?! У тебя билета нет!', 'Hara?! Sənin biletin yoxdur!', .2),
  ('a2', 'A', 'Зато у меня есть характер и бутерброд.', 'Amma mənim xarakterim və sendviçim var.', .3),
  ('m2', 'M', 'Бутерброд бери. Характер оставь дома.', 'Sendviçi götür. Xarakteri evdə burax.', .3),
  ('a3', 'A', 'Я буду тренером Хабиба!', 'Mən Xabibin məşqçisi olacağam!', .2),
  ('m3', 'M', 'А Хабиб в курсе?', 'Xabib xəbərdardır?', .2),
  ('a4', 'A', 'Узнает в клетке!', 'Qəfəsdə biləcək!', 2.6)]),
 (None, 'weigh', [
  ('x1', 'X', 'Взвешивание.', 'Çəkiyə çıxma.', .2),
  ('v2', 'V', 'Конор пришёл в костюме на два размера меньше! Это называется «стиль».', 'Conor iki ölçü kiçik kostyumda gəlib! Buna «stil» deyirlər.', .3),
  ('c1', 'C', 'Я вешу триста килограммов славы!', 'Mən üç yüz kiloqram şöhrət çəkirəm!', 1.6),
  ('r1', 'R', 'Весы сломались. Они потеряли дар речи.', 'Tərəzi sındı. Danışmaq qabiliyyətini itirdi.', .3),
  ('k1', 'K', 'Мой вес нормальный. Настроение тоже.', 'Mənim çəkim normaldır. Əhval-ruhiyyəm də.', .3),
  ('g1', 'G', 'Я не сплю! Хр-р... Я охраняю глазами внутрь.', 'Mən yatmıram! Xır-r... Mən gözlərimlə içəridən qoruyuram.', .3),
  ('r2', 'R', 'Гляделки! Кто моргнёт, тот ест пирожок.', 'Baxış yarışı! Kim göz qırparsa, piroq yeyir.', 2.4),
  ('r3', 'R', 'Я моргнул. Давайте сюда пирожок.', 'Mən göz qırpdım. Piroqu ver.', .6)]),
 (None, 'fight', [
  ('x2', 'X', 'Клетка. Бой века.', 'Qəfəs. Əsrin döyüşü.', .2),
  ('v3', 'V', 'Раунд первый! Девушка с табличкой идёт по рингу, а за ней — ураган!', 'Birinci raund! Lövhəli qız rinqdə yeriyir, arxasında isə qasırğa!', .3),
  ('r4', 'R', 'Бой!', 'Döyüş!', .4),
  ('v4', 'V', 'Конор налетает! Джеб, кросс, хук! Мимо, мимо, мимо!', 'Conor hücum edir! Cəb, kros, huk! Boş, boş, boş!', 3.0),
  ('c2', 'C', 'Получай лоукик! Ай! Моя нога! Я сам себя пнул!', 'Al lou-kik! Ay! Mənim ayağım! Özümü təpikləmişəm!', 1.0),
  ('s1', 'S', 'Конор, дыши! Это не соревнование по болтовне!', 'Conor, nəfəs al! Bu boşboğazlıq yarışı deyil!', 1.0),
  ('a5', 'A', 'Хабиб! Заходи в ноги!', 'Xabib! Ayaqlara gir!', .2),
  ('k2', 'K', 'Сам знаю.', 'Özüm bilirəm.', .3),
  ('v5', 'V', 'Заход в ноги! Поднял! Крутит! Кладёт! Бам!', 'Ayaqlara giriş! Qaldırdı! Fırladır! Qoyur! Bam!', 3.0),
  ('r5', 'R', 'Раз, два, три, пять, девятнадцать!', 'Bir, iki, üç, beş, on doqquz!', .3),
  ('c3', 'C', 'Слезь с меня! Я не диван!', 'Düş üstümdən! Mən divan deyiləm!', .2),
  ('k3', 'K', 'Диван. Мягкий.', 'Divan. Yumşaq.', 1.4),
  ('y1', 'Y', 'Раунд два! Если вы ещё живы!', 'İkinci raund! Əgər hələ sağsınızsa!', 1.0),
  ('v6', 'V', 'Хабиб достал чай и сидит на Конор как на лавочке!', 'Xabib çay çıxardı və Conorun üstündə skamyada kimi oturur!', .3),
  ('v7', 'V', 'А на ринг выбежал болельщик с шашлыком!', 'Rinqə isə kabablı azarkeş qaçdı!', .3),
  ('g2', 'G', 'Стоять! Хр-р... Стоять... хр-р.', 'Dayan! Xır-r... Dayan... xır-r.', 3.0),
  ('c4', 'C', 'Я убегаю! Я альпинист!', 'Mən qaçıram! Mən alpinistəm!', .3),
  ('s2', 'S', 'Конор, слезай! Там сетка, а не лестница!', 'Conor, düş! Orada tor var, pilləkən yox!', .3),
  ('v8', 'V', 'Конор лезет на сетку! Сетка не против!', 'Conor torla qalxır! Tor etiraz etmir!', 3.0),
  ('k4', 'K', 'Тогда я полечу.', 'Onda mən uçaram.', 1.4),
  ('v9', 'V', 'ОРЁЛ! Орёл расправил крылья! Он летит!', 'QARTAL! Qartal qanadlarını açdı! O uçur!', 4.0),
  ('c5', 'C', 'Я не готов к орлу!', 'Mən qartala hazır deyiləm!', 2.6),
  ('k4b', 'K', 'Орёл приземлился.', 'Qartal yerə endi.', .8),
  ('v11', 'V', 'Повторим момент! Хабиб летит, Конор висит, Гена спит!', 'Anı təkrar edək! Xabib uçur, Conor asılıb, Gennadi yatır!', 1.0),
  ('a6', 'A', 'Теперь моя очередь!', 'İndi mənim növbəmdir!', .3),
  ('v10', 'V', 'Амир зашёл судье в ноги! Судья полетел! Вот это заход!', 'Amir hakimin ayaqlarına girdi! Hakim uçdu! Bax bu giriş!', 3.4),
  ('r6', 'R', 'Я лечу... а считать кто будет?', 'Mən uçuram... bəs kim sayacaq?', 1.0)]),
 (None, 'mom', [
  ('m4', 'M', 'АМИР! ДОМОЙ! ЖИВО!', 'AMİR! EVƏ! TEZ!', .4),
  ('c6', 'C', 'Я с ним не дрался! Это всё орёл!', 'Mən onunla döyüşmədim! Bütün bunlar qartaldır!', .3),
  ('k5', 'K', 'Здравствуйте, мама.', 'Salam, ana.', .3),
  ('g3', 'G', 'Что, уже конец? Я всё охранял!', 'Nə, artıq sondur? Mən hər şeyi qoruyurdum!', .5)]),
 (None, 'dinner', [
  ('x3', 'X', 'Ужин.', 'Şam yeməyi.', .2),
  ('c7', 'C', 'А можно добавки?', 'Əlavə ola bilər?', .2),
  ('m5', 'M', 'Руки мыл?', 'Əllərini yumusan?', .2),
  ('c8', 'C', 'Я же чемпион!', 'Mən axı çempionam!', .2),
  ('m6', 'M', 'Чемпионы моют руки!', 'Çempionlar əllərini yuyurlar!', 1.0),
  ('a7', 'A', 'Мам, я сегодня тренировал самого Хабиба.', 'Ana, mən bu gün Xabibin özünü məşq etdirdim.', .2),
  ('m7', 'M', 'Молодец. А уроки?', 'Afərin. Bəs dərslər?', .3),
  ('k6', 'K', 'Нет.', 'Xeyr.', 1.0)]),
]
def build(speed, gap, pscale):
    out = []; t = 0.0; marks = []
    for start, name, lines in SCENES:
        t = max(t + .6, start) if start is not None else t + .6
        marks.append((name, round(t, 2)))
        for ent in lines:
            key, who, ru, az = ent[:4]; pause = (ent[4] if len(ent) > 4 else .3) * pscale
            dur = max(1.3, len(ru) / speed + .5) if who != 'X' else 2.4
            out.append({'key': key, 'who': who, 't': round(t, 2), 'end': round(t + dur, 2), 'ru': ru, 'az': az, 'scene': name})
            t = t + dur + gap + pause
    return out, marks, t
if __name__ == '__main__':
    a = [float(x) for x in sys.argv[1:4]] if len(sys.argv) > 3 else [14, .28, .85]
    lines, marks, end = build(*a)
    for i, l in enumerate(lines): l['id'] = i
    print('lines', len(lines), 'ends %.1f' % end, marks)
    json.dump(lines, open(os.path.join(HERE, 'lines.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    open(os.path.join(HERE, 'lines.js'), 'w', encoding='utf-8').write('window.LINES = ' + json.dumps(lines, ensure_ascii=False) + ';\nwindow.SCENES = ' + json.dumps(dict(marks)) + ';\n')
