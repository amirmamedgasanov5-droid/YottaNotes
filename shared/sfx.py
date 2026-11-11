"""Small numpy sound-design toolkit shared by the episodes (no voice, no external samples)."""
import wave
import numpy as np

SR = 44100


class Mix:
    def __init__(self, dur, seed=1):
        self.dur = dur
        self.N = int(SR * dur)
        self.L = np.zeros(self.N)
        self.R = np.zeros(self.N)
        self.t = np.arange(self.N) / SR
        self.rng = np.random.default_rng(seed)

    # ----- basics -----
    def ta(self, d): return np.arange(int(SR * d)) / SR
    def noise(self, d): return self.rng.standard_normal(int(SR * d))

    @staticmethod
    def filt(x, lo=None, hi=None):
        X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR)
        if lo: X[f < lo] = 0
        if hi: X[f > hi] = 0
        return np.fft.irfft(X, len(x))

    def env(self, d, decay, attack=.002):
        e = np.exp(-self.ta(d) / decay); a = int(SR * attack)
        if a: e[:a] *= np.linspace(0, 1, a)
        return e

    def sine(self, f, d, decay, amp=1.0, attack=.002): return np.sin(2 * np.pi * f * self.ta(d)) * self.env(d, decay, attack) * amp
    def square(self, f, d): return np.sign(np.sin(2 * np.pi * f * self.ta(d)))

    def add(self, sig, at, amp=1.0, pan=0.0):
        i = int(at * SR)
        if i >= self.N or i < 0: return
        s = sig[: self.N - i]
        self.L[i:i + len(s)] += s * amp * np.cos((pan + 1) * np.pi / 4)
        self.R[i:i + len(s)] += s * amp * np.sin((pan + 1) * np.pi / 4)

    def bed(self, sig, env_pts, pan_wobble=False):
        """Adds a long signal scaled by an interpolated envelope [(t, gain), ...]."""
        e = np.interp(self.t, [p[0] for p in env_pts], [p[1] for p in env_pts])
        if pan_wobble:
            w = np.sin(2 * np.pi * .13 * self.t) * .5 + .5
            self.L += sig * e * (.6 + .4 * w); self.R += sig * e * (1 - .4 * w)
        else:
            self.L += sig * e; self.R += sig * e

    @staticmethod
    def lerp(a, b, k): return a + (b - a) * k

    # ----- sounds -----
    def room(self, env_pts, hum=50, seed_noise=True):
        n = self.filt(self.rng.standard_normal(self.N), None, 260) * .02
        n += np.sin(2 * np.pi * hum * self.t) * .012 + np.sin(2 * np.pi * hum * 2 * self.t) * .004
        self.bed(n, env_pts)

    def wind(self, env_pts):
        self.bed(self.filt(self.rng.standard_normal(self.N), 180, 900), env_pts, pan_wobble=True)

    def drone(self, env_pts, f1=55, f2=58.2):
        d = self.filt(np.sign(np.sin(2 * np.pi * f1 * self.t)) + np.sign(np.sin(2 * np.pi * f2 * self.t)), None, 170)
        self.bed(d, env_pts)

    def tick(self, t0, t1, step=1.0, amp=.04, pan=.15):
        for k, ts in enumerate(np.arange(t0, t1, step)):
            self.add(self.filt(self.noise(.012), 2200, None) * self.env(.012, .003, .0004), ts, amp * (1 if k % 2 == 0 else .7), pan)

    def ding(self, at, base=988, amp=.22, pan=.2):
        self.add(self.sine(base, .5, .12) + .4 * self.sine(base * 2, .5, .08), at, amp, pan)
        self.add(self.sine(base * 1.335, .8, .22) + .35 * self.sine(base * 2.67, .8, .12), at + .13, amp, pan)

    def vibrate(self, at, d=.45, amp=.12, pan=.2, f=175, cutoff=700):
        tt = self.ta(d); am = .55 + .45 * np.sign(np.sin(2 * np.pi * 28 * tt))
        s = self.filt(self.square(f, d) * am, None, cutoff) * np.minimum(1, tt / .01) * np.minimum(1, (d - tt) / .03)
        self.add(s, at, amp, pan)
        self.add(self.filt(self.noise(d), 2500, None) * am * .35 * np.minimum(1, tt / .01), at, amp * .5, pan)

    def whoosh(self, at, amp=.1, pan=.3):
        d = .22
        self.add(self.filt(self.noise(d), 600, 5000) * np.hanning(int(SR * d)), at, amp, pan)
        self.add(self.sine(620, .08, .03), at + .17, amp * .8, pan)

    def click(self, at, amp=.3, pan=.8):
        self.add(self.filt(self.noise(.03), 1500, None) * self.env(.03, .004, .0005) + .7 * self.sine(1900, .03, .006), at, amp, pan)

    def typing(self, at, n=8, amp=.1, pan=0.0, gap=.07):
        for k in range(n): self.click(at + k * (gap + .03 * self.rng.random()), amp * (.6 + .4 * self.rng.random()), pan)

    def thump(self, at, amp=.5, pan=.7, f=80):
        self.add(self.sine(f, .3, .07, attack=.004) + .5 * self.sine(f * 1.9, .3, .03), at, amp, pan)
        self.add(self.filt(self.noise(.08), None, 450) * self.env(.08, .02), at, amp * .6, pan)

    def step(self, at, amp=.12, pan=0.0):
        self.add(self.filt(self.noise(.09), 120, 900) * self.env(.09, .025, .004), at, amp, pan)

    def steps(self, t0, t1, gap=.42, amp=.12, pan0=0.0, pan1=None):
        pan1 = pan0 if pan1 is None else pan1
        ts = np.arange(t0, t1, gap)
        for k, t in enumerate(ts): self.step(t, amp, self.lerp(pan0, pan1, k / max(1, len(ts) - 1)))

    def bell(self, at, f, amp=.32, pan=.8):
        d = 1.6
        self.add((self.sine(f, d, .5) + .5 * self.sine(f * 2.01, d, .3) + .25 * self.sine(f * 3.02, d, .15)) * amp, at, 1.0, pan)

    def doorbell(self, at, pan=.8, f1=659, f2=523): self.bell(at, f1, pan=pan); self.bell(at + .55, f2, pan=pan)

    def creak(self, at, d=.8, base=240, amp=.07, pan=.85, falling=True):
        tt = self.ta(d)
        fr = base + 90 * np.sin(2 * np.pi * 6 * tt) + (-110 if falling else 110) * tt
        s = np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.hanning(len(tt)) * amp
        self.add(self.filt(s, 120, 2600), at, 1.0, pan)

    def slam(self, at, pan=.8, amp=.9):
        d = 1.6
        self.add(self.sine(50, d, .5, attack=.003) * amp, at, 1.0, pan)
        self.add(self.filt(self.noise(.5), None, 900) * self.env(.5, .09, .001) * amp, at, 1.0, pan)
        self.add(self.filt(self.noise(d), 200, 3000) * self.env(d, .35, .004) * .08, at + .05, 1.0, pan * .6)

    def heartbeat(self, t0, t1, start_gap=1.0, min_gap=.52, slope=.04, a0=.24, a1=.6):
        hb, j = t0, 0
        while hb < t1:
            a = min(a1, a0 + j * .02)
            self.add(self.sine(55, .22, .06, attack=.004), hb, a, 0)
            self.add(self.sine(50, .2, .05, attack=.004), hb + .22, a * .75, 0)
            hb += max(min_gap, start_gap - j * slope); j += 1

    def pounding(self, t0, t1, gap0=1.0, gap_min=.28, slope=.08, a0=.5, a1=.9, pan=.8, f=76):
        tk, i = t0, 0
        while tk < t1:
            self.thump(tk, min(a1, a0 + i * .03), pan, f + (i % 3) * 4)
            tk += max(gap_min, gap0 - slope * i); i += 1

    def rattle(self, t0, t1, pan=.8, amp=.1):
        for tr in np.arange(t0, t1, .07):
            if self.rng.random() < .7: self.add(self.filt(self.noise(.03), 900, 6000) * self.env(.03, .008), tr, amp * (.4 + self.rng.random()), pan)

    def breath(self, at, d=1.9, amp=.12, pan=-.7):
        self.add(self.filt(self.noise(d), 350, 1600) * np.hanning(int(SR * d)) ** 2 * amp, at, 1.0, pan)

    def sting(self, at, amp=.55, pan=-.5):
        self.add(self.sine(58, 2.4, .6, attack=.005), at, amp, pan)
        cl = sum(self.filt(np.sign(np.sin(2 * np.pi * f * self.ta(2.4))), None, 1100) for f in (220, 233.1, 311.1))
        self.add(cl * self.env(2.4, .9) * .1, at, 1.0, pan * .6)

    def whine(self, t0, t1, f0=1400, slope=150, peak=.02):
        fw = f0 + (self.t - t0) * slope
        self.L += np.sin(2 * np.pi * np.cumsum(fw) / SR) * np.interp(self.t, [t0 - .5, t1, t1 + .05, self.dur], [0, peak, 0, 0])
        self.R += np.sin(2 * np.pi * np.cumsum(fw) / SR) * np.interp(self.t, [t0 - .5, t1, t1 + .05, self.dur], [0, peak, 0, 0])

    def rings(self, t0, t1, gap=1.05, amp=.07, pan=-.5):
        for ts in np.arange(t0, t1, gap):
            self.add(self.filt(self.noise(.9), 600, 2500) * np.hanning(int(SR * .9)) * amp, ts, 1.0, pan)
            self.add(self.sine(82, 1.0, .35, attack=.01) * .35, ts, amp * 3, pan)

    def save(self, path, fade=(.3, 2.5)):
        f = np.interp(self.t, [0, fade[0], self.dur - fade[1], self.dur], [0, 1, 1, 0])
        L, R = self.L * f, self.R * f
        peak = max(np.abs(L).max(), np.abs(R).max(), 1e-9); g = .9 / peak
        L = np.tanh(L * g * 1.1) / np.tanh(1.1) * .95
        R = np.tanh(R * g * 1.1) / np.tanh(1.1) * .95
        pcm = (np.stack([L, R], 1) * 32767).astype('<i2')
        with wave.open(path, 'wb') as w:
            w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        print('wrote', path, f'{self.dur:.0f}s')
