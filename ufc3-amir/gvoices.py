#!/usr/bin/env python3
"""Dub via Google Translate TTS (single voice, pitch-shifted per character). Needs network + user consent.
Usage: python3 -I gvoices.py lines.json out.wav DURATION LANG(ru|az) CACHE_DIR"""
import sys, json, os, subprocess, wave, urllib.parse, urllib.request, hashlib
import numpy as np
SR = 44100
lines = json.load(open(sys.argv[1], encoding='utf-8')); out = sys.argv[2]; DUR = float(sys.argv[3]); LANG = sys.argv[4]; CACHE = sys.argv[5]; os.makedirs(CACHE, exist_ok=True)
PITCH = {'A': 1.25, 'K': .88, 'C': 1.06, 'M': 1.12, 'V': 1.0, 'R': .95, 'G': .8, 'S': .86, 'Y': 1.2}
def tts(text):
    p = os.path.join(CACHE, hashlib.md5((LANG + text).encode()).hexdigest() + '.mp3')
    if not os.path.exists(p):
        u = 'https://translate.google.com/translate_tts?' + urllib.parse.urlencode({'ie': 'UTF-8', 'q': text, 'tl': LANG, 'client': 'tw-ob'})
        open(p, 'wb').write(urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'}), timeout=30).read())
    return p
def render(mp3, pitch, tempo, wav):
    f = f'asetrate={SR*pitch},aresample={SR},atempo={max(.5,min(2.0,tempo/pitch))}'
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', mp3, '-af', f, '-ar', str(SR), '-ac', '1', wav], check=True)
    with wave.open(wav) as w: return np.frombuffer(w.readframes(w.getnframes()), '<i2').astype(np.float64) / 32768
def loud(x):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR); X[(f < 400) | (f > 3800)] = 0; y = np.tanh(2 * np.fft.irfft(X, len(x))) * .7
    d = int(SR * .08); z = np.concatenate([y, np.zeros(d)]); z[d:] += .3 * y; return z
N = int(SR * DUR); track = np.zeros(N); tmp = os.path.join(CACHE, 'tmp.wav')
for l in lines:
    if l['who'] == 'X': continue
    text = l[LANG]; mp3 = tts(text); p = PITCH[l['who']]; room = l['end'] - l['t'] - .05
    x = render(mp3, p, 1.0, tmp)
    if len(x) / SR > room: x = render(mp3, p, len(x) / SR / room * 1.02, tmp)
    nz = np.where(np.abs(x) > .004)[0]
    if len(nz): x = x[nz[0]:nz[-1] + 1]
    if l['who'] == 'V': x = loud(x)
    x *= .8 / (np.abs(x).max() + 1e-9)
    print(f"{l['t']:6.1f} {l['who']} {len(x)/SR:4.1f}/{room:4.1f} {text[:40]}")
    s = int(l['t'] * SR); seg = x[:N - s]; track[s:s + len(seg)] += seg
track *= .9 / (np.abs(track).max() + 1e-9)
with wave.open(out, 'wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((track * 32767).astype('<i2').tobytes())
print('wrote', out)
