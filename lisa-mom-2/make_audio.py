#!/usr/bin/env python3
"""Sound design (no voice) for "Сообщение от мамы", episode 2. Timings match index.html.

Usage: python3 -I make_audio.py out.wav
"""
import sys, wave
import numpy as np

SR = 44100
DUR = 75.0
N = int(SR * DUR)
rng = np.random.default_rng(11)
L = np.zeros(N)
R = np.zeros(N)
t = np.arange(N) / SR


def t_arr(d): return np.arange(int(SR * d)) / SR


def filt(x, lo=None, hi=None):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR)
    if lo: X[f < lo] = 0
    if hi: X[f > hi] = 0
    return np.fft.irfft(X, len(x))


def noise(d): return rng.standard_normal(int(SR * d))


def env_exp(d, decay, attack=0.002):
    tt = t_arr(d); e = np.exp(-tt / decay); a = int(SR * attack)
    if a: e[:a] *= np.linspace(0, 1, a)
    return e


def add(sig, at, amp=1.0, pan=0.0):
    i = int(at * SR)
    if i >= N: return
    s = sig[: N - i]
    L[i:i + len(s)] += s * amp * np.cos((pan + 1) * np.pi / 4)
    R[i:i + len(s)] += s * amp * np.sin((pan + 1) * np.pi / 4)


def sine(f, d, decay, amp=1.0, attack=0.002): return np.sin(2 * np.pi * f * t_arr(d)) * env_exp(d, decay, attack) * amp
def square(f, d): return np.sign(np.sin(2 * np.pi * f * t_arr(d)))


def ding(at, base=988, amp=.22, pan=.2):
    add(sine(base, .5, .12) + .4 * sine(base * 2, .5, .08), at, amp, pan)
    add(sine(base * 1.335, .8, .22) + .35 * sine(base * 2.67, .8, .12), at + .13, amp, pan)


def vibrate(at, d=.45, amp=.12, pan=.2, f=175, cutoff=700):
    tt = t_arr(d); am = .55 + .45 * np.sign(np.sin(2 * np.pi * 28 * tt))
    s = filt(square(f, d) * am, None, cutoff) * np.minimum(1, tt / .01) * np.minimum(1, (d - tt) / .03)
    add(s, at, amp, pan)
    add(filt(noise(d), 2500, None) * am * .35 * np.minimum(1, tt / .01), at, amp * .5, pan)


def whoosh(at, amp=.1, pan=.3):
    d = .22
    add(filt(noise(d), 600, 5000) * np.hanning(int(SR * d)), at, amp, pan)
    add(sine(620, .08, .03), at + .17, amp * .8, pan)


def click(at, amp=.3, pan=.8):
    add(filt(noise(.03), 1500, None) * env_exp(.03, .004, .0005) + .7 * sine(1900, .03, .006), at, amp, pan)


def thump(at, amp=.5, pan=.7, f=80):
    add(sine(f, .3, .07, attack=.004) + .5 * sine(f * 1.9, .3, .03), at, amp, pan)
    add(filt(noise(.08), None, 450) * env_exp(.08, .02), at, amp * .6, pan)


def bell(at, f, amp=.32, pan=.8):
    d = 1.6
    add((sine(f, d, .5) + .5 * sine(f * 2.01, d, .3) + .25 * sine(f * 3.02, d, .15)) * amp, at, 1.0, pan)


def lerp(a, b, k): return a + (b - a) * k


# ---------- bed: room tone, wind, drone, heartbeat ----------
room = filt(rng.standard_normal(N), None, 260) * .02 + np.sin(2 * np.pi * 50 * t) * .012 + np.sin(2 * np.pi * 100 * t) * .004
room *= np.interp(t, [0, 3, 4.6, 45, 45.5, 47, 69.4, 75], [0, 0, 1, 1, .08, .8, .6, 0])
L += room; R += room

wind = filt(rng.standard_normal(N), 180, 900)
wenv = np.interp(t, [0, 4.6, 12, 20, 28, 36, 42, 45, 70, 75], [0, .02, .04, .09, .05, .12, .05, 0, .0, 0])
wl = np.sin(2 * np.pi * .13 * t) * .5 + .5
L += wind * wenv * (.6 + .4 * wl); R += wind * wenv * (1 - .4 * wl)

dm = np.interp(t, [0, 4.6, 38, 42, 45, 59, 66, 66.1, 70, 75], [0, .06, .16, .2, 0, 0, .12, .04, .03, 0])
drone = filt(np.sign(np.sin(2 * np.pi * 55 * t)) + np.sign(np.sin(2 * np.pi * 58.2 * t)), None, 170)
L += drone * dm; R += drone * dm

hb, j = 4.6, 0
while hb < 42:
    a = .22 + min(.35, j * .012)
    add(sine(55, .22, .06, attack=.004), hb, a, 0)
    add(sine(50, .2, .05, attack=.004), hb + .22, a * .75, 0)
    hb += max(.5, 1.0 - j * .014); j += 1

# the thing creeping closer (11 → 24): slow scraping
tc = t_arr(13)
scr = filt(noise(13), 300, 1800) * (.5 + .5 * np.sin(2 * np.pi * .35 * tc)) ** 2 * np.interp(tc, [0, 13], [0, .07])
add(scr, 11, 1.0, -.5)

# ---------- phone messages ----------
vibrate(11.15, .5, .14); ding(11.2, 740)
whoosh(13.6)
vibrate(15.75, .4, .14); ding(15.8, 622, .26)
ding(24.2, 622, .24)
vibrate(32.95, .4, .14); ding(33.0, 560, .26)
ding(35.0, 560, .26); vibrate(34.95, .3, .12)
ding(36.6, 560, .28); vibrate(36.55, .3, .12)

