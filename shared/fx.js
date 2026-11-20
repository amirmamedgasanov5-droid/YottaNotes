/* Comic effects (hit bursts, dust, speed lines, sweat, big words) and a camera. Needs engine.js. */
(() => {
  const { el, G, raw } = E;
  const FX = window.FX = { parent: null };
  const at = (g, x, y) => { g.setAttribute('transform', `translate(${x} ${y})`); return g; };
  const burst = (n, r1, r2) => { let p = []; for (let i = 0; i < n * 2; i++) { const a = i / (n * 2) * Math.PI * 2, r = i % 2 ? r1 : r2; p.push(`${(Math.cos(a) * r).toFixed(1)},${(Math.sin(a) * r).toFixed(1)}`); } return p.join(' '); };
  const tri = (t, f) => f.map(([dt, props]) => [t + dt, props]);
  /* hit burst with a word: FX.impact(t, x, y, size, 'POW!') */
  FX.impact = (t, x, y, size = 1, text = 'POW!', col = '#ffd23f') => {
    const o = at(G(FX.parent), x, y), g = G(o);
    el('polygon', { points: burst(9, 38, 74), fill: '#fff', stroke: '#1b1414', 'stroke-width': 5, 'stroke-linejoin': 'round' }, g);
    el('polygon', { points: burst(9, 28, 56), fill: col }, g);
    if (text) { const tx = el('text', { x: 0, y: 11, 'text-anchor': 'middle', 'font-size': 30, 'font-weight': 900, fill: '#c0182b', stroke: '#fff', 'stroke-width': 4, 'paint-order': 'stroke', 'font-family': 'DejaVu Sans, sans-serif', transform: 'rotate(-8)' }, g); tx.textContent = text; }
    E.anim(g, [[0, { opacity: 0, transform: 'scale(.2) rotate(-12deg)' }], [t - .001, { opacity: 0, transform: 'scale(.2) rotate(-12deg)' }], [t, { opacity: 1, transform: 'scale(.5) rotate(-6deg)' }], [t + .07, { opacity: 1, transform: `scale(${1.25 * size}) rotate(4deg)` }], [t + .26, { opacity: 1, transform: `scale(${size}) rotate(0deg)` }], [t + .44, { opacity: 0, transform: `scale(${size * 1.1}) rotate(0deg)` }]]);
  };
  FX.word = (t0, t1, x, y, text, size = 64, col = '#ffd23f', rot = -6) => {
    const o = at(G(FX.parent), x, y), g = G(o); const tx = el('text', { x: 0, y: 0, 'text-anchor': 'middle', 'font-size': size, 'font-weight': 900, fill: col, stroke: '#1b1414', 'stroke-width': 9, 'paint-order': 'stroke', 'font-family': 'DejaVu Sans, sans-serif', transform: `rotate(${rot})` }, g); tx.textContent = text;
    E.anim(g, [[0, { opacity: 0, transform: 'scale(.3)' }], [t0 - .001, { opacity: 0, transform: 'scale(.3)' }], [t0, { opacity: 1, transform: 'scale(.3)' }], [t0 + .16, { opacity: 1, transform: 'scale(1.18)' }], [t0 + .3, { opacity: 1, transform: 'scale(1)' }], [t1 - .2, { opacity: 1, transform: 'scale(1)' }], [t1, { opacity: 0, transform: 'scale(1.1)' }]]);
  };
  FX.dust = (t, x, y, size = 1) => {
    for (let i = 0; i < 6; i++) {
      const dx = (i - 2.5) * 34 * size, o = at(G(FX.parent), x + dx, y - 6), c = el('circle', { cx: 0, cy: 0, r: (22 + (i % 3) * 8) * size, fill: '#efe6d2', stroke: '#b9ae98', 'stroke-width': 3 }, o);
      c.style.transformOrigin = '0px 0px';
      E.anim(c, [[0, { opacity: 0, transform: 'translate(0px,0px) scale(.2)' }], [t - .001, { opacity: 0, transform: 'translate(0px,0px) scale(.2)' }], [t, { opacity: .95, transform: 'translate(0px,0px) scale(.4)' }], [t + .5, { opacity: .6, transform: `translate(${dx * .5}px,${-26 - (i % 2) * 18}px) scale(1.2)` }], [t + .95, { opacity: 0, transform: `translate(${dx * .8}px,${-40 - (i % 2) * 20}px) scale(1.7)` }]]);
    }
  };
  FX.speed = (t0, t1, x, y, dir = 1, len = 300) => {
    const o = at(G(FX.parent), x, y);
    for (let i = 0; i < 7; i++) { const yy = (i - 3) * 30 + (i % 2) * 8; el('line', { x1: -dir * (20 + (i % 3) * 18), y1: yy, x2: -dir * (len - (i % 4) * 30), y2: yy, stroke: '#fff', 'stroke-width': 4 - (i % 3), 'stroke-linecap': 'round', opacity: .8 }, o); }
    const f = []; for (let t = t0; t < t1; t += .08) f.push([t, { opacity: ((t / .08) | 0) % 2 ? .9 : .35 }]); f.unshift([0, { opacity: 0 }], [t0 - .001, { opacity: 0 }]); f.push([t1, { opacity: 0 }]); E.anim(o, f);
  };
  FX.sweat = (t, x, y, dir = 1) => {
    for (let i = 0; i < 3; i++) {
      const o = at(G(FX.parent), x, y), d = el('path', { d: 'M0 -12 Q9 4 0 10 Q-9 4 0 -12 Z', fill: '#7fd0ff', stroke: '#1b1414', 'stroke-width': 2.4 }, o);
      E.anim(d, [[0, { opacity: 0, transform: 'translate(0px,0px)' }], [t - .001, { opacity: 0, transform: 'translate(0px,0px)' }], [t, { opacity: 1, transform: 'translate(0px,0px)' }], [t + .5, { opacity: 1, transform: `translate(${dir * (30 + i * 18)}px,${-30 + i * 22}px)` }], [t + .8, { opacity: 0, transform: `translate(${dir * (44 + i * 22)}px,${10 + i * 26}px)` }]]);
    }
  };
  FX.ring = (t, x, y, size = 1) => { const o = at(G(FX.parent), x, y), c = el('circle', { cx: 0, cy: 0, r: 40, fill: 'none', stroke: '#fff', 'stroke-width': 6 }, o); c.style.transformOrigin = '0px 0px'; E.anim(c, [[0, { opacity: 0, transform: 'scale(.3)' }], [t - .001, { opacity: 0, transform: 'scale(.3)' }], [t, { opacity: .95, transform: 'scale(.4)' }], [t + .4, { opacity: 0, transform: `scale(${2.6 * size})` }]]); };

  /* camera: everything that is passed in is moved/zoomed together.  CAM.track([[t, cx, cy, zoom], ...]) and CAM.shake(...) */
  FX.camera = (layers) => {
    const outer = G(E.svg), inner = G(outer); layers.forEach(l => inner.appendChild(l));
    outer.parentNode.insertBefore(outer, E.layers.fx);
    const cam = { outer, inner };
    cam.track = (pts) => E.anim(inner, pts.map(([t, cx, cy, z]) => [t, { transform: `translate(${E.W / 2}px,${E.H / 2}px) scale(${z}) translate(${-cx}px,${-cy}px)` }]));
    cam.shake = (segs) => E.shake(outer, segs);
    return cam;
  };
})();
