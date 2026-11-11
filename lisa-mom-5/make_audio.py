#!/usr/bin/env python3
"""Sound design for episode 5 (no voice). Usage: python3 -I make_audio.py out.wav"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'shared'))
import numpy as np
from sfx import Mix

m = Mix(120.0, seed=51)
rng = m.rng

# --- beds ---
m.room([(0, 0), (3, 0), (4.6, 1), (24.4, 1), (24.6, .5), (62, .5), (62.2, .8), (89.9, .8), (90.0, 0), (120, 0)], hum=50)
m.wind([(0, 0), (24.5, .03), (62, .08), (62.2, .02), (89.9, .02), (90, 0), (120, 0)])
m.drone([(0, 0), (31, 0), (39, .08), (61.9, .16), (62, .03), (89.9, .05), (90.2, .14), (116.5, .2), (117, .02), (120, 0)])
m.tick(5.0, 20.0)
m.heartbeat(39.0, 62.0, start_gap=1.0, min_gap=.6, slope=.03, a0=.22)
m.heartbeat(92.0, 117.0, start_gap=1.0, min_gap=.46, slope=.03, a0=.3, a1=.7)

# --- Baba Valya's key, phones off ---
for t, f in ((5.6, 3300), (6.0, 4300), (6.5, 3300)):
    m.add(m.sine(f, .25, .06, attack=.0005) * .12 + m.sine(f * 1.51, .25, .04, attack=.0005) * .06, t, 1.0, .5)
for t, p in ((14.7, -.3), (15.15, .3)):
    d = .35; tt = m.ta(d); fr = np.linspace(700, 180, len(tt))
    m.add(np.sin(2 * np.pi * np.cumsum(fr) / 44100) * np.hanning(len(tt)) * .12, t, 1.0, p)

# --- leaving the flat ---
m.steps(20.0, 23.6, .45, .12, .2, .9)
m.creak(20.5, .9, 230, .07, .85)

# --- landing 5: steps, with something repeating them half a second later ---
for k, t in enumerate(np.arange(25.6, 31.0, .62)):
    m.step(t, .13, .0)
    m.step(t + .55, .07 + .01 * k, -.8)
# --- landing 3: it creaks, they stop, the echo steps go on alone ---
for t in np.arange(32.0, 35.4, .62):
    m.step(t, .13, 0); m.step(t + .55, .1, -.8)
m.creak(33.2, .8, 200, .06, -.6); m.creak(34.4, .7, 170, .06, -.7)
m.step(36.0, .14, -.6); m.step(36.7, .16, -.5); m.step(37.4, .18, -.4)
m.creak(37.9, .9, 190, .07, -.4)

# --- landing 2: it climbs the stairs, stops next to them, goes on ---
t, i = 44.6, 0
while t < 56.0:
    m.thump(t, .16 + .045 * i, m.lerp(-.9, .15, (t - 44.6) / 11.4), 60); t += 1.1; i += 1
m.breath(56.6, 2.6, .14, .1); m.breath(58.0, 2.0, .08, .1)
for k, t in enumerate(np.arange(59.8, 62.0, .9)): m.thump(t, .4 - .12 * k, .1, 60)

# --- basement ---
m.steps(62.5, 65.0, .46, .13, .9, .0)
for tdrip in np.sort(rng.uniform(63, 117, 26)):
    f = 1700 + 700 * rng.random()
    m.add(m.sine(f, .35, .05, attack=.0005) * .05 + m.sine(f * 1.5, .35, .03) * .02, tdrip, 1.0, rng.uniform(-.9, .9))
for t, p in ((68.2, .0), (68.8, -.1), (69.4, 0.0)):
    m.add(m.filt(m.noise(.09), 2500, 8000) * m.env(.09, .02, .0005) * .22 + m.sine(2600, .09, .03) * .08, t, 1.0, p)
m.thump(70.5, .3, -.1, 120)
m.add(m.filt(m.noise(1.4), None, 600) * (.6 + .4 * m.sine(9, 1.4, 99)) * np.hanning(int(44100 * 1.4)) * .18, 73.2, 1.0, -.3)  # dragging the box
for t in np.arange(76.6, 78.2, .11):
    m.add(m.filt(m.noise(.06), 2500, 9000) * m.env(.06, .015) * .08 * rng.random(), t, 1.0, -.2)  # paper
m.click(86.5, .2, 0); m.click(86.6, .15, 0)
# --- the bulb dies ---
m.add(m.filt(m.noise(.2), 3000, None) * m.env(.2, .03, .0005) * .5, 90.0, 1.0, 0)
m.add(m.sine(70, .5, .12, attack=.002) * .5, 90.0, 1.0, 0)
# --- steps from above, a call in the dark ---
t, i = 96.0, 0
while t < 109:
    m.thump(t, .14 + .035 * i, .85, 66); t += max(.8, 1.4 - .06 * i); i += 1
m.breath(100.0, 2.2, .17, .85)
m.whine(109, 117, f0=1100, slope=110, peak=.02)
m.ding(109.2, 440, .12, -.5); m.ding(112.4, 392, .22, 0)
m.sting(112.4, .3, 0)
m.sting(116.7, .5, .3)

m.save(sys.argv[1] if len(sys.argv) > 1 else 'audio5.wav')
