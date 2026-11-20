#!/usr/bin/env python3
"""Subtitles (ASS to burn in, SRT for editors) + readable voice-over script. Usage: python3 -I make_subs.py OUT_DIR"""
import json, os, sys
here = os.path.dirname(os.path.abspath(__file__))
L = json.load(open(os.path.join(here, 'lines.json'), encoding='utf-8'))
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
NAMES = {'ru': {'A': 'Амир', 'K': 'Хабиб', 'C': 'Конор', 'M': 'Мама', 'V': 'Комментатор Валера', 'R': 'Судья Петрович', 'G': 'Охранник Гена', 'S': 'Секундант', 'Y': 'Ринг-гёрл', 'X': ''},
         'az': {'A': 'Amir', 'K': 'Xabib', 'C': 'Conor', 'M': 'Ana', 'V': 'Şərhçi Valera', 'R': 'Hakim Petroviç', 'G': 'Mühafizəçi Gennadi', 'S': 'Köməkçi', 'Y': 'Rinq qızı', 'X': ''}}
COL = {'A': '00D7FF', 'K': '90EE90', 'C': '3C78FF', 'M': 'C8A0FF', 'V': 'FFE066', 'R': 'E0E0E0', 'G': '8CC8FF', 'S': 'A0A0FF', 'Y': 'FF90E0', 'X': 'B0B0B0'}  # BGR
def ts_ass(t): h = int(t // 3600); mi = int(t % 3600 // 60); s = t % 60; return f'{h}:{mi:02d}:{s:05.2f}'
def ts_srt(t): h = int(t // 3600); mi = int(t % 3600 // 60); s = int(t % 60); ms = int(round((t - int(t)) * 1000)); return f'{h:02d}:{mi:02d}:{s:02d},{ms:03d}'
for lang in ('ru', 'az'):
    ass = ['[Script Info]', 'ScriptType: v4.00+', 'PlayResX: 1920', 'PlayResY: 1080', 'WrapStyle: 0', 'ScaledBorderAndShadow: yes', '', '[V4+ Styles]',
           'Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding',
           'Style: Main,DejaVu Sans,56,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,1,0,0,0,100,100,0,0,1,5,2,2,120,120,48,1',
           'Style: Sys,DejaVu Sans,46,&H00D0D0D0,&H00D0D0D0,&H00000000,&H80000000,0,1,0,0,100,100,0,0,1,4,2,8,120,120,60,1', '', '[Events]',
           'Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text']
    srt = []
    for i, l in enumerate(L, 1):
        who = l['who']; text = l[lang]; name = NAMES[lang][who]; style = 'Sys' if who == 'X' else 'Main'
        label = f"{{\\c&H{COL[who]}&}}{name}:{{\\c&HFFFFFF&}} " if who != 'X' else ''
        ass.append(f"Dialogue: 0,{ts_ass(l['t'])},{ts_ass(l['end'])},{style},,0,0,0,,{label}{text}")
        srt.append(f"{i}\n{ts_srt(l['t'])} --> {ts_srt(l['end'])}\n{(name + ': ') if who != 'X' else ''}{text}\n")
    open(os.path.join(out, f'subtitles_{lang}.ass'), 'w', encoding='utf-8').write('\n'.join(ass) + '\n')
    open(os.path.join(out, f'subtitles_{lang}.srt'), 'w', encoding='utf-8').write('\n'.join(srt))
TITLES = {'prolog': 'ПРОЛОГ', 'home': 'СЦЕНА 1. Квартира Амира', 'fight': 'Бой в клетке'}
def tc(t): return f'{int(t // 60)}:{t % 60:04.1f}'
rows = ['«АМИР И ОРЁЛ» — сценарий для озвучки (пародия, все персонажи выдуманы)',
        'Мультик 1:00, широкий формат 16:9. Роли: Амир (ребёнок), Хабиб (спокойный, почти без эмоций), Конор (громкий, самовлюблённый), Мама, Комментатор Валера, Судья Петрович, Охранник Гена, Секундант, Ринг-гёрл.',
        '[с → до] — когда начинать реплику и к какому времени её закончить. (az) — перевод для азербайджанских субтитров.', '']
last = None
for l in L:
    sc = l['scene'] if l['scene'] != 'news' else 'gym'
    if sc != last: rows += ['', '=== ' + TITLES.get(sc, sc) + ' ===']; last = sc
    who = NAMES['ru'][l['who']] or 'Титр'
    rows.append(f"[{tc(l['t'])} → {tc(l['end'])}] {who}: {l['ru']}")
    rows.append(f"      (az) {l['az']}")
open(os.path.join(out, 'scenariy_dlya_ozvuchki.txt'), 'w', encoding='utf-8').write('\n'.join(rows) + '\n')
print('subs ok', len(L))
