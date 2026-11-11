#!/usr/bin/env python3
"""Three speakers for the finale, all synthesized with RHVoice (Russian TTS):
   narr = narrator (artemiy), nina = Nina's old tape recording (elena + tape effect),
   echo = the echo repeating her words (pavel, low, hollow, getting weaker).
Usage: python3 -I voices6.py voice_lines.json out_voice.wav DURATION"""
import sys, json, wave, subprocess, tempfile
import numpy as np

SR = 44100
cfg = json.load(open(sys.argv[1])); out = sys.argv[2]; DUR = float(sys.argv[3])
VOICE = {'narr': ('artemiy', 92, 96), 'nina': ('elena', 100, 88), 'echo': ('pavel', 62, 90)}
rng = np.random.default_rng(6)


def synth(text, who, rate):
    prof, pitch, _ = VOICE[who]
    with tempfile.NamedTemporaryFile(suffix='.wav') as f:
        subprocess.run(['RHVoice-test', '-p', prof, '-r', str(rate), '-t', str(pitch), '-R', str(SR), '-o', f.name],
                       input=text.encode(), check=True, stderr=subprocess.DEVNULL)
        with wave.open(f.name) as w:
            sr = w.getframerate(); x = np.frombuffer(w.readframes(w.getnframes()), '<i2').astype(np.float64) / 32768
            if w.getnchannels() > 1: x = x.reshape(-1, w.getnchannels()).mean(1)
    if sr != SR: x = np.interp(np.arange(int(len(x) * SR / sr)) / SR, np.arange(len(x)) / sr, x)
    return x


def band(x, lo=None, hi=None):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR)
    if lo: X[f < lo] = 0
    if hi: X[f > hi] = 0
    return np.fft.irfft(X, len(x))


def tape(x):
    """Old cassette: narrow band, wow & flutter, hiss, a little saturation."""
    x = band(x, 300, 3300)
    n = len(x); k = np.arange(n)
    warp = k + 120 * np.sin(2 * np.pi * .7 * k / SR) + 25 * np.sin(2 * np.pi * 6 * k / SR)
    x = np.interp(np.clip(warp, 0, n - 1), k, x)
    x = np.tanh(2.2 * x) * .6 + rng.standard_normal(n) * .012
    return x


def hollow(x, gain):
    """The echo: low-passed, doubled with short delays, slow tremolo - and quieter each time."""
    x = band(x, 80, 3200)
    d1, d2 = int(SR * .11), int(SR * .23)
    y = np.concatenate([x, np.zeros(d2)]); y[d1:d1 + len(x)] += .4 * x; y[d2:d2 + len(x)] += .22 * x
    y *= (.85 + .15 * np.sin(2 * np.pi * 8 * np.arange(len(y)) / SR))
    return y * gain


N = int(SR * DUR)
tracks = {'narr': np.zeros(N), 'nina': np.zeros(N), 'echo': np.zeros(N)}
for c in cfg:
    t0, t1, who, text = c['t'], c['end'], c['who'], c['text']
    room = t1 - t0
    rate = VOICE[who][2]
    x = synth(text, who, rate)
    while len(x) / SR > room and rate < 160:
        rate += 6; x = synth(text, who, rate)
    d = len(x) / SR
    print(f'{t0:6.1f}s {who:5s} {d:4.1f}s of {room:4.1f}s rate {rate:3d}' + ('' if d <= room else '  <-- DOES NOT FIT') + f'  {text[:48]}')
    nz = np.where(np.abs(x) > .004)[0]
    if len(nz): x = x[nz[0]:nz[-1] + 1]
    x = x * (.8 / (np.abs(x).max() + 1e-9))
    if who == 'nina': x = tape(x) * 1.1
    if who == 'echo': x = hollow(x, c.get('gain', .8))
    s = int(t0 * SR); seg = x[: N - s]; tracks[who][s:s + len(seg)] += seg

# a touch of room on the narrator
ir_len = int(SR * .4); ir = rng.standard_normal(ir_len) * np.exp(-np.arange(ir_len) / (SR * .1))
n2 = 1 << (N + ir_len).bit_length()
wet = np.fft.irfft(np.fft.rfft(tracks['narr'], n2) * np.fft.rfft(ir, n2), n2)[:N]
wet *= np.abs(tracks['narr']).max() / (np.abs(wet).max() + 1e-9) * .5
tracks['narr'] = tracks['narr'] + .3 * wet
mix = tracks['narr'] * 1.0 + tracks['nina'] * .95 + tracks['echo'] * .9
mix *= .85 / (np.abs(mix).max() + 1e-9)
with wave.open(out, 'wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((mix * 32767).astype('<i2').tobytes())
print('wrote', out)
