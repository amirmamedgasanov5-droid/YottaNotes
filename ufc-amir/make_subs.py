#!/usr/bin/env python3
"""Makes subtitles (ASS for burning in, SRT for editors) and a readable voice-over script.
Usage: python3 -I make_subs.py OUT_DIR"""
import json, os, sys
here = os.path.dirname(os.path.abspath(__file__))
L = json.load(open(os.path.join(here, 'lines.json'), encoding='utf-8'))
out = sys.argv[1]; os.makedirs(out, exist_ok=True)

NAMES = {
    'ru': {'N': 'Диктор', 'A': 'Амир', 'K': 'Хабиб', 'C': 'Конор', 'M': 'Мама', 'S': 'Комментатор', 'R': 'Судья', 'X': ''},
    'az': {'N': 'Aparıcı', 'A': 'Amir', 'K': 'Xabib', 'C': 'Conor', 'M': 'Ana', 'S': 'Şərhçi', 'R': 'Hakim', 'X': ''},
}
# ASS colours are BGR: &HBBGGRR&
COL = {'N': 'FFFFFF', 'A': '00D7FF', 'K': '90EE90', 'C': '3C78FF', 'M': 'C8A0FF', 'S': 'FFE066', 'R': 'D0D0D0', 'X': 'B0B0B0'}


def ts_ass(t): h = int(t // 3600); mi = int(t % 3600 // 60); s = t % 60; return f'{h}:{mi:02d}:{s:05.2f}'
def ts_srt(t): h = int(t // 3600); mi = int(t % 3600 // 60); s = int(t % 60); ms = int(round((t - int(t)) * 1000)); return f'{h:02d}:{mi:02d}:{s:02d},{ms:03d}'


for lang in ('ru', 'az'):
    ass = ['[Script Info]', 'ScriptType: v4.00+', 'PlayResX: 1080', 'PlayResY: 1920', 'WrapStyle: 0', 'ScaledBorderAndShadow: yes', '',
           '[V4+ Styles]', 'Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding',
           'Style: Main,DejaVu Sans,50,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,1,0,0,0,100,100,0,0,1,6,2,2,60,60,150,1',
           'Style: Narr,DejaVu Sans,48,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,1,1,0,0,100,100,0,0,1,6,2,2,60,60,150,1',
           'Style: Sys,DejaVu Sans,40,&H00D0D0D0,&H00D0D0D0,&H00000000,&H80000000,0,1,0,0,100,100,0,0,1,5,2,2,60,60,150,1', '',
           '[Events]', 'Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text']
    srt = []
    for i, l in enumerate(L, 1):
        who = l['who']; text = l[lang]; name = NAMES[lang][who]
        style = 'Narr' if who == 'N' else 'Sys' if who == 'X' else 'Main'
        label = f"{{\\c&H{COL[who]}&}}{name}:{{\\c&HFFFFFF&}} " if who not in ('N', 'X') else ''
        ass.append(f"Dialogue: 0,{ts_ass(l['t'])},{ts_ass(l['end'])},{style},,0,0,0,,{label}{text}")
        srt.append(f"{i}\n{ts_srt(l['t'])} --> {ts_srt(l['end'])}\n{(name + ': ') if who not in ('N', 'X') else ''}{text}\n")
    ass.append(f"Dialogue: 0,{ts_ass(297.8)},{ts_ass(300)},Main,,0,0,0,,")
    open(os.path.join(out, f'subtitles_{lang}.ass'), 'w', encoding='utf-8').write('\n'.join(ass) + '\n')
    open(os.path.join(out, f'subtitles_{lang}.srt'), 'w', encoding='utf-8').write('\n'.join(srt))

# readable script for the person who will record the voices
def tc(t): return f'{int(t // 60)}:{t % 60:04.1f}'
rows = ['«АМИР В UFC» — сценарий для озвучки (пародия, всё выдумано)', 'Мультик 5:00, вертикальный 9:16. Роли: Диктор, Амир (ребёнок), Хабиб, Конор, Мама, Комментатор, Судья.',
        'Таймкод — когда начинать реплику; «до» — к какому времени её нужно закончить.', '']
scene = {0: 'ПРОЛОГ', 1: 'СЦЕНА 1. Комната Амира', 9: 'СЦЕНА 2. Зал «Орёл»', 21: 'СЦЕНА 3. Телевизор', 28: 'СЦЕНА 4. Взвешивание', 38: 'СЦЕНА 5. Бой в клетке', 54: 'СЦЕНА 6. Хабиб выходит в клетку', 62: 'СЦЕНА 7. Ужин'}
for l in L:
    if l['id'] in scene: rows += ['', '=== ' + scene[l['id']] + ' ===']
    who = NAMES['ru'][l['who']] or 'Титр'
    rows.append(f"[{tc(l['t'])} → {tc(l['end'])}] {who:11s} {l['ru']}")
    rows.append(f"{'':35s}(az) {l['az']}")
open(os.path.join(out, 'scenariy_dlya_ozvuchki.txt'), 'w', encoding='utf-8').write('\n'.join(rows) + '\n')
print('subs written to', out, len(L), 'lines')
