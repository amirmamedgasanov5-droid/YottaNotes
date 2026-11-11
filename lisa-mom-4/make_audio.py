#!/usr/bin/env python3
"""Sound design for episode 4 (no voice). Usage: python3 -I make_audio.py out.wav"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'shared'))
from sfx import Mix

m = Mix(120.0, seed=41)

# --- beds ---
m.room([(0, 0), (3, 0), (4.6, 1), (24.2, 1), (24.6, .7), (105, .7), (105.3, .05), (109.5, .05), (110.5, .6), (117, .5), (120, 0)], hum=100)
m.room([(0, 0), (24.4, 0), (25, .8), (104.9, .8), (105.2, 0), (120, 0)], hum=50)
m.wind([(0, 0), (4.6, .03), (15, .05), (24, .02), (76, .0), (85, .05), (105, .09), (105.3, 0), (120, 0)])
m.drone([(0, 0), (62, 0), (76, .08), (93, .15), (105, .2), (105.2, 0), (109.5, 0), (112, .1), (117, .12), (117.1, .03), (120, 0)])
m.tick(25.0, 76.0)
m.heartbeat(85.0, 105.0, start_gap=1.0, min_gap=.5, slope=.03)

# --- stairwell: phone, steps, bell, neighbour opens ---
m.vibrate(5.55, .4, .14); m.ding(5.6, 740)
m.vibrate(7.55, .4, .14); m.ding(7.6, 622, .26)
m.steps(11.0, 13.8, .46, .12, 0.0, .4)
m.doorbell(15.0, pan=.5, f1=587, f2=440)
m.creak(17.5, .8, 230, .07, .5)
m.steps(22.0, 24.2, .42, .12, .3, .9)

# --- the flat ---
m.steps(24.6, 27.4, .42, .12, .9, -.1)
m.add(m.filt(m.noise(2.6), 1200, 4200) * (.6 + .4 * m.sine(7, 2.6, 99)) * m.env(2.6, 2.0) * .12, 28.0, 1.0, -.1)  # pouring tea
m.click(30.4, .12, 0)

# --- flashback: Nina's messages ---
for t, base in ((43.6, 523), (46.2, 523), (48.8, 523), (54.5, 466)):
    m.typing(t - .6, 6, .08, 0); m.ding(t, base, .13, 0)
m.rings(63.0, 69.8)
m.breath(64.0, 2.4, .1); m.breath(67.0, 2.4, .12)

# --- Lisa answered the echo ---
m.ding(70.4, 622, .26, 0); m.whoosh(71.6); m.whoosh(72.8); m.whoosh(74.0)

# --- the echo knocks ---
for tk, a in ((76.0, .3), (76.9, .32), (77.8, .34)): m.thump(tk, a, .8, 88)
m.thump(82.0, .25, .8, 88); m.thump(84.5, .25, .8, 88)
m.breath(86.0, 2.4, .1, .8)
m.pounding(93.5, 105.0, gap0=1.0, gap_min=.3, slope=.08, a0=.5, a1=.95)
m.rattle(98.0, 105.0)
m.vibrate(98.3, .45, .2, pan=-.3); m.ding(98.4, 622, .22, -.3)
m.typing(100.2, 4, .1, -.3)

# --- silence, then the cassette ---
m.click(110.4, .15, 0); m.sting(110.4, .25, 0)
m.whine(112, 116.5, peak=.012)
m.sting(116.6, .45, -.3)

m.save(sys.argv[1] if len(sys.argv) > 1 else 'audio4.wav')
