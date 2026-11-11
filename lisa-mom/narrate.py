#!/usr/bin/env python3
"""Builds the narrator track with RHVoice (Russian TTS), placed at exact timecodes.

Usage: python3 -I narrate.py lines.json out_voice.wav DURATION_SECONDS [profile] [pitch]
lines.json: [[start_seconds, "text"], ...]
If a line would run into the next one, its speech rate is raised automatically.
"""
import sys, json, wave, subprocess, tempfile
import numpy as np

SR = 44100
lines = json.load(open(sys.argv[1]))
out = sys.argv[2]
DUR = float(sys.argv[3])
profile = sys.argv[4] if len(sys.argv) > 4 else 'aleksandr-hq'
pitch = int(sys.argv[5]) if len(sys.argv) > 5 else 92


def synth(text, rate):
    with tempfile.NamedTemporaryFile(suffix='.wav') as f:
        subprocess.run(['RHVoice-test', '-p', profile, '-r', str(rate), '-t', str(pitch), '-R', str(SR), '-o', f.name],
                       input=text.encode(), check=True, stderr=subprocess.DEVNULL)
        with wave.open(f.name) as w:
            sr = w.getframerate()
            x = np.frombuffer(w.readframes(w.getnframes()), '<i2').astype(np.float64) / 32768
            if w.getnchannels() > 1: x = x.reshape(-1, w.getnchannels()).mean(1)
    if sr != SR:
        x = np.interp(np.arange(int(len(x) * SR / sr)) / SR, np.arange(len(x)) / sr, x)
    return x


N = int(SR * DUR)
track = np.zeros(N)
for i, (t0, text) in enumerate(lines):
    nxt = lines[i + 1][0] if i + 1 < len(lines) else DUR
    room = nxt - t0 - 0.3
    rate = 96
    x = synth(text, rate)
    while len(x) / SR > room and rate < 160:
        rate += 6
        x = synth(text, rate)
    d = len(x) / SR
    flag = '' if d <= room else '  <-- DOES NOT FIT'
    print(f'{t0:6.1f}s  {d:4.1f}s of {room:4.1f}s  rate {rate:3d}  {text[:60]}{flag}')
    # trim silence at edges, then place
    nz = np.where(np.abs(x) > 0.004)[0]
    if len(nz): x = x[nz[0]:nz[-1] + 1]
    s = int(t0 * SR)
    seg = x[: N - s]
    track[s:s + len(seg)] += seg

# gentle room reverb for a haunted feel (wet 18%)
ir_len = int(SR * .45)
ir = np.random.default_rng(3).standard_normal(ir_len) * np.exp(-np.arange(ir_len) / (SR * .11))
n = 1 << (N + ir_len).bit_length()
wet = np.fft.irfft(np.fft.rfft(track, n) * np.fft.rfft(ir, n), n)[:N]
wet *= np.abs(track).max() / (np.abs(wet).max() + 1e-9) * .5
track = track + .36 * wet

track *= 0.8 / (np.abs(track).max() + 1e-9)
pcm = (track * 32767).astype('<i2')
with wave.open(out, 'wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print('wrote', out)
