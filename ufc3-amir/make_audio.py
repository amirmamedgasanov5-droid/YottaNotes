#!/usr/bin/env python3
"""Music + sound effects for 'Амир и орёл' (no voices). Event times come from lines.json keys.
Usage: python3 -I make_audio.py out.wav"""
import sys, os, json
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'shared'))
import numpy as np
from sfx import Mix, SR

LINES = {l['key']: l for l in json.load(open(os.path.join(HERE, 'lines.json'), encoding='utf-8'))}
SCN = {}
for l in LINES.values(): SCN.setdefault(l['scene'], l['t'])
T0 = lambda k: LINES[k]['t']; T1 = lambda k: LINES[k]['end']
cutT = lambda n: SCN[n] - .45
END = 300.5
m = Mix(END, seed=88); rng = m.rng; ta = m.ta
hz = lambda n: 440 * 2 ** ((n - 69) / 12)

# ---------------- helpers ----------------
def slide(at, f0, f1, d=.5, amp=.2, pan=0.0):
    tt = ta(d); fr = np.geomspace(f0, f1, len(tt)); s = np.sin(2 * np.pi * np.cumsum(fr) / SR) + .3 * np.sin(4 * np.pi * np.cumsum(fr) / SR)
    m.add(s * np.hanning(len(tt)) ** .5 * amp, at, 1.0, pan)
def boing(at, amp=.22, pan=0.0):
    d = .55; tt = ta(d); fr = 260 + 180 * np.sin(2 * np.pi * 9 * tt) * np.exp(-tt * 4) + 120 * np.exp(-tt * 3)
    m.add(np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-tt * 5) * amp, at, 1.0, pan)
def punch(at, amp=.4, pan=0.0, f=110):
    m.add(m.filt(m.noise(.22), 500, 6000) * np.hanning(int(SR * .22)) * amp * .5, at - .08, 1.0, pan); m.thump(at, amp, pan, f)
def miss(at, amp=.12, pan=0.0): m.whoosh(at, amp, pan)
def clang(at, amp=.22, pan=.2): m.add((m.sine(520, 1.0, .25) + .6 * m.sine(1370, 1.0, .18) + .4 * m.sine(2410, 1.0, .1)) * amp, at, 1.0, pan)
def shutter(at, amp=.1): m.click(at, amp, 0); m.click(at + .06, amp * .8, 0)
def chime(at, notes=(72, 76, 79, 84), amp=.12, gap=.09):
    for i, n in enumerate(notes): m.add(m.sine(hz(n), .6, .18) * amp, at + i * gap, 1.0, 0)
def munch(at, n=6, amp=.1):
    for k in range(n): m.add(m.filt(m.noise(.08), 300, 2500) * m.env(.08, .03, .003) * amp * (.6 + .4 * rng.random()), at + k * .22, 1.0, 0)
def bellring(at, amp=.28):
    d = 1.8; m.add((m.sine(880, d, .55) + .6 * m.sine(1320, d, .4) + .4 * m.sine(2200, d, .25)) * amp, at, 1.0, 0)
def snore(t0, t1, amp=.07, pan=.7):
    t = t0
    while t < t1:
        m.add(m.filt(m.noise(1.1), 80, 380) * np.hanning(int(SR * 1.1)) ** 2 * amp, t, 1.0, pan); t += 2.4
def pop(at, amp=.12, pan=0.0): m.add(m.sine(700, .12, .03) * amp + m.sine(1100, .12, .02) * amp * .5, at, 1.0, pan)
def crowd(env_pts): m.bed(m.filt(rng.standard_normal(m.N), 500, 3000), env_pts)
def scribble(t0, t1, amp=.05):
    t = t0
    while t < t1: m.add(m.filt(m.noise(.12), 2500, 9000) * np.hanning(int(SR * .12)) * amp, t, 1.0, 0); t += .14
def fanfare(at, amp=.1):
    for i, n in enumerate((60, 64, 67, 72, 67, 72)):
        d = .16 if i < 5 else .7; m.add((m.square(hz(n), d) * .5 + m.sine(hz(n), d, 99) * .5) * m.env(d, d * .7, .004) * amp, at + i * .14, 1.0, 0)

