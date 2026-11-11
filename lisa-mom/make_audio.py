#!/usr/bin/env python3
"""Synthesizes the sound design (no voice) for the "Сообщение от мамы" cartoon.

Usage: python3 -I make_audio.py out.wav
Everything is generated with numpy; timings match index.html.
"""
import sys, wave
import numpy as np

SR = 44100
DUR = 71.0
N = int(SR * DUR)
rng = np.random.default_rng(7)
L = np.zeros(N)
R = np.zeros(N)


def t_arr(d):
    return np.arange(int(SR * d)) / SR


def filt(x, lo=None, hi=None):
    """Brick-wall band-limit via FFT (lo = high-pass cutoff, hi = low-pass cutoff)."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    if lo: X[f < lo] = 0
    if hi: X[f > hi] = 0
    return np.fft.irfft(X, len(x))


def noise(d):
    return rng.standard_normal(int(SR * d))


def env_exp(d, decay, attack=0.002):
    t = t_arr(d)
    e = np.exp(-t / decay)
    a = int(SR * attack)
    if a: e[:a] *= np.linspace(0, 1, a)
    return e


def add(sig, at, amp=1.0, pan=0.0):
    i = int(at * SR)
    if i >= N: return
    s = sig[: N - i]
    gl = amp * np.cos((pan + 1) * np.pi / 4)
    gr = amp * np.sin((pan + 1) * np.pi / 4)
    L[i:i + len(s)] += s * gl
    R[i:i + len(s)] += s * gr


def sine(f, d, decay, amp=1.0, attack=0.002):
    return np.sin(2 * np.pi * f * t_arr(d)) * env_exp(d, decay, attack) * amp


def square(f, d):
    return np.sign(np.sin(2 * np.pi * f * t_arr(d)))


# ---------- sounds ----------
def ding(at, base=988, amp=.22, pan=.2):
    add(sine(base, .5, .12) + .4 * sine(base * 2, .5, .08), at, amp, pan)
    add(sine(base * 1.335, .8, .22) + .35 * sine(base * 2.67, .8, .12), at + .13, amp, pan)


def vibrate(at, d=.45, amp=.12, pan=.2, f=175, cutoff=700):
    t = t_arr(d)
    am = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 28 * t))
    s = filt(square(f, d) * am, None, cutoff)
    s *= np.minimum(1, t / .01) * np.minimum(1, (d - t) / .03)
    add(s, at, amp, pan)
    r = filt(noise(d), 2500, None) * am * .35 * np.minimum(1, t / .01)
    add(r, at, amp * .5, pan)


def whoosh(at, amp=.1, pan=.3):
    d = .22
    s = filt(noise(d), 600, 5000) * np.hanning(int(SR * d))
    add(s, at, amp, pan)
    add(sine(620, .08, .03), at + .17, amp * .8, pan)


def click(at, amp=.3, pan=.8):
    s = filt(noise(.03), 1500, None) * env_exp(.03, .004, .0005)
    add(s + .7 * sine(1900, .03, .006), at, amp, pan)


def thump(at, amp=.5, pan=.7, f=80):
    add(sine(f, .3, .07, attack=.004) + .5 * sine(f * 1.9, .3, .03), at, amp, pan)
    add(filt(noise(.08), None, 450) * env_exp(.08, .02), at, amp * .6, pan)


def bell(at, f, amp=.32, pan=.8):
    t_ = 1.6
    s = (sine(f, t_, .5) + .5 * sine(f * 2.01, t_, .3) + .25 * sine(f * 3.02, t_, .15)) * amp
    add(s, at, 1.0, pan)


# ---------- timeline ----------
# room tone with a deliberate "silence" window 43 → 49.5 (the knocking stops)
t = np.arange(N) / SR
room = filt(rng.standard_normal(N), None, 260) * .02 + np.sin(2 * np.pi * 50 * t) * .012 + np.sin(2 * np.pi * 100 * t) * .004
duck = np.interp(t, [0, 3, 4.5, 43, 43.2, 49.4, 50.2, 67, 70, 71], [0, 0, 1, 1, .2, .2, 1, 1, .15, 0])
room *= duck
L += room; R += room

# wind
wind = filt(rng.standard_normal(N), 180, 900)
wind_env = (np.interp(t, [0, 21, 24, 27, 30, 33, 43, 44, 46.5, 49, 51, 71], [0, 0, .05, .13, .05, 0, 0, .04, .11, .04, 0, 0])
            + .012 * duck)
wl = np.sin(2 * np.pi * .13 * t) * .5 + .5
L += wind * wind_env * (.6 + .4 * wl); R += wind * wind_env * (1 - .4 * wl)

# clock ticks
for k, ts in enumerate(np.arange(5.0, 60.0, 1.0)):
    c = filt(noise(.012), 2200, None) * env_exp(.012, .003, .0004)
    add(c, ts, .05 if k % 2 == 0 else .035, pan=.1)

# phone notifications
vibrate(11.95, .5, .14)
ding(12.0)
ding(13.6, base=988)
whoosh(16.2)
click(17.1); click(17.2); thump(17.35, .22, .8, 120); click(17.8); thump(17.95, .25, .8, 110)
ding(23.6); ding(25.4)
whoosh(27.6)
ding(38.6, base=880, amp=.26)
vibrate(38.55, .4, .12)
whoosh(45.2)
ding(47.4, base=830, amp=.2)
ding(61.0, base=740, amp=.2)
ding(63.2, base=622, amp=.24)

# doorbell (door is on the right side of the picture)
bell(30.0, 659); bell(30.55, 523)

# knocks – accelerating, then abrupt stop
tk, i = 34.0, 0
while tk < 42.95:
    thump(tk, .52 + min(.3, i * .03), .75, 78 + (i % 3) * 4)
    tk += max(.27, .95 - .075 * i); i += 1
# door handle rattle
for tr in np.arange(40.0, 42.8, .07):
    if rng.random() < .7:
        add(filt(noise(.03), 900, 6000) * env_exp(.03, .008), tr, .1 * rng.random() + .04, .8)

# heartbeat 44.5 → 60.4 (speeds up and grows)
hb, j = 44.5, 0
while hb < 60.3:
    a = .28 + min(.35, j * .025)
    add(sine(55, .22, .06, attack=.004), hb, a, 0)
    add(sine(50, .2, .05, attack=.004), hb + .22, a * .75, 0)
    hb += max(.52, 1.05 - j * .04); j += 1

# kitchen phone buzzing (left side), 3 pulses
for tv in (50.0, 50.62, 51.24):
    vibrate(tv, .4, .22, pan=-.8, f=190, cutoff=900)

# tension: dark drone + whine rising 50 → 60.4
dm = np.interp(t, [49, 60.4, 60.5, 63, 70], [0, .14, 0.05, .05, 0])
drone = filt(np.sign(np.sin(2 * np.pi * 55 * t)) + np.sign(np.sin(2 * np.pi * 58.2 * t)), None, 170)
L += drone * dm; R += drone * dm
fw = 1400 + (t - 50) * 105
wh = np.sin(2 * np.pi * np.cumsum(fw) / SR) * np.interp(t, [49.5, 60.4, 60.45, 71], [0, .02, 0, 0])
L += wh; R += wh

# the thing appears in the kitchen doorway (60.4) – low hit + swell, left side
add(sine(58, 2.2, .6, attack=.005), 60.4, .75, -.5)
add(filt(noise(1.6), None, 1400) * np.hanning(int(SR * 1.6)) ** .5 * env_exp(1.6, .7), 60.4, .5, -.4)
cluster = sum(filt(np.sign(np.sin(2 * np.pi * f * t_arr(2.2))), None, 1100) for f in (220, 233.1, 311.1))
add(cluster * env_exp(2.2, .8) * .12, 60.4, 1.0, -.3)
# creak
tc = t_arr(1.2)
creak = np.sin(2 * np.pi * np.cumsum(150 + 120 * np.sin(2 * np.pi * 3 * tc) + 60 * tc) / SR) * np.hanning(len(tc)) * .05
add(filt(creak, 100, 2200), 59.2, 1.0, -.6)

# final fade
fade = np.interp(t, [0, .3, 69.5, 71], [0, 1, 1, 0])
L *= fade; R *= fade

# soft limit / normalize
peak = max(np.abs(L).max(), np.abs(R).max(), 1e-9)
g = 0.9 / peak
L = np.tanh(L * g * 1.1) / np.tanh(1.1) * .95
R = np.tanh(R * g * 1.1) / np.tanh(1.1) * .95

out = sys.argv[1] if len(sys.argv) > 1 else 'audio.wav'
pcm = (np.stack([L, R], 1) * 32767).astype('<i2')
with wave.open(out, 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print('wrote', out, f'{DUR:.0f}s')
