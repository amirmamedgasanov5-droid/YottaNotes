#!/usr/bin/env python3
"""Dub via Google Translate TTS (single voice, pitch-shifted per character). Needs network + user consent.
Usage: python3 -I gvoices.py lines.json out.wav DURATION LANG(ru|az) CACHE_DIR"""
import sys, json, os, subprocess, wave, urllib.parse, urllib.request, hashlib
import numpy as np
SR = 44100
lines = json.load(open(sys.argv[1], encoding='utf-8')); out = sys.argv[2]; DUR = float(sys.argv[3]); LANG = sys.argv[4]; CACHE = sys.argv[5]; os.makedirs(CACHE, exist_ok=True)
# per character: pitch ratio, formant ratio (<1 = bigger body, >1 = smaller), tempo, rasp, brightness(tilt), drive
VOX = {'A': (1.30, 1.28, 1.05, 0, 1.0, 0), 'K': (.66, .80, .88, 1.0, -1, 2.2), 'C': (.98, .97, 1.12, .15, 1.0, .8),
       'M': (1.02, 1.0, 1.0, 0, 0, 0), 'V': (.78, .86, 1.10, .1, 0, 1.0), 'R': (.72, .88, .95, .35, -1, 1.2),
       'G': (.55, .72, .78, .7, -1, 1.8), 'S': (.85, .9, 1.0, .2, 0, 1.0), 'Y': (1.22, 1.15, 1.05, 0, 1.0, 0)}
def tts(text):
    p = os.path.join(CACHE, hashlib.md5((LANG + text).encode()).hexdigest() + '.mp3')
    if not os.path.exists(p):
        u = 'https://translate.google.com/translate_tts?' + urllib.parse.urlencode({'ie': 'UTF-8', 'q': text, 'tl': LANG, 'client': 'tw-ob'})
        open(p, 'wb').write(urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'}), timeout=30).read())
    return p
def render(mp3, who, extra, wav):
    p, f, tp, rasp, tilt, drive = VOX[who]
    af = f'rubberband=pitch={p}:formant=shifted:tempo={max(.5, min(3.0, tp * extra))}:pitchq=quality,aresample={SR}'
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', mp3, '-af', af, '-ar', str(SR), '-ac', '1', wav], check=True)
    with wave.open(wav) as w: x = np.frombuffer(w.readframes(w.getnframes()), '<i2').astype(np.float64) / 32768
    return color(x, who)
def band(x, lo=None, hi=None):
    X = np.fft.rfft(x); fr = np.fft.rfftfreq(len(x), 1 / SR)
    if lo: X[fr < lo] = 0
    if hi: X[fr > hi] = 0
    return np.fft.irfft(X, len(x))
def color(x, who):
    p, f, tp, rasp, tilt, drive = VOX[who]; rng = np.random.default_rng(ord(who))
    env = np.convolve(np.abs(x), np.ones(int(SR * .02)) / int(SR * .02), 'same')
    if tilt < 0: x = x + .5 * band(x, None, 700) - .25 * band(x, 3500, None)       # darker, boomy
    if tilt > 0: x = x + .6 * band(x, 2500, 7000)                                  # brighter
    if rasp:                                                                       # hoarse: growl AM + gated noise + saturation
        t = np.arange(len(x)) / SR
        growl = x * (1 - .55 * rasp * (.5 + .5 * np.sign(np.sin(2 * np.pi * 63 * t + 3 * np.sin(2 * np.pi * 7 * t)))))
        nz = band(rng.standard_normal(len(x)), 1500, 6500) * env * 1.4 * rasp
        x = growl + nz
    if drive: x = np.tanh(x * (1 + drive * 1.6)) / np.tanh(1 + drive * 1.6)
    return x
def loud(x):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR); X[(f < 400) | (f > 3800)] = 0; y = np.tanh(2 * np.fft.irfft(X, len(x))) * .7
    d = int(SR * .08); z = np.concatenate([y, np.zeros(d)]); z[d:] += .3 * y; return z
N = int(SR * DUR); track = np.zeros(N); tmp = os.path.join(CACHE, 'tmp.wav')
for l in lines:
    if l['who'] == 'X': continue
    text = l[LANG]; mp3 = tts(text); room = l['end'] - l['t'] - .05
    x = render(mp3, l['who'], 1.0, tmp)
    if len(x) / SR > room: x = render(mp3, l['who'], len(x) / SR / room * 1.02, tmp)
    nz = np.where(np.abs(x) > .004)[0]
    if len(nz): x = x[nz[0]:nz[-1] + 1]
    if l['who'] == 'V': x = loud(x)
    x *= .8 / (np.abs(x).max() + 1e-9)
    print(f"{l['t']:6.1f} {l['who']} {len(x)/SR:4.1f}/{room:4.1f} {text[:40]}")
    s = int(l['t'] * SR); seg = x[:N - s]; track[s:s + len(seg)] += seg
track *= .9 / (np.abs(track).max() + 1e-9)
with wave.open(out, 'wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((track * 32767).astype('<i2').tobytes())
print('wrote', out)
