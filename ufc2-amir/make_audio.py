#!/usr/bin/env python3
"""Music + sound effects for 'Амир и большой бой' (no voices). Event times come from lines.json keys.
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
END = 300.0
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

G0, G1, W0, W1, F0, D0 = cutT('gym'), cutT('weigh'), cutT('weigh'), cutT('fight'), cutT('fight'), cutT('dinner')
G1 = cutT('weigh')

# ---------------- music per scene ----------------
slide(.5, 200, 1400, 1.0, .16); punch(1.4, .55, 0, 70); chime(1.8, (72, 79, 84, 88), .1)
music(SCN['home'], cutT('gym'), 108, ['C', 'G', 'Am', 'F'], .05)
music(cutT('gym'), T0('v2') - .1, 104, ['C', 'Am', 'F', 'G'], .045)
music(T0('v2'), cutT('weigh'), 100, ['Am', 'F', 'C', 'G'], .03)
music(cutT('weigh'), cutT('fight'), 112, ['Am', 'F', 'G', 'Am'], .05)
music(cutT('fight'), T0('m4') - 1.6, 138, ['Am', 'F', 'C', 'G'], .055)
music(T0('r12') + 1.0, cutT('dinner'), 100, ['C', 'G', 'Am', 'F'], .04)
music(cutT('dinner'), T1('k15') - .1, 92, ['C', 'F', 'G', 'C'], .05)
m.room([(0, 0), (6, 0), (6.6, .5), (END, .0)], hum=50)

# ---------------- HOME ----------------
crowd([(0, 0), (T0('v1') - .1, 0), (T0('v1'), .08), (T0('v1') + 1.6, .08), (T0('v1') + 2.2, 0), (END, 0)])
for t in np.arange(T0('a1') + .4, T1('a1') - .2, .6): punch(t, .3, .1, 110); punch(t + .3, .25, .1, 100)
m.click(T0('v1') - .3, .15, .5)
scribble(T1('a4') - 1.3, T1('a5'), .05)
m.whoosh(T0('a6') + .15, .16, 0); slide(T0('a6') + .4, 500, 1800, .8, .06, -.2)
pop(T1('a5') + .05, .08)

# ---------------- GYM ----------------
slide(G0 + .1, 900, 300, T0('k1') - G0 - .3, .08, -.5); m.click(T0('k1') - .2, .18, 0)
snore(G0, T0('k3'), .06, -.6)
m.add(m.filt(m.noise(.9), 60, 500) * np.hanning(int(SR * .9)) * .22, T0('k3') + .1, 1.0, -.4)                # bear wakes: growl
for t in np.arange(T0('k3') + .6, T1('k3') + .8, .75): m.thump(t, .22, -.3, 60)                                   # heavy steps
m.steps(T0('k3') + .3, T1('k3') + 1.2, .36, .12, .8, .1)
boing(T1('k5') + .75, .1, .6); m.thump(T0('a9') + 1.5, .28, .6, 80)
m.add(m.filt(m.noise(.6), 300, 3000) * np.hanning(int(SR * .6)) * (.6 + .4 * m.sine(11, .6, 99)) * .12, T1('k7') + .2, 1.0, 0)  # paw shake
slide(T1('k7') + .85, 700, 120, .7, .16, -.3); m.thump(T1('k7') + 1.5, .3, -.3, 60)
snore(T1('k7') + 1.6, G1, .07, -.4)
m.thump(T0('a12'), .22, 0, 90); m.steps(T0('a12') + .1, T1('a12') + .5, .33, .12, -.3, .4)
m.add(m.filt(m.noise(.4), 100, 800) * np.hanning(int(SR * .4)) * .3, T1('k10') - .1, 1.0, .3)                      # papakha fwump
m.steps(T0('a13') + .2, T1('a13') + 1.2, .5, .1, .5, -.3)
punch(T1('a13') + 1.2, .35, -.2, 80); clang(T1('a13') + 1.25, .1); chime(T1('a13') + 1.35, (84, 88, 91), .06, .12)
m.click(T0('v2') - .1, .2, .5); m.add(m.filt(m.noise(.4), 1000, 6000) * np.hanning(int(SR * .4)) * .05, T0('v2') - .1, 1.0, .4)
chime(T0('v2'), (74, 79, 83, 86), .08, .1)
m.steps(T0('v2') + .4, T0('v2') + 1.8, .35, .1, -.5, .0)
m.add(m.sine(200, .25, .06) * .15, T0('a16') + .6, 1.0, .5)

# ---------------- WEIGH-IN ----------------
for t in (W0 + .3, T0('r2') + .3, T0('r4'), T0('r6') + .2, T0('r7') + .2): shutter(t)
m.steps(T0('r1') + .2, T1('r1') + .6, .38, .12, .8, .1)
t = T0('r2') + .6
while t < T0('r2') + 2.2: m.click(t, .1, 0); t += max(.03, .13 - (t - T0('r2') - .6) * .05)
m.add(m.square(130, .7) * .12 + m.square(137, .7) * .08, T0('r2') + 1.6, 1.0, 0); boing(T0('r2') + 1.5, .18, 0)
m.thump(T0('r2') + 2.8, .1, .3, 120)
snore(W0, T0('g1'), .06, .7)
m.steps(T0('r3') - .6, T0('r3') + .8, .36, .1, -.8, -.2)
d4 = T1('r4') - T0('r4')
for f in (.25, .45, .8): pop(T0('r4') + d4 * f, .12, 0)
for tt in np.arange(T0('r6'), T0('r7') - .3, .5): m.add(m.sine(60, .2, .06) * .25, tt, 1.0, 0)
m.add(m.sine(2500, .06, .02) * .1, T0('r7') - .2, 1.0, 0); boing(T0('r7'), .1, 0)

# ---------------- FIGHT ----------------
crowd([(0, 0), (F0, 0), (F0 + .6, .05), (T0('r9'), .07), (T0('c7'), .04), (T0('c8') - .1, .05), (T0('c8') + .1, .12), (T0('c8') + 1.5, .04), (T0('v8'), .08), (T0('a22') + 1.6, .05), (T0('v9') + .2, .14), (T0('v9') + 2.5, .05), (T0('m4') - .4, .05), (T0('m4'), 0), (T0('r12') + 1, 0), (END, 0)])
bellring(T0('r9') - .5); bellring(T0('r9') - .1, .22)
babble = lambda t0, t1, amp=.035, pan=.5: [m.add(m.square(140 + 120 * rng.random(), .08) * m.env(.08, .05, .004) * amp, tt, 1.0, pan) for tt in np.arange(t0, t1, .13)]
babble(T0('c7'), T1('c7'), .03, .5)
snore(F0, T0('g1'), .06, .8)
for t in np.arange(T0('v7') + .3, T1('v7') + .8, .55): m.thump(t, .22, m.lerp(.9, .3, (t - T0('v7')) / 5), 58)
munch(T0('c8') + .1, 8, .1); m.add(m.filt(m.noise(.2), 200, 1200) * m.env(.2, .06) * .12, T0('c8') + .1, 1.0, .3)
for t in np.arange(T0('r10') + .4, T1('r10') - .4, .9): m.whoosh(t, .14, .3); m.whoosh(t + .45, .1, -.3)
slide(T0('r11') + .1, 300, 1500, .35, .1, 0)                                                                          # shoelace sproing
pop(T0('c10') - .1, .1, .3); munch(T0('a20') + .4, 7, .08)
m.add(m.sine(2400, .08, .03) * .1, T1('r11'), 1.0, 0)
for t in np.arange(T0('k12'), T1('k13'), .5): m.add(m.sine(60, .2, .06) * .12, t, 1.0, 0)
slide(T1('a22') + .8, 1400, 160, .7, .16, .1); m.thump(T1('a22') + 1.7, .45, .0, 75); chime(T1('a22') + 1.75, (84, 88, 91, 96, 91, 88), .06, .12)
fanfare(T0('v9') + .4, .08)
# mom storms in
for t in np.arange(T0('m4') - 1.5, T0('m4') + .4, .42): m.thump(t, .3, m.lerp(.9, .4, (t - T0('m4') + 1.5) / 1.9), 70)
m.whoosh(T0('m4') + .1, .2, .3)
m.steps(T0('c12') - .1, T1('c12') + .5, .2, .16, 0.2, -.8)
m.add(m.filt(m.noise(.4), 200, 2500) * np.hanning(int(SR * .4)) * .1, T0('g2') - .3, 1.0, .8)                         # Gena wakes (gasp)

# ---------------- DINNER ----------------
m.add(m.filt(m.noise(1.4), 2000, 8000) * np.hanning(int(SR * 1.4)) * .08, D0 + .4, 1.0, 0)
for tt in np.sort(rng.uniform(D0 + .6, T0('k15') - .5, 10)): m.add(m.sine(rng.choice([2400, 3100, 1900]), .12, .03) * .06, tt, 1.0, rng.uniform(-.8, .8))
munch(D0 + 1.2, 8, .05); m.whoosh(T0('m5') + .2, .12, .5)
m.steps(T1('m6') - .2, T1('m6') + 1.1, .2, .16, 0.0, -.9)
m.add(m.sine(120, .6, .2) * .15, T0('k15') + .1, 1.0, 0)                                                              # deadpan "dun"
for t in (T1('k15') + .1, T1('k15') + .5, T1('k15') + .9): m.add(m.sine(4200, .12, .04) * .05, t, 1.0, .3)               # crickets
fanfare(T1('k15') + .95, .09); chime(T1('k15') + 1.9, (72, 76, 79, 84, 88), .1, .1)
m.room([(0, 0), (D0, 0), (D0 + .4, .4), (T1('k15'), .4), (END, 0)], hum=60)

m.save(sys.argv[1] if len(sys.argv) > 1 else 'ufc2_sfx.wav', fade=(.3, 2.0))
