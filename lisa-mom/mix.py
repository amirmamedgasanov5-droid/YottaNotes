#!/usr/bin/env python3
"""Mixes narrator over the sound effects, ducking the effects while the voice speaks.

Usage: python3 -I mix.py sfx.wav voice.wav out.wav
"""
import sys, wave
import numpy as np


def read(p):
    with wave.open(p) as w:
        ch = w.getnchannels()
        x = np.frombuffer(w.readframes(w.getnframes()), '<i2').astype(np.float64) / 32768
        return x.reshape(-1, ch).T, w.getframerate()


sfx, sr = read(sys.argv[1])
voice, _ = read(sys.argv[2])
n = min(sfx.shape[1], voice.shape[1])
sfx, v = sfx[:, :n], voice[0, :n]

# voice activity -> smooth mask -> duck effects by about 6 dB
win = int(sr * .18)
act = (np.convolve(np.abs(v), np.ones(win) / win, 'same') > .02).astype(float)
mask = np.convolve(act, np.ones(int(sr * .35)) / int(sr * .35), 'same')
duck = 1 - .5 * np.clip(mask * 1.5, 0, 1)

mix = sfx * duck + v * 1.0
peak = np.abs(mix).max()
if peak > .97: mix *= .97 / peak
pcm = (mix.T * 32767).astype('<i2')
with wave.open(sys.argv[3], 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(sr); w.writeframes(pcm.tobytes())
print('wrote', sys.argv[3])
