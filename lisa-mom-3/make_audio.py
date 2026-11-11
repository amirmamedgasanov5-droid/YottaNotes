#!/usr/bin/env python3
"""Sound design (no voice) for "Сообщение от мамы", episode 3. Timings match index.html.

Usage: python3 -I make_audio.py out.wav
"""
import sys, wave
import numpy as np

SR = 44100
DUR = 70.0
N = int(SR * DUR)
rng = np.random.default_rng(23)
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
def lerp(a, b, k): return a + (b - a) * k


def ding(at, base=988, amp=.22, pan=.2):
    add(sine(base, .5, .12) + .4 * sine(base * 2, .5, .08), at, amp, pan)
    add(sine(base * 1.335, .8, .22) + .35 * sine(base * 2.67, .8, .12), at + .13, amp, pan)


def vibrate(at, d=.45, amp=.12, pan=.2, f=175, cutoff=700):
    tt = t_arr(d); am = .55 + .45 * np.sign(np.sin(2 * np.pi * 28 * tt))
    s = filt(square(f, d) * am, None, cutoff) * np.minimum(1, tt / .01) * np.minimum(1, (d - tt) / .03)
    add(s, at, amp, pan)
    add(filt(noise(d), 2500, None) * am * .35 * np.minimum(1, tt / .01), at, amp * .5, pan)


def click(at, amp=.3, pan=.8):
    add(filt(noise(.03), 1500, None) * env_exp(.03, .004, .0005) + .7 * sine(1900, .03, .006), at, amp, pan)


def thump(at, amp=.5, pan=.7, f=80):
    add(sine(f, .3, .07, attack=.004) + .5 * sine(f * 1.9, .3, .03), at, amp, pan)
    add(filt(noise(.08), None, 450) * env_exp(.08, .02), at, amp * .6, pan)


def soft_step(at, amp=.12, pan=0.0):
    add(filt(noise(.09), 120, 900) * env_exp(.09, .025, .004), at, amp, pan)


def shutter(at, amp=.35):
    click(at, amp, 0); click(at + .07, amp * .8, 0)
    add(sine(1400, .05, .012) * .2, at + .02, 1.0, 0)


def chirp(at, f0, f1, amp, pan):
    d = .11
    tt = t_arr(d); fr = np.linspace(f0, f1, len(tt))
    add(np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.hanning(len(tt)), at, amp, pan)


# ---------- morning: room tone, birds, ticking clock ----------
room = filt(rng.standard_normal(N), None, 300) * .018 + np.sin(2 * np.pi * 50 * t) * .008
room *= np.interp(t, [0, 3, 4.6, 32.3, 32.6, 33, 41, 52, 52.2, 53.6, 59.7, 64, 70], [0, 0, 1, 1, .3, .3, .6, .6, 0, .5, .2, .1, 0])
L += room; R += room

birds = 0
tb = 4.6
while tb < 32.3:
    f0 = 2400 + 1300 * rng.random(); n = int(rng.integers(2, 5))
    for k in range(n):
        chirp(tb + k * .13, f0, f0 + (rng.random() - .3) * 900, .018 + .02 * rng.random(), rng.random() * 1.6 - .8)
    tb += 1.2 + 2.6 * rng.random()

for tc in np.arange(5.0, 32.3, 1.0):
    add(filt(noise(.012), 2200, None) * env_exp(.012, .003, .0004), tc, .035, .15)

# ---------- entering: door, steps ----------
tcr = t_arr(.7)
creak = np.sin(2 * np.pi * np.cumsum(240 + 90 * np.sin(2 * np.pi * 6 * tcr) - 110 * tcr) / SR) * np.hanning(len(tcr)) * .07
add(filt(creak, 120, 2600), 5.0, 1.0, .85)
for k, ts in enumerate(np.arange(5.9, 9.0, .42)): soft_step(ts, .13, lerp(.85, .2, k / 7))
for k, ts in enumerate(np.arange(6.4, 9.6, .4)): soft_step(ts, .12, lerp(.9, .1, k / 8))
# mom to the kitchen and back
for k, ts in enumerate(np.arange(15.0, 19.2, .42)): soft_step(ts, .12, lerp(.2, -.8, k / 10))
add(sine(900, .05, .02) * .05, 20.4, 1.0, -.6); click(20.5, .12, -.6)           # picks the phone up
for k, ts in enumerate(np.arange(21.0, 25.2, .42)): soft_step(ts, .12, lerp(-.8, .2, k / 10))