# ---------- doorbell + soft knocks (real mom) ----------
bell(22.0, 659); bell(22.55, 523)
for tk, a in ((23.2, .32), (23.75, .3), (24.5, .28)): thump(tk, a, .75, 90)

# ---------- keys, the run, the thing rushes ----------
for t0, t1 in ((37.9, 38.7), (38.9, 39.7)):
    tt = t0
    while tt < t1:
        add(filt(noise(.05), 3000, 9000) * env_exp(.05, .012, .0005), tt, .09 + .07 * rng.random(), lerp(0, .7, (tt - 37.9) / 1.8))
        tt += .05 + .05 * rng.random()
st = 38.0
while st < 39.7:
    thump(st, .28, lerp(0, .85, (st - 38) / 1.7), 110); st += .2
st = 38.5
while st < 40.35:
    thump(st, .52, lerp(-.4, .75, (st - 38.5) / 1.85), 62); st += .17
gt = t_arr(3.6)
growl = filt(np.sign(np.sin(2 * np.pi * np.cumsum(np.linspace(70, 42, len(gt))) / SR)) * (.7 + .3 * np.sin(2 * np.pi * 9 * gt)), None, 220)
add(growl * np.hanning(len(gt)) ** .5 * .5, 38.5, 1.0, .1)

# lock + door open
click(39.7); click(39.8, .25); thump(39.9, .2, .8, 120); click(39.98); thump(40.1, .25, .8, 105)
tcr = t_arr(.8)
creak = np.sin(2 * np.pi * np.cumsum(240 + 90 * np.sin(2 * np.pi * 6 * tcr) - 110 * tcr) / SR) * np.hanning(len(tcr)) * .07
add(filt(creak, 120, 2600), 40.2, 1.0, .85)

# mom drags Lisa out, door slams, pounding from inside
st = 41.0
while st < 41.95:
    thump(st, .25, lerp(.8, 1.0, (st - 41) / .95), 120); st += .14
d = 1.6
add(sine(50, d, .5, attack=.003) * .9, 42.0, 1.0, .8)
add(filt(noise(.5), None, 900) * env_exp(.5, .09, .001) * .9, 42.0, 1.0, .8)
add(filt(noise(d), 200, 3000) * env_exp(d, .35, .004) * .08, 42.05, 1.0, .5)
for tp, a in ((42.25, .75), (42.6, .7), (43.05, .65), (43.65, .5), (44.4, .35), (45.4, .2)): thump(tp, a, .8, 70)

# ---------- police outside ----------
ts = t_arr(21)
sir_f = np.where((ts * 1.45) % 1 < .5, 640 + 0 * ts, 880 + 0 * ts)
sir_f = np.convolve(sir_f, np.ones(900) / 900, 'same')
siren = np.sign(np.sin(2 * np.pi * np.cumsum(sir_f) / SR)) * .5 + np.sin(2 * np.pi * np.cumsum(sir_f) / SR) * .5
siren = filt(siren, 400, 3200) * np.interp(ts, [0, 4, 9, 15, 20, 21], [0, .04, .07, .04, .01, 0])
add(siren, 46.5, 1.0, -.25)
for tcl in np.arange(47.0, 59.5, 1.0):
    add(filt(noise(.012), 2200, None) * env_exp(.012, .003, .0004), tcl, .04, .1)
for rs in (52.0, 54.6, 57.0):  # radio squelch
    add(filt(noise(.35), 700, 3200) * np.hanning(int(SR * .35)) * .05, rs, 1.0, .5)
    add(sine(1100, .05, .02) * .03, rs + .35, 1.0, .5)

# ---------- the end: the phone and the eyes ----------
for tv in (59.2, 59.82, 60.44): vibrate(tv, .4, .22, pan=-.8, f=190, cutoff=900)
ding(61.0, 740, .2); ding(63.0, 622, .26)
fw = 1400 + (t - 59) * 150
wh = np.sin(2 * np.pi * np.cumsum(fw) / SR) * np.interp(t, [58.5, 66, 66.05, 75], [0, .02, 0, 0])
L += wh; R += wh
add(sine(58, 2.4, .6, attack=.005), 66.0, .55, -.5)
cluster = sum(filt(np.sign(np.sin(2 * np.pi * f * t_arr(2.4))), None, 1100) for f in (220, 233.1, 311.1))
add(cluster * env_exp(2.4, .9) * .1, 66.0, 1.0, -.3)
tc2 = t_arr(1.2)
creak2 = np.sin(2 * np.pi * np.cumsum(150 + 120 * np.sin(2 * np.pi * 3 * tc2) + 60 * tc2) / SR) * np.hanning(len(tc2)) * .05
add(filt(creak2, 100, 2200), 65.2, 1.0, -.6)

fade = np.interp(t, [0, .3, 72.5, 75], [0, 1, 1, 0])
L *= fade; R *= fade
peak = max(np.abs(L).max(), np.abs(R).max(), 1e-9); g = .9 / peak
L = np.tanh(L * g * 1.1) / np.tanh(1.1) * .95
R = np.tanh(R * g * 1.1) / np.tanh(1.1) * .95
out = sys.argv[1] if len(sys.argv) > 1 else 'audio.wav'
pcm = (np.stack([L, R], 1) * 32767).astype('<i2')
with wave.open(out, 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print('wrote', out, f'{DUR:.0f}s')