CH = {'Am': (45, (57, 60, 64)), 'F': (41, (53, 57, 60)), 'C': (48, (60, 64, 67)), 'G': (43, (55, 59, 62)), 'Dm': (38, (50, 53, 57)), 'E': (40, (52, 56, 59))}
def music(t0, t1, bpm, prog, vol):
    beat = 60 / bpm; t = t0; bar = 0
    while t < t1 - .01:
        root, tri = CH[prog[bar % len(prog)]]
        for b in range(4):
            tb = t + b * beat
            if tb >= t1: break
            if b in (0, 2): m.add(m.square(hz(root + (12 if b == 2 and bpm > 130 else 0)), beat * .9) * m.env(beat * .9, .25, .004) * vol * 1.3, tb, 1.0, -.1)
            for k in range(2):
                n = tri[(b * 2 + k) % 3]; tt = tb + k * beat / 2
                if tt >= t1: break
                d = beat / 2
                m.add(np.sin(2 * np.pi * hz(n + 12) * ta(d)) * m.env(d, d * .8, .003) * vol * .9 + np.sign(np.sin(2 * np.pi * hz(n) * ta(d))) * m.env(d, d * .5, .003) * vol * .25, tt, 1.0, .15)
            m.add(m.filt(m.noise(.04), 6000, None) * m.env(.04, .012, .001) * vol * .5, tb + beat / 2, 1.0, .3)
            if bpm > 120: m.add(m.sine(70, .12, .04, attack=.002) * vol * 1.6, tb, 1.0, 0)
        t += 4 * beat; bar += 1


H1, W1, F1 = cutT('weigh'), cutT('fight'), cutT('dinner')
c0 = T0('v4') + .3; k0 = T0('c2') + .2; tk = T0('v5') - .2; tA = T0('v9') + .4; ar = T0('v10') - .4; cl0 = T0('c4') + .2; f0 = T0('v7') - .2; g0 = T0('g2') + .3

# ---------------- music ----------------
slide(.5, 200, 1400, 1.0, .16); punch(1.4, .55, 0, 70); chime(1.8, (72, 79, 84, 88), .1)
music(SCN['home'], H1, 116, ['C', 'G', 'Am', 'F'], .05)
music(H1, W1, 112, ['Am', 'F', 'G', 'Am'], .05)
music(W1, T0('m4') - 1.6, 142, ['Am', 'F', 'C', 'G'], .055)
music(F1, T1('k6') - .1, 92, ['C', 'F', 'G', 'C'], .05)
m.room([(0, 0), (6, 0), (6.6, .5), (END, .0)], hum=50)

# ---------------- HOME ----------------
m.click(T0('v1') - .3, .15, .5)
for t in np.arange(T0('a1') + .4, T1('a1') - .2, .6): punch(t, .3, .1, 110); punch(t + .3, .25, .1, 100)
m.steps(T0('a4') + .6, T1('a4') + 1.4, .3, .12, 0, .5)
boing(T1('a4') + 1.3, .12, .4)

# ---------------- WEIGH-IN ----------------
for t in (H1 + .3, T0('c1') + .3, T0('r2') + .3): shutter(t)
snore(H1, T0('g1'), .06, .7)
m.steps(T0('c1') - .8, T0('c1') + .2, .38, .12, .8, .1)
t = T0('r2') + .6
while t < T0('r2') + 2.2: m.click(t, .1, 0); t += max(.03, .13 - (t - T0('r2') - .6) * .05)
boing(T0('r3'), .16, 0)