# ---------- gallery ----------
ding(25.5, 880, .12, 0)
for tp in (28.6, 30.0, 32.4): shutter(tp, .12)
# birds stop, dread starts at 32.4
dm = np.interp(t, [0, 32.2, 33, 41, 45, 52, 52.2, 53.6, 59, 66, 66.1, 70], [0, 0, .06, .14, .2, .22, 0, 0, 0, .12, .04, 0])
drone = filt(np.sign(np.sin(2 * np.pi * 55 * t)) + np.sign(np.sin(2 * np.pi * 58.2 * t)), None, 170)
L += drone * dm; R += drone * dm

# heartbeat 37 → 52
hb, j = 37.0, 0
while hb < 52:
    a = .24 + min(.35, j * .03)
    add(sine(55, .22, .06, attack=.004), hb, a, 0)
    add(sine(50, .2, .05, attack=.004), hb + .22, a * .75, 0)
    hb += max(.55, 1.0 - j * .05); j += 1

# the phone takes a photo by itself (41.0)
shutter(41.0, .7)
add(filt(noise(.5), 1000, 8000) * env_exp(.5, .1) * .22, 41.0, 1.0, 0)
add(sine(70, 1.2, .3, attack=.004) * .5, 41.0, 1.0, 0)

# something exhales behind them, left side (44 →)
for te, a in ((44.3, .1), (46.8, .13), (49.2, .16)):
    d = 1.9
    add(filt(noise(d), 350, 1600) * np.hanning(int(SR * d)) ** 2 * a, te, 1.0, -.7)
add(sine(58, 2.0, .6, attack=.005) * .4, 44.0, 1.0, -.6)

# turn around (52.0): silence, then sharp glitch
add(filt(noise(.3), 2000, None) * env_exp(.3, .05, .0005) * .5, 52.1, 1.0, -.2)
add(sine(48, 1.3, .35, attack=.003) * .7, 52.1, 1.0, -.2)

# run for the door 55 → 58.8, slam 59.6
st = 55.0
while st < 58.6:
    thump(st, .3, lerp(.0, .9, (st - 55) / 3.6), 110); st += .19
add(sine(50, 1.6, .5, attack=.003) * .9, 59.6, 1.0, .8)
add(filt(noise(.5), None, 900) * env_exp(.5, .09, .001) * .9, 59.6, 1.0, .8)

# the message (61.0, 63.0) and the eyes (62 → 66.6)
vibrate(60.95, .45, .2, pan=0); ding(61.0, 740, .2, 0)
vibrate(62.95, .45, .2, pan=0); ding(63.0, 622, .26, 0)
fw = 1300 + (t - 59.5) * 160
wh = np.sin(2 * np.pi * np.cumsum(fw) / SR) * np.interp(t, [59.5, 66, 66.05, 70], [0, .018, 0, 0])
L += wh; R += wh
add(sine(58, 2.4, .6, attack=.005) * .5, 66.0, 1.0, -.5)
cluster = sum(filt(np.sign(np.sin(2 * np.pi * f * t_arr(2.4))), None, 1100) for f in (220, 233.1, 311.1))
add(cluster * env_exp(2.4, .9) * .09, 66.0, 1.0, -.3)

fade = np.interp(t, [0, .3, 67.5, 70], [0, 1, 1, 0])
L *= fade; R *= fade
peak = max(np.abs(L).max(), np.abs(R).max(), 1e-9); g = .9 / peak
L = np.tanh(L * g * 1.1) / np.tanh(1.1) * .95
R = np.tanh(R * g * 1.1) / np.tanh(1.1) * .95
out = sys.argv[1] if len(sys.argv) > 1 else 'audio.wav'
pcm = (np.stack([L, R], 1) * 32767).astype('<i2')
with wave.open(out, 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print('wrote', out, f'{DUR:.0f}s')
