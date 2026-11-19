#!/usr/bin/env python3
"""Multi-voice Russian dub for the parody, one RHVoice voice per character.
Usage: python3 -I voices.py lines.json out.wav DURATION SPEED     (SPEED 1.0 = normal, 1.3 = fast)"""
import sys, json, wave, subprocess, tempfile
import numpy as np

SR = 44100
lines = json.load(open(sys.argv[1], encoding='utf-8')); out = sys.argv[2]; DUR = float(sys.argv[3]); SPEED = float(sys.argv[4])
# who: (RHVoice voice, pitch %, base rate %, level)
V = {'N': ('mikhail', 92, 100, 1.0), 'A': ('artemiy', 150, 104, 1.0), 'K': ('aleksandr-hq', 80, 88, 1.0),
     'C': ('evgeniy-rus', 110, 118, 1.0), 'S': ('pavel', 108, 116, 1.0), 'R': ('vitaliy', 95, 100, 1.0), 'M': ('elena', 100, 98, 1.0)}
rng = np.random.default_rng(5)


def synth(text, who, rate):
    prof, pitch, _, _ = V[who]
    with tempfile.NamedTemporaryFile(suffix='.wav') as f:
        subprocess.run(['RHVoice-test', '-p', prof, '-r', str(int(rate)), '-t', str(pitch), '-R', str(SR), '-o', f.name], input=text.encode(), check=True, stderr=subprocess.DEVNULL)
        with wave.open(f.name) as w:
            sr = w.getframerate(); x = np.frombuffer(w.readframes(w.getnframes()), '<i2').astype(np.float64) / 32768
            if w.getnchannels() > 1: x = x.reshape(-1, w.getnchannels()).mean(1)
    if sr != SR: x = np.interp(np.arange(int(len(x) * SR / sr)) / SR, np.arange(len(x)) / sr, x)
    return x


def band(x, lo=None, hi=None):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR)
    if lo: X[f < lo] = 0
    if hi: X[f > hi] = 0
    return np.fft.irfft(X, len(x))


def loudspeaker(x):
    y = band(x, 450, 3400); y = np.tanh(2.5 * y) * .6
    d = int(SR * .09); z = np.concatenate([y, np.zeros(d)]); z[d:] += .35 * y
    return z


N = int(SR * DUR); track = np.zeros(N)
for l in lines:
    if l['who'] == 'X': continue
    who = l['who']; t0, t1 = l['t'], l['end']; room = t1 - t0
    base = V[who][2] * SPEED; rate = base
    x = synth(l['ru'], who, rate)
    while len(x) / SR > room and rate < 190:
        rate += 8; x = synth(l['ru'], who, rate)
    d = len(x) / SR
    print(f"{t0:6.1f}s {who} {d:4.1f}s of {room:4.1f}s rate {int(rate):3d}" + ('' if d <= room else '  <-- DOES NOT FIT') + f"  {l['ru'][:42]}")
    nz = np.where(np.abs(x) > .004)[0]
    if len(nz): x = x[nz[0]:nz[-1] + 1]
    x = x * (.8 / (np.abs(x).max() + 1e-9)) * V[who][3]
    if l['id'] == 59: x = loudspeaker(x)
    s = int(t0 * SR); seg = x[: N - s]; track[s:s + len(seg)] += seg
track *= .9 / (np.abs(track).max() + 1e-9)
with wave.open(out, 'wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((track * 32767).astype('<i2').tobytes())
print('wrote', out)