# ---------------- FIGHT ----------------
crowd([(0, 0), (W1, 0), (W1 + .6, .06), (tk + 2.3, .06), (tk + 2.5, .14), (tk + 4, .06), (tA + 7.6, .06), (tA + 7.8, .16), (tA + 10, .06), (T0('m4') - .4, .06), (T0('m4'), 0), (END, 0)])
snore(W1, T0('g2') - .3, .06, .8)
bellring(T0('r4') - .1); bellring(T0('r4') + .3, .2)
for t in (c0 + 1.2, c0 + 1.9, c0 + 3.4): miss(t, .14, -.1)
punch(c0 + 2.72, .5, -.2, 100)
punch(k0 + .82, .35, .2, 150); clang(k0 + .85, .2)                        # shin vs shin
for i, t in enumerate(np.arange(k0 + 1.0, k0 + 2.6, .3)): boing(t, .05, .3)
m.whoosh(tk + .1, .22, 0); m.thump(tk + .35, .25, 0, 100)
m.whoosh(tk + 1.1, .2, .1); m.whoosh(tk + 1.8, .2, -.1)
m.thump(tk + 2.46, .85, 0, 55); m.add(m.filt(m.noise(.5), 80, 900) * np.hanning(int(SR * .5)) * .35, tk + 2.46, 1.0, 0)
slide(tk + 2.5, 900, 200, .5, .1, 0); chime(tk + 2.7, (80, 76, 72), .06, .12)
for dt, n in zip((0, .62, 1.24, 1.86, 2.7), (60, 62, 64, 67, 72)): m.add(m.sine(hz(n), .3, .2) * .16, T0('r5') + dt, 1.0, 0)
munch(T0('y1') - 1.4, 6, .07)
for t in np.arange(f0 + .3, f0 + 10, .5): m.step(t, .09, m.lerp(.6, -.4, ((t - f0) % 4) / 4))
for t in np.arange(g0, g0 + 4.6, .6): m.thump(t, .14, -.2, 70)
m.thump(g0 + 4.8, .3, 0, 60); snore(g0 + 5.2, F1, .06, .3)
m.add(m.filt(m.noise(.9), 60, 500) * np.hanning(int(SR * .9)) * .22, f0 + 8.0, 1.0, -.6)           # bear growl
munch(f0 + 12.2, 8, .09)
for t in np.arange(cl0 + 1.4, cl0 + 7.4, .5): m.click(t, .12, .3); m.add(m.filt(m.noise(.1), 2500, 7000) * np.hanning(int(SR * .1)) * .06, t, 1.0, .3)   # fence rattle
m.whoosh(cl0 + .1, .2, .3)
fanfare(tA - .7, .08)
m.add(m.filt(m.noise(.6), 400, 4000) * np.hanning(int(SR * .6)) * .2, tA - .1, 1.0, 0)            # wings unfurl
for t in np.arange(tA + .1, tA + 8.3, .45): m.add(m.filt(m.noise(.3), 150, 1200) * np.hanning(int(SR * .3)) ** 2 * .18, t, 1.0, np.sin(t) * .5)   # wing flaps
slide(tA + .2, 300, 900, 1.2, .07, 0); slide(tA + 7.0, 900, 160, .75, .12, 0)
m.thump(tA + 7.75, .9, 0, 52); chime(tA + 7.9, (84, 81, 77, 72), .06, .12)
m.steps(ar - .1, ar + .8, .26, .1, -.2, .3); m.whoosh(ar + .9, .2, 0); m.add(m.sine(900, .3, .1) * .18, ar + 1.3, 1.0, 0); boing(ar + 1.3, .12, 0)
slide(ar + 2.0, 300, 1300, 1.1, .1, .2); m.thump(ar + 3.55, .7, .2, 60); chime(ar + 3.7, (84, 88, 91, 96), .06, .1)
for t in np.arange(T0('m4') - 1.5, T0('m4') + .4, .42): m.thump(t, .3, m.lerp(.9, .4, (t - T0('m4') + 1.5) / 1.9), 70)
m.whoosh(T0('m4') + .1, .2, .3); m.steps(T0('c6') - .5, T0('c6') + .7, .22, .16, 0, -.8)
m.add(m.filt(m.noise(.4), 200, 2500) * np.hanning(int(SR * .4)) * .1, T0('g3') - .6, 1.0, .8)

# ---------------- DINNER ----------------
m.add(m.filt(m.noise(1.4), 2000, 8000) * np.hanning(int(SR * 1.4)) * .08, F1 + .4, 1.0, 0)
munch(F1 + 1.2, 8, .05); m.whoosh(T0('m5') + .2, .12, .5)
m.steps(T1('m6') - .2, T1('m6') + 1.1, .2, .16, 0.0, -.9)
m.add(m.sine(120, .6, .2) * .15, T0('k6') + .1, 1.0, 0)
for t in (T1('k6') + .1, T1('k6') + .5, T1('k6') + .9): m.add(m.sine(4200, .12, .04) * .05, t, 1.0, .3)
fanfare(T1('k6') + .95, .09); chime(T1('k6') + 1.9, (72, 76, 79, 84, 88), .1, .1)
m.room([(0, 0), (F1, 0), (F1 + .4, .4), (T1('k6'), .4), (END, 0)], hum=60)

m.save(sys.argv[1] if len(sys.argv) > 1 else 'ufc3_sfx.wav', fade=(.3, 2.0))
