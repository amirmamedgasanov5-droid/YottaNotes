/* Tiny cartoon engine: everything is a Web Animations track, so a page can play in real time
   in a browser and can also be rendered frame by frame (window.seek(t)).
   Coordinates: 360 x 640 viewBox, characters stand with their feet at (0,0) local. */
(() => {
  const NS = 'http://www.w3.org/2000/svg';
  const E = window.E = { TOTAL: 120 };
  const svg = E.svg = document.getElementById('svg');
  const el = E.el = (tag, attrs = {}, parent = svg) => { const e = document.createElementNS(NS, tag); for (const k in attrs) e.setAttribute(k, attrs[k]); parent.appendChild(e); return e; };
  const G = E.G = (parent, attrs) => el('g', attrs || {}, parent);
  const raw = E.raw = (parent, markup) => { const g = G(parent); g.innerHTML = markup; return g; };
  const off = t => Math.max(0, Math.min(1, t / E.TOTAL));

  /* ---------- tracks ---------- */
  E.anim = (node, frames) => {
    const k = frames.map(([t, p]) => ({ offset: off(t), ...p }));
    k.sort((a, b) => a.offset - b.offset);
    if (k[0].offset > 0) k.unshift({ ...k[0], offset: 0 });
    if (k[k.length - 1].offset < 1) k.push({ ...k[k.length - 1], offset: 1 });
    return node.animate(k, { duration: E.TOTAL * 1000, fill: 'both', easing: 'linear' });
  };
  E.vis = (node, wins) => {
    const f = [];
    wins.forEach(([a, b]) => {
      if (a > .002) f.push([a - .001, { opacity: 0 }]);
      f.push([a, { opacity: 1 }], [b, { opacity: 1 }], [b + .001, { opacity: 0 }]);
    });
    if (!f.length) f.push([0, { opacity: 0 }]);
    E.anim(node, f);
  };
  E.opacity = (node, pts) => E.anim(node, pts.map(([t, o]) => [t, { opacity: o }]));
  E.fill = (node, pts) => E.anim(node, pts.map(([t, c]) => [t, { fill: c }]));
  E.move = (node, pts) => {
    let x = 0, y = 0, s = 1;
    E.anim(node, pts.map(([t, px, py, ps]) => {
      if (px !== undefined) x = px; if (py !== undefined) y = py; if (ps !== undefined) s = ps;
      return [t, { transform: `translate(${x}px,${y}px) scale(${s})` }];
    }));
  };
  E.pop = (node, t0, t1, origin = 'center') => {
    node.style.transformBox = 'fill-box'; node.style.transformOrigin = origin;
    E.anim(node, [[t0, { opacity: 0, transform: 'scale(.6)' }], [t0 + .28, { opacity: 1, transform: 'scale(1)' }],
      [t1, { opacity: 1, transform: 'scale(1)' }], [t1 + .001, { opacity: 0, transform: 'scale(1)' }]]);
  };
  /* E.shake(node, t0, t1, amp, hz)  or  E.shake(node, [[t0, t1, amp, hz], ...]) */
  E.shake = (node, a, b, amp = 2, hz = 15) => {
    const segs = Array.isArray(a) ? a : [[a, b, amp, hz]];
    const f = [[0, { transform: 'translate(0px,0px)' }]];
    segs.forEach(([t0, t1, am = 2, h = 15]) => {
      const dt = 1 / h / 2; let s = 1;
      f.push([Math.max(0, t0 - .001), { transform: 'translate(0px,0px)' }]);
      for (let t = t0; t < t1; t += dt) { f.push([t, { transform: `translate(${(s * am).toFixed(2)}px,${(-s * am * .5).toFixed(2)}px)` }]); s = -s; }
      f.push([t1, { transform: 'translate(0px,0px)' }]);
    });
    E.anim(node, f);
  };
  E.bob = (node, amp = 3, ms = 320) => node.animate([{ transform: 'translateY(0px)' }, { transform: `translateY(${-amp}px)` }],
    { duration: ms, iterations: Infinity, direction: 'alternate', easing: 'ease-in-out' });
  /* flickering light; if `until` is given the light is dead after that time */
  E.flicker = (node, base = 1, seed = 1, until = null) => {
    const f = []; let r = seed * 9301 + 49297;
    for (let t = 0; t < E.TOTAL; t += .12) {
      r = (r * 9301 + 49297) % 233280; const v = r / 233280;
      f.push([t, { opacity: until !== null && t >= until ? 0 : v > .93 ? base * .2 : v > .85 ? base * .6 : base }]);
    }
    E.anim(node, f);
  };

  /* ---------- layers ---------- */
  E.layers = {};
  ['scenes', 'actors', 'fx', 'ui', 'top'].forEach(n => E.layers[n] = G(svg));

  E.defs = el('defs', {}, svg);
  E.defs.innerHTML = `
    <radialGradient id="vg" cx=".5" cy=".42" r=".75"><stop offset=".3" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity=".93"/></radialGradient>
    <radialGradient id="glow"><stop offset="0" stop-color="#ffe9a8" stop-opacity=".9"/><stop offset="1" stop-color="#ffe9a8" stop-opacity="0"/></radialGradient>
    <radialGradient id="blueglow"><stop offset="0" stop-color="#9fd3ff" stop-opacity=".8"/><stop offset="1" stop-color="#9fd3ff" stop-opacity="0"/></radialGradient>`;

  /* ---------- effects ---------- */
  E.dim = pts => { const r = el('rect', { width: 360, height: 640, fill: '#000' }, E.layers.fx); E.opacity(r, pts); return r; };
  E.vignette = pts => { const r = el('rect', { width: 360, height: 640, fill: 'url(#vg)' }, E.layers.fx); E.opacity(r, pts); return r; };
  E.flash = (t, color = '#fff', dur = .45, peak = .95) => { const r = el('rect', { width: 360, height: 640, fill: color }, E.layers.top); E.opacity(r, [[0, 0], [t - .001, 0], [t, peak], [t + dur, 0]]); };
  E.cut = (t, dur = .3) => { const r = el('rect', { width: 360, height: 640, fill: '#000' }, E.layers.top); E.opacity(r, [[0, 0], [t - dur, 0], [t, 1], [t + dur, 0]]); };
  E.card = (t0, t1, items, bg = '#05070a') => {
    const g = G(E.layers.top); el('rect', { width: 360, height: 640, fill: bg }, g);
    items.forEach(it => { const x = el('text', { x: 180, y: it.y, 'text-anchor': 'middle', 'font-size': it.size, 'font-weight': it.weight || 900, fill: it.color || '#fff' }, g); x.textContent = it.text; });
    E.opacity(g, [[0, 0], [t0 - .001, 0], [t0, 1], [t1 - .6, 1], [t1, 0]]);
  };
  E.tag = (t0, t1, text) => {
    const g = G(E.layers.ui); const w = Math.round(text.length * 8 + 30);
    el('rect', { x: 180 - w / 2, y: 14, width: w, height: 28, rx: 14, fill: 'rgba(0,0,0,.65)' }, g);
    const x = el('text', { x: 180, y: 33, 'text-anchor': 'middle', 'font-size': 13, 'font-weight': 700, fill: '#fff' }, g); x.textContent = text;
    E.opacity(g, [[0, 0], [t0, 0], [t0 + .4, 1], [t1 - .4, 1], [t1, 0]]);
  };
  E.bubble = (t0, t1, o) => {
    const g = G(E.layers.ui); const { x, y, w, lines, tail = 'down', tx = x + w / 2, red } = o; const h = 18 + lines.length * 20;
    el('path', {
      d: `M${x + 10} ${y} h${w - 20} a10 10 0 0 1 10 10 v${h - 20} a10 10 0 0 1 -10 10 h${-(w - 20)} a10 10 0 0 1 -10 -10 v${-(h - 20)} a10 10 0 0 1 10 -10z`,
      fill: red ? '#7a1010' : '#fff', stroke: '#111', 'stroke-width': 2.5 }, g);
    if (tail === 'down') el('polygon', { points: `${tx - 8},${y + h - 1} ${tx + 8},${y + h - 1} ${tx},${y + h + 14}`, fill: red ? '#7a1010' : '#fff', stroke: '#111', 'stroke-width': 2.5 }, g);
    if (tail === 'down') el('rect', { x: tx - 9, y: y + h - 4, width: 18, height: 5, fill: red ? '#7a1010' : '#fff' }, g);
    lines.forEach((ln, i) => { const tt = el('text', { x: x + w / 2, y: y + 24 + i * 20, 'text-anchor': 'middle', 'font-size': 14, 'font-weight': 800, fill: ln.color || (red ? '#fff' : '#111') }, g); tt.textContent = ln.text; });
    E.pop(g, t0, t1, `${tx}px ${y + h}px`);
  };
  /* chat panel with messages: [t, side 'L'|'R', text, slot 0-3, kind 'red'|'warm'|undefined] */
  E.chat = (t0, t1, header, avatar, msgs, status = '● в сети') => {
    const p = G(E.layers.ui);
    el('rect', { x: 14, y: 398, width: 332, height: 228, rx: 22, fill: '#0f141b', stroke: '#2a3340', 'stroke-width': 2 }, p);
    el('circle', { cx: 42, cy: 421, r: 11, fill: avatar }, p);
    const nm = el('text', { x: 62, y: 426, 'font-size': 15, 'font-weight': 800, fill: '#fff' }, p); nm.textContent = header;
    if (status) { const st = el('text', { x: 62 + header.length * 9 + 12, y: 426, 'font-size': 11, fill: '#7fbf7f' }, p); st.textContent = status; }
    E.vis(p, [[t0, t1]]);
    msgs.forEach(([t, side, text, slot, kind]) => {
      const w = Math.min(300, Math.round(text.length * 8.6 + 28)), y = 440 + slot * 44, x = side === 'L' ? 28 : 332 - w;
      const g = G(E.layers.ui);
      el('rect', { x, y, width: w, height: 34, rx: 17, fill: kind === 'red' ? '#7a1010' : kind === 'warm' ? '#8a5a12' : side === 'L' ? '#2b3440' : '#2f6fed' }, g);
      const tx = el('text', { x: x + 14, y: y + 22.5, 'font-size': 15, 'font-weight': kind ? 800 : 600, fill: '#fff' }, g); tx.textContent = text;
      E.pop(g, t, t1, side === 'L' ? '0% 50%' : '100% 50%');
    });
  };
  /* sound-wave rings that expand from a point */
  E.rings = (t0, t1, x, y, color = '#ff5d5d', n = 3) => {
    for (let i = 0; i < n; i++) {
      const c = el('circle', { cx: x, cy: y, r: 20, fill: 'none', stroke: color, 'stroke-width': 3 }, E.layers.ui);
      c.style.transformBox = 'fill-box'; c.style.transformOrigin = 'center';
      const f = [[0, { opacity: 0, transform: 'scale(.4)' }]];
      for (let t = t0 + i * .35; t < t1; t += 1.05) f.push([t - .001, { opacity: 0, transform: 'scale(.4)' }], [t, { opacity: .9, transform: 'scale(.4)' }], [t + 1.0, { opacity: 0, transform: 'scale(3.4)' }], [t + 1.001, { opacity: 0, transform: 'scale(.4)' }]);
      E.anim(c, f);
    }
  };
  E.particles = (t0, t1, from, to, n = 24, color = '#fff6c8') => {
    for (let i = 0; i < n; i++) {
      const r = ((i * 7919) % 100) / 100, r2 = ((i * 104729) % 100) / 100;
      const c = el('circle', { cx: 0, cy: 0, r: 1.6 + r * 2.4, fill: color }, E.layers.ui);
      const s = t0 + r * (t1 - t0) * .6, e = Math.min(t1, s + 2.2 + r2 * 1.2);
      E.anim(c, [[0, { opacity: 0, transform: `translate(${from[0]}px,${from[1]}px)` }], [s, { opacity: 0, transform: `translate(${from[0] + (r - .5) * 30}px,${from[1] + (r2 - .5) * 90}px)` }],
        [s + .2, { opacity: 1, transform: `translate(${from[0] + (r - .5) * 30}px,${from[1] + (r2 - .5) * 90}px)` }],
        [e, { opacity: 0, transform: `translate(${to[0] + (r2 - .5) * 20}px,${to[1] + (r - .5) * 60}px)` }]]);
    }
  };

  /* ---------- actors (feet at 0,0) ---------- */
  const limbs = (g, lines) => {
    const o = G(g), w = G(g);
    lines.forEach(([x1, y1, x2, y2]) => { el('line', { x1, y1, x2, y2, stroke: '#111', 'stroke-width': 10, 'stroke-linecap': 'round' }, o); el('line', { x1, y1, x2, y2, stroke: '#fff', 'stroke-width': 5.5, 'stroke-linecap': 'round' }, w); });
  };
  const FACE = {
    calm: (d, r) => `<circle cx="-6" cy="0" r="2.4" fill="#111" transform="translate(${d * 2} 0)"/><circle cx="6" cy="0" r="2.4" fill="#111" transform="translate(${d * 2} 0)"/><path d="M-4 12 h8" stroke="#111" stroke-width="2.4" stroke-linecap="round"/>`,
    worry: (d, r) => `<circle cx="-6" cy="0" r="2.6" fill="#111" transform="translate(${d * 2} 0)"/><circle cx="6" cy="0" r="2.6" fill="#111" transform="translate(${d * 2} 0)"/><path d="M-10 -7 l7 -3 M10 -7 l-7 -3" stroke="#111" stroke-width="2.2" stroke-linecap="round"/><path d="M-5 13 q2.5 -3 5 0 q2.5 3 5 0" stroke="#111" stroke-width="2.2" fill="none" stroke-linecap="round"/>`,
    scared: (d, r) => `<circle cx="-6" cy="0" r="${r}" fill="#fff" stroke="#111" stroke-width="2"/><circle cx="6" cy="0" r="${r}" fill="#fff" stroke="#111" stroke-width="2"/><circle cx="${-6 + d * 2.6}" cy="1" r="2.1" fill="#111"/><circle cx="${6 + d * 2.6}" cy="1" r="2.1" fill="#111"/><ellipse cx="0" cy="14" rx="3.2" ry="4.4" fill="#111"/>`,
    shock: (d, r) => `<circle cx="-6.5" cy="0" r="${r + 1.6}" fill="#fff" stroke="#111" stroke-width="2"/><circle cx="6.5" cy="0" r="${r + 1.6}" fill="#fff" stroke="#111" stroke-width="2"/><circle cx="${-6.5 + d * 2}" cy="0" r="1.5" fill="#111"/><circle cx="${6.5 + d * 2}" cy="0" r="1.5" fill="#111"/><ellipse cx="0" cy="15" rx="4.6" ry="6.4" fill="#111"/>`,
    smile: (d, r) => `<circle cx="-6" cy="0" r="2.4" fill="#111" transform="translate(${d * 2} 0)"/><circle cx="6" cy="0" r="2.4" fill="#111" transform="translate(${d * 2} 0)"/><path d="M-6 10 q6 8 12 0" stroke="#111" stroke-width="2.4" fill="none" stroke-linecap="round"/>`,
    cry: (d, r) => `<path d="M-10 1 q4 -5 8 0 M2 1 q4 -5 8 0" stroke="#111" stroke-width="2.4" fill="none" stroke-linecap="round"/><ellipse cx="-8" cy="8" rx="1.8" ry="3.4" fill="#5ab4ff"/><ellipse cx="8" cy="8" rx="1.8" ry="3.4" fill="#5ab4ff"/><path d="M-5 15 q5 -4 10 0" stroke="#111" stroke-width="2.4" fill="none" stroke-linecap="round"/>`,
    grim: (d, r) => `<circle cx="-6" cy="1" r="2.2" fill="#111" transform="translate(${d * 2} 0)"/><circle cx="6" cy="1" r="2.2" fill="#111" transform="translate(${d * 2} 0)"/><path d="M-11 -6 l8 3 M11 -6 l-8 3" stroke="#111" stroke-width="2.6" stroke-linecap="round"/><path d="M-5 13 h10" stroke="#111" stroke-width="2.6" stroke-linecap="round"/>`,
  };
  E.face = (a, defs) => defs.forEach(([t0, t1, mood, dir = 0]) => {
    const g = G(a.head, { transform: `translate(0 ${a.faceY})` }); g.innerHTML = FACE[mood](dir, a.eyeR); E.vis(g, [[t0, t1]]);
  });
  E.Lisa = (parent) => {
    const g = G(parent), body = G(g); E.bob(body);
    limbs(body, [[0, -76, 0, -38], [0, -38, -9, 0], [0, -38, 9, 0], [0, -68, -16, -40], [0, -68, 16, -40]]);
    const head = G(body); el('circle', { cx: 0, cy: -96, r: 19, fill: '#fff', stroke: '#111', 'stroke-width': 3.5 }, head);
    el('path', { d: 'M-19 -100 Q0 -126 19 -100 Q0 -112 -19 -100Z', fill: '#3a2414', stroke: '#111', 'stroke-width': 1.5 }, head);
    return { g, body, head, faceY: -95, eyeR: 4.6 };
  };
  E.Mom = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 2.5, 380);
    const inner = G(body, { transform: 'scale(1.06)' });
    limbs(inner, [[-8, -34, -10, 0], [8, -34, 10, 0], [-16, -68, -26, -38], [16, -68, 26, -38]]);
    el('path', { d: 'M-17 -76 L17 -76 L24 -30 L-24 -30 Z', fill: '#c0392b', stroke: '#111', 'stroke-width': 3, 'stroke-linejoin': 'round' }, inner);
    const head = G(inner);
    el('circle', { cx: 0, cy: -98, r: 21, fill: '#fff', stroke: '#111', 'stroke-width': 3.5 }, head);
    el('path', { d: 'M-25 -94 Q-28 -126 0 -127 Q28 -126 25 -94 L29 -56 L19 -58 L17 -92 Q0 -104 -17 -92 L-19 -58 L-29 -56 Z', fill: '#3a2414', stroke: '#111', 'stroke-width': 2, 'stroke-linejoin': 'round' }, head);
    el('ellipse', { cx: 0, cy: -75, rx: 13, ry: 4, fill: '#e8b33c', stroke: '#111', 'stroke-width': 2 }, inner);
    return { g, body, head, faceY: -97, eyeR: 4.2 };
  };
  E.Valya = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 2, 450);
    const inner = G(body, { transform: 'scale(.93)' });
    limbs(inner, [[-6, -30, -7, 0], [6, -30, 7, 0], [-16, -64, -22, -38], [16, -64, 22, -38]]);
    el('path', { d: 'M-16 -48 L16 -48 L24 -8 L-24 -8 Z', fill: '#6a4c7a', stroke: '#111', 'stroke-width': 3, 'stroke-linejoin': 'round' }, inner);
    el('path', { d: 'M-24 -74 Q0 -84 24 -74 L20 -44 L-20 -44 Z', fill: '#4f7a5a', stroke: '#111', 'stroke-width': 3, 'stroke-linejoin': 'round' }, inner);
    const head = G(inner);
    el('circle', { cx: 0, cy: -96, r: 19, fill: '#fff', stroke: '#111', 'stroke-width': 3.5 }, head);
    el('path', { d: 'M-19 -98 Q0 -122 19 -98 Q0 -108 -19 -98Z', fill: '#b9bcc4', stroke: '#111', 'stroke-width': 1.5 }, head);
    el('circle', { cx: 0, cy: -121, r: 8, fill: '#b9bcc4', stroke: '#111', 'stroke-width': 2 }, head);
    const gl = G(head, { transform: 'translate(0 -95)' }); gl.innerHTML = '<circle cx="-6" cy="0" r="5.6" fill="none" stroke="#111" stroke-width="1.8"/><circle cx="6" cy="0" r="5.6" fill="none" stroke="#111" stroke-width="1.8"/><path d="M-0.4 0 h0.8" stroke="#111" stroke-width="1.8"/>';
    return { g, body, head, faceY: -95, eyeR: 3.8 };
  };
  E.Thing = (parent) => {
    const g = G(parent), body = G(g), s = G(body, { transform: 'translate(-58 -336)' });
    s.innerHTML = `<g fill="#04060a" stroke="#161b24" stroke-width="1.5" stroke-linejoin="round"><ellipse cx="58" cy="190" rx="13" ry="17"/>
      <path d="M36 214 Q58 202 80 214 L86 300 L77 300 L76 262 L71 336 L63 336 L58 290 L53 336 L45 336 L40 262 L39 300 L30 300 Z"/></g>
      <ellipse cx="52" cy="187" rx="3.2" ry="4.6" fill="#fff"/><ellipse cx="64" cy="187" rx="3.2" ry="4.6" fill="#fff"/>`;
    return { g, body };
  };
  E.at = (a, pts) => E.move(a.g, pts);
  /* static prop that pops in/out: a wrapper carries the position, the inner group carries the pop */
  E.placed = (parent, markup, x, y, s, t0, t1) => {
    const w = G(parent); E.move(w, [[0, x, y, s], [E.TOTAL, x, y, s]]);
    const inner = raw(w, markup); E.pop(inner, t0, t1); return inner;
  };

  /* ---------- props ---------- */
  E.boombox = (parent) => {
    const g = G(parent);
    g.innerHTML = `<rect x="-36" y="-30" width="72" height="34" rx="5" fill="#3b3f46" stroke="#111" stroke-width="2.5"/>
      <circle cx="-21" cy="-12" r="11" fill="#1d2025" stroke="#777" stroke-width="2"/><circle cx="21" cy="-12" r="11" fill="#1d2025" stroke="#777" stroke-width="2"/>
      <rect x="-10" y="-24" width="20" height="12" rx="2" fill="#a8b0a0" stroke="#111" stroke-width="1.5"/><circle cx="-4" cy="-18" r="2.4" fill="#444"/><circle cx="4" cy="-18" r="2.4" fill="#444"/>
      <path d="M-26 -30 q26 -14 52 0" stroke="#111" stroke-width="3" fill="none"/>`;
    return g;
  };
  E.cassette = (parent) => { const g = G(parent); g.innerHTML = '<rect x="-12" y="-8" width="24" height="16" rx="2" fill="#e8dcc0" stroke="#111" stroke-width="1.8"/><circle cx="-5" cy="0" r="3" fill="#222"/><circle cx="5" cy="0" r="3" fill="#222"/>'; return g; };

  /* ---------- scenes ---------- */
  E.scene = (t0, t1, build) => { const g = G(E.layers.scenes); const r = build(g); E.vis(g, [[t0, t1]]); return Object.assign({ g }, r); };

  const doorSwing = (door, t0, t1, hold = .2) => { door.style.transformBox = 'view-box'; E.anim(door, [[0, { transform: 'scaleX(1)' }], [t0, { transform: 'scaleX(1)' }], [t0 + .4, { transform: `scaleX(${hold})` }], [t1, { transform: `scaleX(${hold})` }], [t1 + .12, { transform: 'scaleX(1)' }]]); };
  E.doorSwing = doorSwing;

  /* tone: night | day | dawn */
  E.apt = (g, tone = 'night') => {
    const C = {
      night: { wall: '#27303f', floor: '#3a2d28', base: '#1d1612', sky: '#0a1226', cur: '#7a4b5c', sofa: '#5b3f78', sofa2: '#6c4b8c' },
      day:   { wall: '#d7c7a6', floor: '#9a7a58', base: '#6e5538', sky: '#bfe6ff', cur: '#c9788a', sofa: '#7a5c99', sofa2: '#8a6cab' },
      dawn:  { wall: '#6b5a66', floor: '#4d3a36', base: '#2b201d', sky: '#f4a384', cur: '#a8586a', sofa: '#5b3f78', sofa2: '#6c4b8c' },
    }[tone];
    const lamp = tone === 'night';
    raw(g, `<rect width="360" height="400" fill="${C.wall}"/><rect y="335" width="360" height="65" fill="${C.floor}"/><rect y="331" width="360" height="5" fill="${C.base}"/>
      <rect x="112" y="58" width="96" height="104" fill="${C.sky}" stroke="${C.base}" stroke-width="5"/>
      ${tone === 'night' ? '<circle cx="178" cy="92" r="13" fill="#f3f1d8"/><circle cx="184" cy="88" r="11" fill="#0a1226"/>' : tone === 'day' ? '<circle cx="176" cy="96" r="16" fill="#fff4a8"/>' : '<circle cx="160" cy="146" r="26" fill="#ffd27a"/>'}
      <line x1="160" y1="58" x2="160" y2="162" stroke="${C.base}" stroke-width="3"/><line x1="112" y1="110" x2="208" y2="110" stroke="${C.base}" stroke-width="3"/>
      <path d="M104 52 Q116 110 108 170 L128 170 Q122 110 128 52Z" fill="${C.cur}"/><path d="M216 52 Q204 110 212 170 L192 170 Q198 110 192 52Z" fill="${C.cur}"/>
      ${tone !== 'night' ? '<polygon points="114,162 206,162 300,335 130,335" fill="#fff6c0" opacity=".16"/>' : ''}
      <circle cx="248" cy="96" r="17" fill="#e8e4d4" stroke="${C.base}" stroke-width="3"/><line x1="248" y1="96" x2="248" y2="84" stroke="#222" stroke-width="2.5"/><line x1="248" y1="96" x2="257" y2="99" stroke="#222" stroke-width="2.5"/>
      <rect x="14" y="136" width="88" height="200" fill="${C.base}"/><rect x="18" y="140" width="80" height="196" fill="#04060a"/>
      <rect x="30" y="302" width="56" height="5" fill="#2a2a2e"/><line x1="34" y1="307" x2="34" y2="336" stroke="#2a2a2e" stroke-width="3"/><line x1="82" y1="307" x2="82" y2="336" stroke="#2a2a2e" stroke-width="3"/>`);
    const hall = el('rect', { x: 284, y: 126, width: 68, height: 206, fill: '#fffbe8', opacity: 0 }, g);
    const door = G(g);
    raw(door, `<rect x="284" y="126" width="68" height="206" fill="#5a3a22" stroke="#1a0f08" stroke-width="4"/><rect x="294" y="140" width="48" height="74" fill="none" stroke="#3a2414" stroke-width="3"/><rect x="294" y="226" width="48" height="90" fill="none" stroke="#3a2414" stroke-width="3"/><circle cx="297" cy="236" r="4.5" fill="#d4a93c"/><circle cx="318" cy="172" r="4" fill="#0b0b0b"/><rect x="286" y="327" width="64" height="4" fill="#ffe9a0" opacity=".9"/>`);
    door.style.transformOrigin = '352px 230px';
    if (lamp) { const l = G(g); raw(l, `<ellipse cx="236" cy="250" rx="130" ry="150" fill="url(#glow)" opacity=".55"/><line x1="236" y1="190" x2="236" y2="336" stroke="#1a1410" stroke-width="4"/><polygon points="214,192 258,192 248,166 224,166" fill="#d9a441" stroke="#1a1410" stroke-width="2"/><ellipse cx="236" cy="338" rx="16" ry="4" fill="#1a1410"/>`); E.flicker(l, 1, 3); }
    raw(g, `<rect x="80" y="236" width="140" height="62" rx="14" fill="${C.sofa}" stroke="#1d1328" stroke-width="3"/><rect x="80" y="290" width="140" height="46" rx="10" fill="${C.sofa2}" stroke="#1d1328" stroke-width="3"/>`);
    const kphone = G(g); raw(kphone, '<ellipse cx="58" cy="290" rx="52" ry="40" fill="url(#blueglow)"/><rect x="51.5" y="295" width="14" height="5" rx="1" fill="#bfe3ff"/>'); kphone.style.opacity = 0;
    return { door, hall, kphone, doorOpen: (a, b) => { E.doorSwing(door, a, b); E.vis(hall, [[a + .2, b]]); } };
  };

  E.stairs = (g, label = '5', sky = '#10182e') => {
    raw(g, `<rect width="360" height="400" fill="#9a9fa3"/><rect y="150" width="360" height="190" fill="#56725f"/><rect y="146" width="360" height="5" fill="#d8d8d0"/>
      <rect width="360" height="44" fill="#3a3e43"/><rect y="340" width="360" height="60" fill="#5a5e63"/><rect y="338" width="360" height="4" fill="#3a3e43"/>
      <g fill="#6c7176" stroke="#2a2d31" stroke-width="2"><rect x="12" y="190" width="60" height="70"/><line x1="12" y1="213" x2="72" y2="213"/><line x1="12" y1="236" x2="72" y2="236"/><line x1="42" y1="190" x2="42" y2="260"/></g>
      <rect x="296" y="88" width="54" height="100" fill="${sky}" stroke="#2a2d31" stroke-width="4" class="sky"/><line x1="323" y1="88" x2="323" y2="188" stroke="#2a2d31" stroke-width="3"/><rect x="292" y="196" width="62" height="12" fill="#c8c8c0" stroke="#2a2d31" stroke-width="2"/>
      <rect x="150" y="300" width="60" height="38" fill="none"/>
      <text x="180" y="212" text-anchor="middle" font-size="46" font-weight="900" fill="#d8d8d0" opacity=".28">${label}</text>`);
    const behind47 = el('rect', { x: 92, y: 150, width: 62, height: 188, fill: '#1a1612', opacity: 0 }, g);
    const behind44 = el('rect', { x: 208, y: 150, width: 62, height: 188, fill: '#ffd98a', opacity: 0 }, g);
    const d47 = G(g), d44 = G(g);
    raw(d47, '<rect x="92" y="150" width="62" height="188" fill="#6a4630" stroke="#1a0f08" stroke-width="3"/><rect x="100" y="160" width="46" height="70" fill="none" stroke="#3a2414" stroke-width="2.5"/><circle cx="102" cy="250" r="4" fill="#d4a93c"/><rect x="113" y="186" width="24" height="13" fill="#e8dcc0" stroke="#111"/><text x="125" y="196" text-anchor="middle" font-size="10" font-weight="900" fill="#111">47</text>');
    raw(d44, '<rect x="208" y="150" width="62" height="188" fill="#7a5238" stroke="#1a0f08" stroke-width="3"/><rect x="216" y="160" width="46" height="70" fill="none" stroke="#3a2414" stroke-width="2.5"/><circle cx="260" cy="250" r="4" fill="#d4a93c"/><rect x="229" y="186" width="24" height="13" fill="#e8dcc0" stroke="#111"/><text x="241" y="196" text-anchor="middle" font-size="10" font-weight="900" fill="#111">44</text>');
    d47.style.transformOrigin = '92px 230px'; d44.style.transformOrigin = '270px 230px';
    const bulb = G(g); raw(bulb, '<ellipse cx="180" cy="60" rx="110" ry="130" fill="url(#glow)" opacity=".6"/><line x1="180" y1="44" x2="180" y2="56" stroke="#222" stroke-width="2"/><circle cx="180" cy="62" r="8" fill="#fff3b0" stroke="#222" stroke-width="2"/>');
    return { bulb, d47, d44, behind47, behind44, open44: (a, b) => { E.doorSwing(d44, a, b, .15); E.vis(behind44, [[a + .2, b]]); }, open47: (a, b) => { E.doorSwing(d47, a, b, .15); E.vis(behind47, [[a + .2, b]]); } };
  };

  E.flat = (g) => {
    let stripes = ''; for (let x = 0; x < 360; x += 24) stripes += `<rect x="${x}" y="0" width="12" height="336" fill="#bf9a6d" opacity=".55"/>`;
    raw(g, `<rect width="360" height="400" fill="#cfae82"/>${stripes}<rect y="335" width="360" height="65" fill="#6b4a30"/><rect y="331" width="360" height="5" fill="#3d2a1a"/>
      <rect x="22" y="92" width="110" height="150" fill="#8a2f2f" stroke="#3d2a1a" stroke-width="3"/><g stroke="#e8c27a" stroke-width="2" fill="none"><rect x="32" y="102" width="90" height="130"/><path d="M32 102 L122 232 M122 102 L32 232"/><circle cx="77" cy="167" r="22"/></g>
      <rect x="148" y="70" width="70" height="86" fill="#0a1226" stroke="#3d2a1a" stroke-width="5"/><circle cx="196" cy="98" r="9" fill="#f3f1d8"/><circle cx="200" cy="95" r="8" fill="#0a1226"/><path d="M142 64 Q152 110 146 160 L162 160 Q156 110 160 64Z" fill="#a8586a"/><path d="M224 64 Q214 110 220 160 L204 160 Q210 110 206 64Z" fill="#a8586a"/>
      <rect x="236" y="110" width="104" height="8" fill="#3d2a1a"/><g stroke="#3d2a1a" stroke-width="2"><rect x="244" y="74" width="24" height="34" fill="#e8c05a"/><rect x="274" y="80" width="20" height="28" fill="#d9a441"/><rect x="300" y="72" width="26" height="36" fill="#e8c05a"/></g>
      <g fill="#6a4c7a" stroke="#3d2a1a" stroke-width="3"><rect x="262" y="236" width="68" height="64" rx="14"/></g><rect x="268" y="296" width="12" height="40" fill="#3d2a1a"/><rect x="312" y="296" width="12" height="40" fill="#3d2a1a"/>
      <ellipse cx="150" cy="300" rx="52" ry="8" fill="#3d2a1a"/><rect x="138" y="262" width="24" height="38" fill="#5a3a22"/><ellipse cx="150" cy="262" rx="48" ry="8" fill="#8a5a38" stroke="#3d2a1a" stroke-width="2"/>`);
    const cups = G(g); raw(cups, '<rect x="118" y="244" width="16" height="16" rx="3" fill="#f4efe0" stroke="#3d2a1a" stroke-width="2"/><rect x="164" y="244" width="16" height="16" rx="3" fill="#f4efe0" stroke="#3d2a1a" stroke-width="2"/><path d="M138 262 q12 -22 24 0 z" fill="#d9d3c0" stroke="#3d2a1a" stroke-width="2"/><rect x="144" y="236" width="12" height="8" fill="#d9d3c0" stroke="#3d2a1a" stroke-width="2"/>');
    const candle = G(g); raw(candle, '<ellipse cx="286" cy="86" rx="40" ry="34" fill="url(#glow)"/><rect x="283" y="96" width="6" height="14" fill="#f4efe0" stroke="#3d2a1a"/><ellipse cx="286" cy="90" rx="3" ry="6" fill="#ffb347"/>'); E.flicker(candle, 1, 5);
    const behind = el('rect', { x: 284, y: 126, width: 68, height: 206, fill: '#2a2a2e', opacity: 0 }, g);
    const door = G(g); raw(door, '<rect x="284" y="126" width="68" height="206" fill="#7a5238" stroke="#1a0f08" stroke-width="4"/><rect x="294" y="140" width="48" height="74" fill="none" stroke="#3a2414" stroke-width="3"/><rect x="294" y="226" width="48" height="90" fill="none" stroke="#3a2414" stroke-width="3"/><circle cx="297" cy="236" r="4.5" fill="#d4a93c"/><rect x="290" y="216" width="22" height="5" fill="#bbb"/>');
    door.style.transformOrigin = '352px 230px';
    return { cups, candle, door, behind, doorOpen: (a, b) => { E.doorSwing(door, a, b); E.vis(behind, [[a + .2, b]]); } };
  };

  E.basement = (g) => {
    raw(g, `<rect width="360" height="400" fill="#2b2e33"/><rect y="340" width="360" height="60" fill="#3a3d42"/><rect y="338" width="360" height="4" fill="#202226"/>
      <g stroke="#555a61" stroke-width="10" stroke-linecap="round"><line x1="0" y1="40" x2="360" y2="40"/><line x1="0" y1="66" x2="360" y2="66"/></g><g stroke="#2a2d31" stroke-width="3"><line x1="60" y1="30" x2="60" y2="76"/><line x1="200" y1="30" x2="200" y2="76"/><line x1="310" y1="30" x2="310" y2="76"/></g>
      <g fill="#6b4a30" stroke="#1a120a" stroke-width="3"><rect x="14" y="276" width="70" height="62"/><rect x="26" y="222" width="56" height="54"/><rect x="92" y="298" width="46" height="40"/></g>
      <g fill="#7a5a3a" stroke="#1a120a" stroke-width="3"><rect x="270" y="270" width="76" height="68"/><rect x="284" y="214" width="58" height="56"/></g>
      <g fill="none" stroke="#6a7077" stroke-width="3"><rect x="150" y="150" width="104" height="188"/><line x1="150" y1="200" x2="254" y2="200"/><line x1="150" y1="250" x2="254" y2="250"/><line x1="176" y1="150" x2="176" y2="338"/><line x1="202" y1="150" x2="202" y2="338"/><line x1="228" y1="150" x2="228" y2="338"/></g>
      <rect x="168" y="290" width="68" height="48" fill="#8a6a44" stroke="#1a120a" stroke-width="3"/><rect x="180" y="300" width="44" height="22" fill="#e8dcc0" stroke="#111" stroke-width="1.5"/><text x="202" y="315" text-anchor="middle" font-size="11" font-weight="900" fill="#111">НИНА · 47</text>
      <ellipse cx="110" cy="356" rx="46" ry="7" fill="#1f3340" opacity=".8"/><ellipse cx="300" cy="372" rx="36" ry="6" fill="#1f3340" opacity=".8"/>`);
    const bulb = G(g); raw(bulb, '<ellipse cx="180" cy="110" rx="150" ry="160" fill="url(#glow)" opacity=".5"/><line x1="180" y1="70" x2="180" y2="96" stroke="#222" stroke-width="2"/><circle cx="180" cy="102" r="8" fill="#fff3b0" stroke="#222" stroke-width="2"/>');
    return { bulb };
  };
})();
