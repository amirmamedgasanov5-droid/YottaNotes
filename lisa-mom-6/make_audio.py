#!/usr/bin/env python3
"""Sound design for episode 6, the finale (no voice). Usage: python3 -I make_audio.py out.wav"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'shared'))
import numpy as np
from sfx import Mix, SR

m = Mix(120.0, seed=61)
rng = m.rng


def chirp(at, f0, f1, amp, pan):
    tt = m.ta(.11)
    m.add(np.sin(2 * np.pi * np.cumsum(np.linspace(f0, f1, len(tt))) / SR) * np.hanning(len(tt)), at, amp, pan)


def birds(t0, t1, amp, density=2.0):
    t = t0
    while t < t1:
        f0 = 2400 + 1300 * rng.random()
        for k in range(int(rng.integers(2, 5))): chirp(t + k * .13, f0, f0 + (rng.random() - .3) * 900, amp * (.5 + rng.random()), rng.random() * 1.6 - .8)
        t += density * (.5 + rng.random())


# --- beds ---
m.room([(0, 0), (3, 0), (4.6, 1), (22, 1), (27.2, .4), (33, .4), (104, .3), (106, .8), (118, .8), (120, 0)], hum=100)
m.drone([(0, 0), (4.6, .1), (27, .16), (27.4, .06), (33.4, .1), (61, .13), (104, .2), (104.2, 0), (120, 0)])
m.heartbeat(4.6, 27.0, start_gap=.62, min_gap=.4, slope=.01, a0=.35, a1=.7)
m.heartbeat(33.5, 61.0, start_gap=1.0, min_gap=.62, slope=.03, a0=.25)
m.wind([(0, 0), (28, .0), (33, .04), (60, .07), (103, .05), (104.5, 0), (120, 0)])

# --- the chase: their quick steps, the echo's heavy ones behind ---
for a, b in ((4.6, 9.4), (9.6, 13.9), (14.1, 18.4), (18.6, 22.3)):
    for k, t in enumerate(np.arange(a, b, .23)): m.step(t, .17, m.lerp(-.7, .9, (t - a) / (b - a)))
    for t in np.arange(a - .5, b - .5, .5):
        if t > 0: m.thump(t + .6, .3, m.lerp(-.9, .6, (t - a) / (b - a)), 62)
# landing 5: Valya, the keys, door 47
m.creak(22.6, .8, 230, .07, .5); m.steps(22.9, 24.5, .3, .13, .3, -.1)
for t, p in ((24.8, 0), (25.25, -.1), (25.7, 0)): m.add(m.filt(m.noise(.09), 2500, 8000) * m.env(.09, .02, .0005) * .2 + m.sine(2600, .09, .03) * .07, t, 1.0, p)
m.creak(25.0, .9, 210, .07, -.4); m.steps(26.0, 26.9, .2, .17, -.3, .5)

# --- into the flat: slam, the lock turns itself ---
m.slam(27.9, .8, .9)
m.click(28.2, .3, .6); m.click(28.35, .25, .6); m.thump(28.5, .3, .6, 120)
m.steps(28.4, 29.6, .4, .12, .9, .3)
birds(29.5, 61.0, .012, 3.2)
m.breath(33.8, 2.4, .13, -.7); m.sting(34.0, .35, -.6)
m.rings(44.2, 46.6, gap=1.2, amp=.06); m.rings(48.6, 50.6, gap=1.2, amp=.06)
m.thump(58.5, .2, -.6, 66); m.thump(59.2, .26, -.4, 66)

# --- the tape plays: hiss, motor, and the echo getting weaker ---
m.add(m.filt(m.noise(44.5), 3500, None) * .018, 61.4, 1.0, 0)
m.add(np.sin(2 * np.pi * 60 * m.ta(44.5)) * .01, 61.4, 1.0, .0)
m.click(60.9, .25, 0); m.click(61.2, .2, 0)
for t, g in ((65.3, .12), (74.9, .1), (87.5, .08), (97.7, .06), (104.1, .05)):
    m.rings(t, t + 1.4, gap=1.4, amp=g, pan=-.6); m.breath(t - .3, 2.2, g, -.7)
m.whine(98, 104.2, f0=900, slope=60, peak=.014)

# --- freedom: the lock turns, the door opens, light ---
m.click(104.1, .3, .8); m.thump(104.3, .35, .8, 110)
m.creak(104.6, 1.0, 240, .08, .85)
d = 2.2; tt = m.ta(d)
m.add(m.filt(m.noise(d), 500, 4000) * np.hanning(len(tt)) ** 2 * .12 * np.linspace(1, 0, len(tt)), 104.6, 1.0, .3)   # the echo leaves
m.click(106.1, .15, 0)
pad = np.zeros(m.N)
for f, a in ((261.6, .05), (329.6, .04), (392.0, .04), (523.3, .025)):
    pad += np.sin(2 * np.pi * f * m.t) * a
m.bed(pad, [(0, 0), (104.3, 0), (112, 1), (116.5, 1), (120, 0)])
birds(105.0, 118.0, .03, 1.1)
m.ding(113.5, 988, .2, 0); m.ding(115.6, 1175, .22, 0)

m.save(sys.argv[1] if len(sys.argv) > 1 else 'audio6.wav', fade=(.3, 1.5))
