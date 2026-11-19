#!/usr/bin/env python3
"""Script source for 'Амир и большой бой' (parody). Writes lines.json + lines.js with computed timings.
Each entry: (key, who, ru, az[, pause_after_seconds]).  Scenes are anchored at fixed start times."""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
TOTAL = 300.0

SCENES = [
 (0.8, 'prolog', [
  ('x0', 'X', 'Пародия. Все персонажи выдуманы.', 'Parodiya. Bütün personajlar uydurmadır.', 0.2)]),
 (6.0, 'home', [
  ('v1', 'V', 'Хабиб снова победил! Он был так спокоен, что соперник уснул сам.', 'Xabib yenə qalib gəldi! O, o qədər sakit idi ki, rəqib özü yatdı.', .5),
  ('a1', 'A', 'Я тоже хочу в UFC! У меня уже есть и перчатки, и страшный взгляд.', 'Mən də UFC-yə getmək istəyirəm! Həm əlcəklərim var, həm də qorxulu baxışım.', .3),
  ('m1', 'M', 'Амир! Ты уроки сделал?', 'Amir! Dərslərini etmisən?', .2),
  ('a2', 'A', 'Мам, я готовлюсь к бою.', 'Ana, mən döyüşə hazırlaşıram.', .2),
  ('m2', 'M', 'С кем?', 'Kiminlə?', .1),
  ('a3', 'A', 'С Хабибом.', 'Xabiblə.', .1),
  ('m3', 'M', 'А он в курсе?', 'O xəbərdardır?', .3),
  ('a4', 'A', 'Пока нет. Сейчас напишу письмо.', 'Hələ yox. İndi məktub yazacağam.', 1.4),
  ('a5', 'A', 'Хабиб, давай драться. Адрес: диван. Амир, десять лет. Принеси сменку.', 'Xabib, gəl döyüşək. Ünvan: divan. Amir, on yaşım var. Dəyişək paltar gətir.', .3),
  ('a6', 'A', 'Отправляю самолётиком!', 'Təyyarəciklə göndərirəm!', 1.6)]),
 (None, 'gym', [
  ('x2', 'X', 'Горы. Зал Хабиба.', 'Dağlar. Xabibin zalı.', .3),
  ('k1', 'K', 'Давай драться. Адрес: диван. Принеси сменку.', 'Gəl döyüşək. Ünvan: divan. Dəyişək paltar gətir.', .6),
  ('k2', 'K', 'Ребёнок. Хорошо.', 'Uşaqdır. Yaxşı.', .3),
  ('k3', 'K', 'Миша, сегодня работаем с мелким.', 'Mişa, bu gün balacayla işləyirik.', 1.2),
  ('a7', 'A', 'Здравствуйте! Я пришёл вас победить. Только после обеда.', 'Salam! Mən sizi məğlub etməyə gəlmişəm. Yalnız nahardan sonra.', .3),
  ('k4', 'K', 'Сначала отжимания.', 'Əvvəlcə sinə üstə əyilmə.', .2),
  ('a8', 'A', 'Сколько?', 'Neçə dəfə?', .1),
  ('k5', 'K', 'Сколько сможешь.', 'Nə qədər bacarsan.', 1.8),
  ('a9', 'A', 'Раз! ...Всё. Я чемпион по отжиманиям.', 'Bir! ...Bəs. Mən əyilmə çempionuyam.', .3),
  ('k6', 'K', 'Ты сделал одно.', 'Sən bir dənə etdin.', .2),
  ('a10', 'A', 'Но очень качественное!', 'Amma çox keyfiyyətli!', .3),
  ('k7', 'K', 'Парная работа. Это Миша. Он вежливый. Главное — не шуми.', 'Cüt iş. Bu Mişadır. O, tərbiyəlidir. Əsas odur ki, səs-küy salma.', 2.2),
  ('a11', 'A', 'Он умер?', 'O öldü?', .2),
  ('k8', 'K', 'Нет. Он увидел твои отжимания.', 'Xeyr. O, sənin əyilmələrini gördü.', .4),
  ('k9', 'K', 'Теперь захват. Хватай меня за ногу.', 'İndi tutma. Mənim ayağımdan tut.', 1.6),
  ('a12', 'A', 'Я не держу! Я просто боюсь отпустить!', 'Mən tutmuram! Sadəcə buraxmağa qorxuram!', .3),
  ('k10', 'K', 'Для начала сойдёт. И последнее. Папаха силы.', 'Başlanğıc üçün olar. Və sonuncu. Güc papağı.', 1.4),
  ('a13', 'A', 'Я ничего не вижу, но я очень сильный!', 'Heç nə görmürəm, amma çox güclüyəm!', 1.6)]),
 (None, 'news', [
  ('v2', 'V', 'Срочные новости! Конор Макгрегор вызвал на бой... так... ребёнка.', 'Təcili xəbər! Conor McGregor döyüşə çağırıb... belə... uşağı.', .3),
  ('v3', 'V', 'Мы проверили. Ребёнок существует. Мама подтвердила.', 'Yoxladıq. Uşaq mövcuddur. Anası təsdiq edib.', .4),
  ('c1', 'C', 'Эй, мелкий! Я такой быстрый, что вчера убежал от собственной тени!', 'Ey, balaca! Mən o qədər sürətliyəm ki, dünən öz kölgəmdən qaçdım!', .2),
  ('c2', 'C', 'Она до сих пор меня ищет!', 'O, hələ də məni axtarır!', .4),
  ('a14', 'A', 'Он слишком много говорит.', 'O, çox danışır.', .3),
  ('k11', 'K', 'Пусть. Это его разминка.', 'Qoy danışsın. Bu, onun isinməsidir.', .4),
  ('c3', 'C', 'У меня костюм дороже твоего дома! Хотя... у тебя же есть дом?', 'Mənim kostyumum sənin evindən bahalıdır! Hərçənd... sənin evin var ki?', .3),
  ('a15', 'A', 'Есть! С диваном!', 'Var! Divanlı!', .2),
  ('c4', 'C', 'Диван — слабак!', 'Divan zəifdir!', .6),
  ('a16', 'A', 'Не слушай его, диванчик.', 'Ona qulaq asma, divançıq.', 1.0)]),
 (None, 'weigh', [
  ('x4', 'X', 'Взвешивание.', 'Çəkiyə çıxma.', .3),
  ('v4', 'V', 'Дамы и господа! Хабиб Нур... Нурмамедов... неважно, все пришли!', 'Xanımlar və cənablar! Xabib Nur... Nurməmmədov... fərqi yoxdur, hamı gəlib!', .3),
  ('r1', 'R', 'Конор, на весы.', 'Conor, tərəziyə.', 1.2),
  ('r2', 'R', 'Весы просят вас слезть. Вежливо.', 'Tərəzi sizdən düşməyi xahiş edir. Nəzakətlə.', .3),
  ('c5', 'C', 'Это их проблемы!', 'Bu, onların problemidir!', .3),
  ('v5', 'V', 'Охранник Гена спит. Это нормально.', 'Mühafizəçi Gennadi yatır. Bu normaldır.', .6),
  ('r3', 'R', 'Амир, теперь ты.', 'Amir, indi sən.', 1.0),
  ('r4', 'R', 'Двадцать девять килограммов. Плюс шапка. Плюс бутерброд. Плюс сумка.', 'İyirmi doqquz kiloqram. Üstəgəl papaq. Üstəgəl sendviç. Üstəgəl çanta.', .3),
  ('r5', 'R', 'Ладно. Это не вес, это обед.', 'Yaxşı. Bu çəki deyil, bu nahardır.', .3),
  ('a17', 'A', 'Мама сказала: без бутерброда не выходить.', 'Ana dedi: sendvicsiz çıxma.', .4),
  ('r6', 'R', 'Гляделки. Кто моргнёт — тот проиграл.', 'Baxış yarışı. Kim göz qırparsa, uduzur.', 3.0),
  ('r7', 'R', 'Я моргнул. Я проиграл.', 'Mən göz qırpdım. Uduzdum.', .3),
  ('c6', 'C', 'Ты не участвуешь!', 'Sən iştirak etmirsən!', .3),
  ('r8', 'R', 'Всё равно проиграл.', 'Yenə də uduzdum.', .8)]),
 (None, 'fight', [
  ('x5', 'X', 'Арена. Суббота.', 'Arena. Şənbə.', .3),
  ('v6', 'V', 'В левом углу — Амир... Амирка... Амирчик! В правом — Конор!', 'Sol küncdə — Amir... Amirka... Amirçik! Sağ küncdə — Conor!', .3),
  ('g1', 'G', 'Что? Уже начали?', 'Nə? Artıq başladılar?', .2),
  ('r9', 'R', 'Бой!', 'Döyüş!', .4),
  ('c7', 'C', 'Я тебя поймаю! Я чемпион! Я красивый! У меня костюм! И я...', 'Mən səni tutacağam! Mən çempionam! Mən gözələm! Kostyumum var! Və mən...', .3),
  ('a18', 'A', 'А долго ещё?', 'Hələ çoxdur?', .3),
  ('v7', 'V', 'Конор говорит уже минуту! Это рекорд скорости говорения!', 'Conor artıq bir dəqiqədir danışır! Bu, danışma sürəti rekorddur!', 1.0),
  ('c8', 'C', 'Мой галстук! Он итальянский!', 'Mənim qalstukum! O, italyandır!', .3),
  ('c9', 'C', 'Судья! Медведь — это не по правилам!', 'Hakim! Ayı qaydalara uyğun deyil!', .3),
  ('r10', 'R', 'В правилах про медведей ничего нет. Я проверял. Я их съел.', 'Qaydalarda ayılar haqqında heç nə yoxdur. Yoxladım. Mən onları yemişəm.', .6),
  ('r11', 'R', 'Минуточку. Шнурок.', 'Bir dəqiqə. Ayaqqabı bağı.', 1.4),
  ('c10', 'C', 'Бутерброд будешь?', 'Sendviç yeyərsən?', .2),
  ('a19', 'A', 'С чем?', 'Nəylə?', .1),
  ('c11', 'C', 'С колбасой.', 'Kolbasalı.', .1),
  ('a20', 'A', 'Давай.', 'Ver.', .5),
  ('v8', 'V', 'Бойцы делят бутерброд! Такого в UFC ещё не было!', 'Döyüşçülər sendviçi bölürlər! UFC-də belə hal olmayıb!', .4),
  ('k12', 'K', 'Амир! Делай то, что я говорил!', 'Amir! Dediyimi et!', .2),
  ('a21', 'A', 'Вы ничего не говорили!', 'Siz heç nə deməmişdiniz!', .2),
  ('k13', 'K', 'Значит, делай то, что не говорил!', 'Deməli, deməyəni et!', .2),
  ('a22', 'A', 'Понял!', 'Başa düşdüm!', 1.4),
  ('v9', 'V', 'Нокаут! Амир победил, ничего не делая! Гениально!', 'Nokaut! Amir heç nə etmədən qalib gəldi! Dahiyanə!', .6)]),
 (None, 'mom', [
  ('m4', 'M', 'АМИР! ЭТО ЧТО ТАКОЕ?! ДОМОЙ!', 'AMİR! BU NƏDİR?! EVƏ!', .3),
  ('c12', 'C', 'Я не виноват! Это он!', 'Mən günahkar deyiləm! Bu, odur!', .3),
  ('k14', 'K', 'Здравствуйте, мама.', 'Salam, ana.', .3),
  ('g2', 'G', 'Что? Уже конец?', 'Nə? Artıq sondur?', .2),
  ('r12', 'R', 'Победила... мама!', 'Qalib... ana!', .8)]),
 (None, 'dinner', [
  ('x7', 'X', 'Ужин.', 'Şam yeməyi.', .3),
  ('c13', 'C', 'А добавки можно?', 'Əlavə ola bilər?', .2),
  ('m5', 'M', 'Руки мыл?', 'Əllərini yumusan?', .2),
  ('c14', 'C', 'Я же чемпион!', 'Mən axı çempionam!', .2),
  ('m6', 'M', 'Чемпионы моют руки.', 'Çempionlar əllərini yuyurlar.', 1.0),
  ('m7', 'M', 'Амир, молодец. А уроки?', 'Amir, afərin. Bəs dərslər?', .3),
  ('a23', 'A', 'Хабиб, победите меня обратно!', 'Xabib, məni geri məğlub edin!', .3),
  ('k15', 'K', 'Нет.', 'Xeyr.', 1.0)]),
]

