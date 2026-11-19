#!/usr/bin/env python3
"""Music + sound effects for the "Amir in UFC" parody (no voices). Usage: python3 -I make_audio.py out.wav"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'shared'))
import numpy as np
from sfx import Mix, SR

m = Mix(300.0, seed=77)
rng = m.rng
ta = m.ta


def hz(n): return 440 * 2 ** ((n - 69) / 12)


# ---------- helpers ----------
def slide(at, f0, f1, d=.5, amp=.2, pan=0.0):
    tt = ta(d); fr = np.geomspace(f0, f1, len(tt))
    s = np.sin(2 * np.pi * np.cumsum(fr) / SR) + .3 * np.sin(4 * np.pi * np.cumsum(fr) / SR)
    m.add(s * np.hanning(len(tt)) ** .5 * amp, at, 1.0, pan)


def boing(at, amp=.25, pan=0.0):
    d = .55; tt = ta(d); fr = 260 + 180 * np.sin(2 * np.pi * 9 * tt) * np.exp(-tt * 4) + 120 * np.exp(-tt * 3)
    m.add(np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-tt * 5) * amp, at, 1.0, pan)


def punch(at, amp=.45, pan=0.0, f=110):
    m.add(m.filt(m.noise(.22), 500, 6000) * np.hanning(int(SR * .22)) * amp * .5, at - .08, 1.0, pan)
    m.thump(at, amp, pan, f)


def buzzer(at, d=.6, amp=.16):
    m.add(m.square(130, d) * .6 + m.square(137, d) * .4, at, amp, 0)


def bellring(at, amp=.3):
    d = 1.8
    m.add((m.sine(880, d, .55) + .6 * m.sine(1320, d, .4) + .4 * m.sine(2200, d, .25)) * amp, at, 1.0, 0)


def munch(at, n=6, amp=.1):
    for k in range(n): m.add(m.filt(m.noise(.08), 300, 2500) * m.env(.08, .03, .003) * amp * (.6 + .4 * rng.random()), at + k * .22, 1.0, 0)


def slurp(at, amp=.12):
    d = .9; tt = ta(d); m.add(m.filt(m.noise(d), 600, 3500) * np.hanning(len(tt)) * (.6 + .4 * np.sin(2 * np.pi * 14 * tt)) * amp, at, 1.0, .3)


def chime(at, notes=(72, 76, 79, 84), amp=.12, gap=.09):
    for i, n in enumerate(notes): m.add(m.sine(hz(n), .6, .18) * amp, at + i * gap, 1.0, 0)


def clang(at, amp=.25, pan=.2):
    m.add((m.sine(520, 1.0, .25) + .6 * m.sine(1370, 1.0, .18) + .4 * m.sine(2410, 1.0, .1)) * amp, at, 1.0, pan)


def shutter(at, amp=.12):
    m.click(at, amp, 0); m.click(at + .06, amp * .8, 0)


def babble(t0, t1, amp=.04, pan=.5):
    t = t0
    while t < t1:
        f = 140 + 120 * rng.random(); d = .07 + .05 * rng.random()
        m.add(m.square(f, d) * m.env(d, d * .6, .004) * amp, t, 1.0, pan); t += .1 + .09 * rng.random()


def fanfare(at, amp=.1):
    for i, n in enumerate((60, 64, 67, 72, 67, 72)):
        d = .16 if i < 5 else .7
        m.add((m.square(hz(n), d) * .5 + m.sine(hz(n), d, 99) * .5) * m.env(d, d * .7, .004) * amp, at + i * .14, 1.0, 0)


# ---------- chords & music ----------
CH = {'Am': (45, (57, 60, 64)), 'F': (41, (53, 57, 60)), 'C': (48, (60, 64, 67)), 'G': (43, (55, 59, 62)), 'Dm': (38, (50, 53, 57)), 'E': (40, (52, 56, 59))}


def music(t0, t1, bpm, prog, vol, swing=False):
    beat = 60 / bpm; t = t0; bar = 0
    while t < t1 - .01:
        root, tri = CH[prog[bar % len(prog)]]
        for b in range(4):
            tb = t + b * beat
            if tb >= t1: break
            if b in (0, 2): m.add(m.square(hz(root + (12 if b == 2 and bpm > 130 else 0)), beat * .9) * m.env(beat * .9, .25, .004) * vol * 1.3, tb, 1.0, -.1)
            for k in range(2):
                n = tri[(b * 2 + k) % 3]
                tt = tb + k * beat / 2
                if tt >= t1: break
                d = beat / 2
                m.add(np.sin(2 * np.pi * hz(n + 12) * ta(d)) * m.env(d, d * .8, .003) * vol * .9 + np.sign(np.sin(2 * np.pi * hz(n) * ta(d))) * m.env(d, d * .5, .003) * vol * .25, tt, 1.0, .15)
            m.add(m.filt(m.noise(.04), 6000, None) * m.env(.04, .012, .001) * vol * .5, tb + beat / 2, 1.0, .3)
            if bpm > 120: m.add(m.sine(70, .12, .04, attack=.002) * vol * 1.6, tb, 1.0, 0)   # kick
        t += 4 * beat; bar += 1


music(6.5, 42.3, 108, ['C', 'G', 'Am', 'F'], .05)
music(42.4, 92.3, 116, ['C', 'Am', 'F', 'G'], .05)
music(92.4, 125.3, 100, ['Am', 'F', 'C', 'G'], .028)
music(125.5, 172.2, 112, ['Am', 'F', 'G', 'Am'], .055)
music(172.3, 259.8, 138, ['Am', 'F', 'C', 'G'], .06)
music(259.9, 269.9, 176, ['Am', 'G', 'F', 'G'], .07)
music(275.2, 282.8, 120, ['C', 'G', 'Am', 'F'], .05)
music(283.0, 297.6, 92, ['C', 'F', 'G', 'C'], .05)

# ---------- title + scene 1: Amir's room ----------
slide(.6, 200, 1400, 1.0, .18); punch(1.6, .6, 0, 70); chime(2.0, (72, 79, 84, 88), .1)
m.room([(0, 0), (6, 0), (6.5, .6), (300, .0)], hum=50)
for t in (13.4, 14.4, 15.4, 16.6): m.whoosh(t, .12, .2); punch(t + .1, .18, .2, 100)
m.whoosh(17.0, .15, .5); punch(17.4, .55, .6, 85)
for t in np.arange(17.45, 18.6, .08):
    m.add(m.filt(m.noise(.03), 500, 3000) * m.env(.03, .01) * .09 * rng.random(), t, 1.0, .7)
slide(18.0, 1200, 200, .7, .2, .2); m.thump(19.0, .4, -.5, 70)
m.vibrate(35.45, .45, .12, 0, 175); m.ding(35.5, 988, .18, 0)
boing(38.9, .25); chime(39.2, (67, 72, 76, 79, 84), .1, .1)

# ---------- scene 2: the gym ----------
m.steps(42.4, 44.4, .4, .12, -.8, -.1)
m.add(m.sine(2200, .08, .03) * .08, 48.0, 1.0, 0); m.add(m.sine(2600, .06, .03) * .06, 48.5, 1.0, 0)
m.steps(51.0, 57.2, .2, .14, -.5, .5)
slide(57.4, 900, 180, .6, .2, -.2); m.thump(58.0, .35, -.4, 80)
m.steps(59.0, 67.4, .62, .14, -.3, .6)
for t in np.arange(59.2, 67.4, .62): m.thump(t, .12, .2, 90)
m.thump(67.7, .3, .5, 100)
slurp(71.2)
boing(77.9, .2, .7); slide(78.3, 600, 1500, .25, .08, .7)
m.steps(86.4, 87.3, .17, .16, -.2, .3); punch(87.5, .5, .1, 95); clang(87.55, .22); slide(87.6, 1000, 150, .7, .18, -.3); m.thump(88.3, .35, -.6, 80)
chime(90.4, (72, 76, 79), .12)

# ---------- TV ----------
m.click(92.4, .2, .7); m.add(m.filt(m.noise(.5), 1000, 6000) * np.hanning(int(SR * .5)) * .06, 92.4, 1.0, .5)
chime(92.5, (74, 79, 83, 86), .1, .1)
for t in (99.6, 100.0): m.add(m.sine(2400, .1, .03) * .1, t, 1.0, .5); boing(t + .05, .1, .5)
m.click(112.2, .15, .5)

# ---------- weigh-in ----------
m.steps(125.6, 127.4, .38, .12, .8, .1)
for t in (128.0, 135.4, 144.2, 152.4, 160.1, 168.2): shutter(t)
tk = 128.0
while tk < 130.5: m.click(tk, .12, 0); tk += max(.03, .12 - (tk - 128) * .03)
buzzer(129.4, .7); boing(130.3, .22, 0); m.add(m.sine(2000, .1, .04) * .1, 131.6, 1.0, .3)
m.steps(139.6, 141.4, .4, .12, -.8, -.1)
chime(144.2, (79, 83, 86, 91), .1)
munch(148.6, 8)
m.add(m.filt(m.noise(.12), 200, 1200) * m.env(.12, .04) * .15, 152.0, 1.0, -.3)
for t in np.arange(156.5, 160.0, .5): m.add(m.sine(60, .2, .06) * .3, t, 1.0, 0)
m.add(m.sine(2800, .05, .02) * .1, 160.0, 1.0, 0); boing(160.1, .15, 0)

# ---------- arena ----------
crowd = m.filt(rng.standard_normal(m.N), 500, 3000)
swell = np.interp(m.t, [172, 173, 180, 181, 195, 199.5, 200.4, 203, 209, 211, 217, 234, 238, 246, 255, 269.5, 270.1, 275, 278.9, 282.8, 283.5],
                  [0, .02, .02, .05, .03, .03, .12, .03, .08, .04, .05, .05, .09, .05, .06, .06, 0, 0, .05, .04, 0])
m.L += crowd * swell; m.R += crowd * swell
for t in (172.3, 172.8, 173.3, 180.0, 180.4, 180.8, 211.0, 211.4, 211.8): bellring(t, .22)
babble(188.5, 198.2, .035, .5)
m.steps(199.0, 199.9, .3, .1, .3, .4)
slide(200.0, 300, 1500, .35, .18, .4); slide(200.4, 1500, 120, .6, .16, .4); m.thump(201.3, .45, .6, 75)
chime(201.0, (84, 88, 91, 96, 91, 88), .06, .12)
for t in (203.5, 204.4, 205.3, 206.0, 206.6): m.add(m.sine(1000, .1, .05) * .08, t, 1.0, .4)
m.add(m.filt(m.noise(.5), 300, 5000) * np.hanning(int(SR * .5)) * np.linspace(1, .1, int(SR * .5)) * .2, 207.2, 1.0, 0)   # record scratch
slide(209.8, 200, 700, .4, .12, .6)
for t, a in ((216.6, .16), (217.2, .2), (218.7, .16), (219.2, .2)): m.whoosh(t, a, -.2 if a < .2 else .2)
for t in (216.7, 218.9, 220.0): m.whoosh(t, .08, -.5)
for t in np.arange(224.7, 228.0, .5): m.add(m.sine(60, .2, .06) * .25, t, 1.0, 0)
m.thump(234.9, .4, .3, 90)
for t in np.arange(236, 246.0, .42): m.thump(t, .2, m.lerp(-.5, .6, (t % 2) / 2), 75)
for t in (238.0, 240.0, 242.0, 244.0): boing(t, .14, .2)
m.steps(246.2, 250.6, .2, .16, -.4, .5)
m.add(m.sine(2000, .08, .03) * .08, 246.4, 1.0, 0)
for t in np.arange(250.7, 254.7, .8): m.thump(t, .16, -.6, 70)
m.add((np.sign(np.sin(2 * np.pi * 110 * ta(.8))) * .5 + np.sign(np.sin(2 * np.pi * 165 * ta(.8))) * .3) * m.env(.8, .3, .01) * .12, 250.7, 1.0, -.3)
m.steps(255.8, 264.0, .17, .15, -.6, .6)
for t in np.arange(255.8, 264.0, .85): m.thump(t, .12, -.5, 85)
for t in np.arange(264.2, 266.2, .09): m.add(m.filt(m.noise(.04), 1500, 7000) * m.env(.04, .012) * .13 * rng.random(), t, 1.0, .8)
for t in np.arange(260.0, 269.0, 1.6): slide(t, 500, 1300, .2, .06, 0)   # little comic slides during the chase
# the loudspeaker: chime, squeal, and silence
chime(269.9, (79, 76, 72), .12, .2)
m.add(np.sin(2 * np.pi * 3200 * ta(.35)) * np.hanning(int(SR * .35)) * .06, 270.1, 1.0, 0)
m.thump(270.3, .0, 0, 60)
chime(278.9, (72, 76, 79, 84), .1)
m.thump(275.7, .3, .8, 75); m.steps(279.0, 282.4, .17, .15, .5, 1.0)
for t in (276.0, 291.0): slurp(t)

# ---------- dinner ----------
m.room([(0, 0), (282.8, 0), (283.2, .4), (298, .4), (300, 0)], hum=60)
m.add(m.filt(m.noise(1.2), 2000, 8000) * np.hanning(int(SR * 1.2)) * .08, 283.5, 1.0, 0)   # steam
for t in np.sort(rng.uniform(284, 294, 14)): m.add(m.sine(rng.choice([2400, 3100, 1900]), .12, .03) * .07, t, 1.0, rng.uniform(-.8, .8))
munch(285.0, 10, .06); munch(290.5, 8, .06)
fanfare(294.4, .1); chime(297.8, (72, 76, 79, 84, 88), .12, .1)
m.add(m.filt(m.noise(1.4), 4000, None) * m.env(1.4, .4) * .1, 297.8, 1.0, 0)

m.save(sys.argv[1] if len(sys.argv) > 1 else 'ufc_sfx.wav', fade=(.3, 2.0))
