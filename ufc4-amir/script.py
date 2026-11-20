#!/usr/bin/env python3
"""Script v4 'Амир и орёл' (short, fast). Timings come from the real Google-TTS clip lengths (needs network + user consent).
Usage: python3 -I script.py CACHE_DIR"""
import json, os, sys, subprocess, hashlib, urllib.parse, urllib.request
HERE = os.path.dirname(os.path.abspath(__file__)); CACHE = sys.argv[1]; os.makedirs(CACHE, exist_ok=True)
TEMPO = {'A': 1.05, 'K': .88, 'C': 1.12, 'M': 1.0, 'V': 1.10, 'R': .95, 'G': .78, 'S': 1.0, 'Y': 1.05}
L = [
 ('x0', 'X', 'Пародия. Все персонажи выдуманы.', 'Parodiya. Bütün personajlar uydurmadır.', .1),
 ('v1', 'V', 'Бой века! Хабиб против Конора!', 'Əsrin döyüşü! Xabib Conora qarşı!', .1),
 ('c1', 'C', 'Я самый красивый! Я всех побью!', 'Mən ən yaraşıqlıyam! Hamını döyəcəyəm!', .1),
 ('k1', 'K', 'Посмотрим.', 'Baxarıq.', .15),
 ('r1', 'R', 'Бой!', 'Döyüş!', .1),
 ('v2', 'V', 'Конор налетает! Джеб, кросс, хук! Мимо, мимо, мимо!', 'Conor hücum edir! Cəb, kros, huk! Boş, boş, boş!', .1),
 ('c2', 'C', 'Получай лоукик! Ай! Моя нога!', 'Al lou-kik! Ay! Mənim ayağım!', .1),
 ('a1', 'A', 'Хабиб, давай!', 'Xabib, hayda!', .1),
 ('v3', 'V', 'Заход в ноги! Поднял! Крутит! Кладёт! Бам!', 'Ayaqlara girdi! Qaldırdı! Fırladır! Qoyur! Bam!', 1.0),
 ('r2', 'R', 'Раз! Два! Три! Он дышит!', 'Bir! İki! Üç! O nəfəs alır!', .1),
 ('c3', 'C', 'Я просто прилёг!', 'Mən sadəcə uzandım!', .2),
 ('v4', 'V', 'Конор лезет на сетку!', 'Conor torla yuxarı dırmanır!', .1),
 ('k2', 'K', 'Орёл.', 'Qartal.', .3),
 ('v5', 'V', 'Полёт орла! Хабиб ловит Конора! Бам!', 'Qartalın uçuşu! Xabib Conoru tutur! Bam!', .5),
 ('c4', 'C', 'Хочу к маме!', 'Anamın yanına istəyirəm!', .1),
 ('a2', 'A', 'Теперь моя очередь!', 'İndi mənim növbəmdir!', .1),
 ('v6', 'V', 'Амир делает заход в ноги судье! Судья улетел!', 'Amir hakimə ayaqlara giriş edir! Hakim uçdu!', .5),
 ('m1', 'M', 'Амир! Домой! Ужин!', 'Amir! Evə! Şam yeməyi!', .1),
 ('c5', 'C', 'Мама, спаси!', 'Ana, xilas et!', .1),
 ('m2', 'M', 'Все за стол! Руки мыть!', 'Hamı süfrəyə! Əllər yuyulsun!', .2),
 ('k3', 'K', 'Слушаюсь.', 'Baş üstə.', .1)]
def dur(text):
    p = os.path.join(CACHE, hashlib.md5(('ru' + text).encode()).hexdigest() + '.mp3')
    if not os.path.exists(p):
        u = 'https://translate.google.com/translate_tts?' + urllib.parse.urlencode({'ie': 'UTF-8', 'q': text, 'tl': 'ru', 'client': 'tw-ob'})
        open(p, 'wb').write(urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'}), timeout=30).read())
    return float(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', p]))
out = []; t = 0.8
for key, who, ru, az, pause in L:
    d = 2.2 if who == 'X' else dur(ru) / TEMPO[who] + .12
    out.append({'key': key, 'who': who, 't': round(t, 2), 'end': round(t + d, 2), 'ru': ru, 'az': az, 'scene': 'fight', 'id': len(out)})
    t += d + pause + .05
    if key == 'x0': t += .0
print('lines', len(out), 'ends %.1f' % t)
json.dump(out, open(os.path.join(HERE, 'lines.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
open(os.path.join(HERE, 'lines.js'), 'w', encoding='utf-8').write('window.LINES = ' + json.dumps(out, ensure_ascii=False) + ';\nwindow.SCENES = {"fight": 0.8};\n')
for l in out: print(f"{l['t']:5.1f}-{l['end']:5.1f} {l['key']}")