def build(speed=12.0, gap=.35, pscale=1.0):
    out = []; t = 0.0; scene_marks = []
    for start, name, lines in SCENES:
        t = max(t + .6, start) if start is not None else t + .6
        scene_marks.append((name, round(t, 2)))
        for ent in lines:
            key, who, ru, az = ent[:4]; pause = (ent[4] if len(ent) > 4 else .3) * pscale
            dur = max(1.3, len(ru) / speed + .5) if who != 'X' else 2.4
            out.append({'key': key, 'who': who, 't': round(t, 2), 'end': round(t + dur, 2), 'ru': ru, 'az': az, 'scene': name})
            t = t + dur + gap + pause
    return out, scene_marks, t

if __name__ == '__main__':
    speed = float(sys.argv[1]) if len(sys.argv) > 1 else 12.0
    gap = float(sys.argv[2]) if len(sys.argv) > 2 else .35
    pscale = float(sys.argv[3]) if len(sys.argv) > 3 else 1.0
    lines, marks, end = build(speed, gap, pscale)
    for i, l in enumerate(lines): l['id'] = i
    print('lines:', len(lines), ' ends at: %.1f s (limit %.0f)' % (end, TOTAL))
    print('scenes:', marks)
    json.dump(lines, open(os.path.join(HERE, 'lines.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    open(os.path.join(HERE, 'lines.js'), 'w', encoding='utf-8').write('window.LINES = ' + json.dumps(lines, ensure_ascii=False) + ';\nwindow.SCENES = ' + json.dumps(dict(marks)) + ';\n')
