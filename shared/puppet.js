/* Puppet rig: a detailed 2D cartoon person made of jointed parts (thighs, shins, torso, arms, head)
   animated with Web Animations (seekable). Needs engine.js. Feet at (0,0), y grows downwards.
   Rest-pose geometry is drawn in one common coordinate system; every joint is a <g> with a transform-origin
   at the joint, so rotating a parent carries its children. */
(() => {
  const { el, G, raw } = E;
  const P = window.P = {};
  const OUT = '#1b1414', SW = 3.4;

  const hex = (h) => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16));
  const toHex = (a) => '#' + a.map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join('');
  const dk = (h, f) => toHex(hex(h).map(v => v * (1 - f)));
  const lt = (h, f) => toHex(hex(h).map(v => v + (255 - v) * f));
  P.dk = dk; P.lt = lt;

  const joint = (parent, px, py) => { const g = G(parent); g.style.transformOrigin = `${px}px ${py}px`; return g; };
  const cap = (g, x, y, w, len, fill, shade = true) => {
    el('rect', { x: x - w / 2, y, width: w, height: len, rx: w / 2, fill, stroke: OUT, 'stroke-width': SW }, g);
    if (shade) el('rect', { x: x + w * .12, y: y + 2, width: w * .3, height: len - 4, rx: w * .15, fill: '#000', opacity: .1 }, g);
  };
  const path = (g, d, fill, extra = {}) => el('path', Object.assign({ d, fill, stroke: OUT, 'stroke-width': SW, 'stroke-linejoin': 'round' }, extra), g);

  /* ---------------- build ---------------- */
  P.make = (parent, spec) => {
    const S = Object.assign({
      skin: '#f2c9a0', hair: 'short', hairColor: '#4a2f1a', top: 'tee', topColor: '#2a56c6', bottom: 'shorts', bottomColor: '#17171c', stripe: null,
      shoes: '#e8e8e8', gloves: null, hat: null, beard: null, glasses: false, headset: false, belly: 0, scale: 1, headScale: 1, tie: null, apron: false, legColor: null,
      emblem: null, brow: '#1b1414', cheeks: false,
    }, spec);
    const root = G(parent), world = G(root, { transform: `scale(${S.scale})` }); // world: body scale
    const rig = G(world); const breath = G(rig); E.bob(breath, 1.8, 900 + Math.random() * 300);
    const skin = S.skin, skinD = dk(skin, .12);
    const legColor = S.legColor || (S.bottom === 'pants' || S.bottom === 'leggings' ? S.bottomColor : skin);
    const parts = {};

    // ---- legs (hip y=-138) ----
    const mkLeg = (side) => {
      const x = side * 15; const thigh = joint(breath, x, -138); const shin = joint(thigh, x, -72);
      cap(thigh, x, -142, 30, 76, legColor);
      cap(shin, x, -76, 26, 66, legColor);
      if (S.bottom === 'shorts' || S.bottom === 'leggings+shorts') { path(thigh, `M${x - 16} -142 L${x + 16} -142 L${x + 18} -100 L${x - 18} -100 Z`, S.bottomColor); if (S.stripe) el('rect', { x: x + (side > 0 ? 12 : -15), y: -142, width: 3.4, height: 42, fill: S.stripe }, thigh); }
      if (S.shoes) { path(shin, `M${x - 13} -16 L${x + 13} -16 L${x + 14} -6 Q${x + 28} -4 ${x + 26} 2 L${x - 15} 2 Q${x - 17} -6 ${x - 13} -16 Z`, S.shoes); el('rect', { x: x - 15, y: -1, width: 41, height: 3, fill: '#222' }, shin); }
      else path(shin, `M${x - 12} -16 L${x + 12} -16 L${x + 13} -6 Q${x + 26} -4 ${x + 24} 2 L${x - 14} 2 Q${x - 16} -6 ${x - 12} -16 Z`, skin);
      return { thigh, shin };
    };
    const legL = mkLeg(-1), legR = mkLeg(1);
    parts.thighL = legL.thigh; parts.shinL = legL.shin; parts.thighR = legR.thigh; parts.shinR = legR.shin;

    // ---- torso (pivot at hips) ----
    const torso = joint(breath, 0, -138); parts.torso = torso;
    const bw = S.belly;
    const tpath = `M-40 -208 Q-40 -218 -28 -218 L28 -218 Q40 -218 40 -208 L${33 + bw * .7} -176 Q${37 + bw} -156 ${34 + bw * .6} -140 Q0 -128 -${34 + bw * .6} -140 Q-${37 + bw} -156 -${33 + bw * .7} -176 Z`;
    const topFill = S.top === 'bare' ? skin : S.topColor;
    if (S.top === 'dress') path(torso, 'M-38 -210 Q-40 -218 -28 -218 L28 -218 Q40 -218 38 -210 L52 -92 Q0 -80 -52 -92 Z', S.topColor);
    else path(torso, tpath, topFill);
    el('path', { d: S.top === 'dress' ? 'M14 -216 L38 -210 L52 -92 Q36 -86 20 -86 Z' : `M12 -216 L28 -216 Q40 -216 40 -208 L33 -140 L14 -134 Z`, fill: '#000', opacity: .1 }, torso);
    if (S.top === 'bare') {
      el('path', { d: 'M-24 -196 Q-12 -184 0 -190 Q12 -184 24 -196', fill: 'none', stroke: OUT, 'stroke-width': 2.4, 'stroke-linecap': 'round' }, torso);
      el('path', { d: 'M0 -190 L0 -150 M-14 -172 Q0 -168 14 -172 M-13 -158 Q0 -154 13 -158', fill: 'none', stroke: OUT, 'stroke-width': 2, opacity: .55, 'stroke-linecap': 'round' }, torso);
      el('ellipse', { cx: 0, cy: -198, rx: 22, ry: 9, fill: '#6a4a30', opacity: .6 }, torso);
    }
    if (S.top === 'suit') {
      path(torso, 'M-12 -218 L0 -196 L12 -218 L8 -150 L-8 -150 Z', '#f6f6f6');
      path(torso, 'M-12 -218 L-30 -200 L-20 -150 L-9 -150 L-2 -196 Z', dk(S.topColor, .06)); path(torso, 'M12 -218 L30 -200 L20 -150 L9 -150 L2 -196 Z', dk(S.topColor, .06));
      if (S.tie) { path(torso, 'M-4 -210 L4 -210 L6 -192 L0 -186 L-6 -192 Z', S.tie); path(torso, 'M-5 -190 L5 -190 L9 -150 L0 -142 L-9 -150 Z', S.tie); }
      if (S.pinstripe) for (let i = -30; i <= 30; i += 10) el('line', { x1: i, y1: -214, x2: i * 1.05, y2: -142, stroke: '#fff', 'stroke-width': 1.2, opacity: .28 }, torso);
    }
    if (S.top === 'ref') for (let i = -30; i <= 30; i += 12) el('rect', { x: i - 3, y: -216, width: 6, height: 76, fill: '#111', opacity: .95 }, torso);
    if (S.top === 'ref') path(torso, 'M-12 -216 L12 -216 L8 -208 L-8 -208 Z', '#d62828');
    if (S.top === 'uniform') { for (let i = 0; i < 3; i++) el('circle', { cx: 0, cy: -190 + i * 18, r: 3.2, fill: '#d9b44a', stroke: OUT, 'stroke-width': 1.5 }, torso); path(torso, 'M-40 -208 L-12 -218 L-4 -206 L-24 -196 Z', dk(S.topColor, .15)); }
    if (S.top === 'jacket') { path(torso, 'M-10 -218 L0 -198 L10 -218 L6 -150 L-6 -150 Z', '#e9eef7'); path(torso, 'M-10 -218 L-28 -200 L-18 -150 L-8 -150 L-1 -196 Z', dk(S.topColor, .08)); path(torso, 'M10 -218 L28 -200 L18 -150 L8 -150 L1 -196 Z', dk(S.topColor, .08)); }
    if (S.apron) { path(torso, 'M-26 -190 L26 -190 L32 -92 L-32 -92 Z', '#f4efe0'); el('rect', { x: -14, y: -150, width: 28, height: 22, rx: 4, fill: 'none', stroke: OUT, 'stroke-width': 2.2 }, torso); el('path', { d: 'M-18 -190 L-14 -218 M18 -190 L14 -218', stroke: '#f4efe0', 'stroke-width': 5 }, torso); }
    if (S.emblem === 'ball') { el('circle', { cx: 0, cy: -176, r: 15, fill: '#f4f6fb', stroke: OUT, 'stroke-width': 2.6 }, torso); el('path', { d: 'M-9 -184 Q0 -176 9 -184 M-9 -168 Q0 -176 9 -168 M0 -191 L0 -161', fill: 'none', stroke: '#2a56c6', 'stroke-width': 2.2 }, torso); }
    if (S.belt) { el('rect', { x: -33, y: -150, width: 66, height: 12, rx: 3, fill: S.belt, stroke: OUT, 'stroke-width': 2.4 }, torso); el('circle', { cx: 0, cy: -144, r: 7, fill: lt(S.belt, .4), stroke: OUT, 'stroke-width': 2 }, torso); }
    if (S.bottom === 'shorts' || S.bottom === 'leggings+shorts') path(torso, 'M-34 -150 Q0 -142 34 -150 L36 -124 Q0 -116 -36 -124 Z', S.bottomColor);
    if (S.bottom === 'shorts' && S.towel) { path(torso, 'M-36 -150 L36 -150 L40 -92 L-40 -92 Z', '#f6f6f6'); el('rect', { x: 10, y: -150, width: 14, height: 58, fill: '#111' }, torso); }
    if (S.bottom === 'pants') path(torso, 'M-34 -150 Q0 -142 34 -150 L36 -126 Q0 -116 -36 -126 Z', S.bottomColor);
    parts.torsoArt = torso;

    // neck
    const neck = el('rect', { x: -10, y: -230, width: 20, height: 18, rx: 5, fill: skin, stroke: OUT, 'stroke-width': SW }, torso);

    // ---- arms (shoulder y=-206) ----
    const sleeveCol = S.top === 'suit' || S.top === 'jacket' || S.top === 'ref' || S.top === 'uniform' ? S.topColor : null;
    const mkArm = (side) => {
      const sx = side * 40; const up = joint(breath, sx, -206); const fore = joint(up, sx, -152);
      cap(up, sx, -212, 22, 62, sleeveCol || (S.top === 'tee' || S.top === 'dress' || S.top === 'ref' ? skin : skin));
      if (S.top === 'tee' || S.top === 'dress' || S.top === 'uniform') { path(up, `M${sx - 12} -214 L${sx + 12} -214 L${sx + 13} -186 L${sx - 13} -186 Z`, S.topColor); }
      if (S.top === 'ref') { path(up, `M${sx - 12} -214 L${sx + 12} -214 L${sx + 13} -190 L${sx - 13} -190 Z`, '#f2f2f2'); }
      cap(fore, sx, -156, 19, 58, sleeveCol && S.top !== 'ref' && S.top !== 'uniform' ? sleeveCol : skin);
      if (S.gloves) {
        path(fore, `M${sx - 15} -106 Q${sx - 20} -88 ${sx - 12} -78 Q${sx} -72 ${sx + 12} -78 Q${sx + 20} -88 ${sx + 15} -106 Z`, S.gloves);
        el('rect', { x: sx - 14, y: -110, width: 28, height: 8, rx: 3, fill: S.wrist || '#2a56c6', stroke: OUT, 'stroke-width': 2.2 }, fore);
        el('path', { d: `M${sx - 10} -96 Q${sx} -92 ${sx + 10} -96`, fill: 'none', stroke: OUT, 'stroke-width': 1.8, opacity: .6 }, fore);
      } else el('circle', { cx: sx, cy: -96, r: 12, fill: skin, stroke: OUT, 'stroke-width': SW }, fore);
      return { up, fore, sx };
    };
    const armL = mkArm(-1), armR = mkArm(1);
    parts.armL = armL.up; parts.foreL = armL.fore; parts.armR = armR.up; parts.foreR = armR.fore;
    parts.handR = G(armR.fore, { transform: 'translate(40 -90)' }); parts.handL = G(armL.fore, { transform: 'translate(-40 -90)' });

    // ---- head ----
    const head = joint(breath, 0, -228); parts.head = head;
    const hs = G(head, { transform: `translate(0 ${-268 * (1 - S.headScale) * 0}) scale(${S.headScale})` }); hs.style.transformOrigin = '0px -228px';
    const H = G(hs);
    el('circle', { cx: -42, cy: -266, r: 9, fill: skin, stroke: OUT, 'stroke-width': SW }, H); el('circle', { cx: 42, cy: -266, r: 9, fill: skin, stroke: OUT, 'stroke-width': SW }, H);
    // hair behind (long / bun)
    if (S.hair === 'mom') { el('circle', { cx: 0, cy: -318, r: 17, fill: S.hairColor, stroke: OUT, 'stroke-width': SW }, H); }
    el('ellipse', { cx: 0, cy: -268, rx: 43, ry: 47, fill: skin, stroke: OUT, 'stroke-width': SW }, H);
    el('path', { d: 'M18 -312 Q46 -296 44 -262 Q40 -230 20 -224 Q36 -250 32 -290 Z', fill: '#000', opacity: .08 }, H);
    if (S.cheeks) { el('ellipse', { cx: -26, cy: -252, rx: 8, ry: 5, fill: '#e8736a', opacity: .35 }, H); el('ellipse', { cx: 26, cy: -252, rx: 8, ry: 5, fill: '#e8736a', opacity: .35 }, H); }
    // beard (under the face features)
    if (S.beard) {
      if (S.beardStyle === 'short') path(H, 'M-38 -262 Q-40 -226 0 -222 Q40 -226 38 -262 Q30 -244 0 -244 Q-30 -244 -38 -262 Z', S.beard, { 'stroke-width': 2.4 });
      else path(H, 'M-42 -270 Q-46 -214 0 -208 Q46 -214 42 -270 Q34 -240 0 -240 Q-34 -240 -42 -270 Z', S.beard, { 'stroke-width': 2.6 });
    }
    // face
    const F = G(H, { transform: 'translate(0 -268)' });
    const mkEye = (sx) => {
      const eye = G(F); eye.style.transformOrigin = `${sx}px -4px`;
      el('ellipse', { cx: sx, cy: -4, rx: 11.5, ry: 13, fill: '#fff', stroke: OUT, 'stroke-width': 2.6 }, eye);
      const pupil = G(eye); el('circle', { cx: sx, cy: -3, r: 5.4, fill: '#1b1414' }, pupil); el('circle', { cx: sx + 1.8, cy: -5.4, r: 1.8, fill: '#fff' }, pupil);
      return { eye, pupil };
    };
    const eL = mkEye(-16), eR = mkEye(16);
    const lids = G(F); el('ellipse', { cx: -16, cy: -4, rx: 13, ry: 14.5, fill: skin, stroke: OUT, 'stroke-width': 2.6 }, lids); el('ellipse', { cx: 16, cy: -4, rx: 13, ry: 14.5, fill: skin, stroke: OUT, 'stroke-width': 2.6 }, lids); el('path', { d: 'M-27 -2 Q-16 6 -5 -2 M5 -2 Q16 6 27 -2', fill: 'none', stroke: OUT, 'stroke-width': 3, 'stroke-linecap': 'round' }, lids); lids.style.opacity = 0;
    const mkBrow = (sx, sgn) => { const b = G(F); b.style.transformOrigin = `${sx}px -26px`; el('path', { d: `M${sx - 12} -24 Q${sx} ${-30 - 1} ${sx + 12} -24`.replace(/-24 Q/, '-24 Q'), fill: 'none', stroke: S.brow, 'stroke-width': 5.4, 'stroke-linecap': 'round' }, b); return b; };
    const bL = mkBrow(-16, -1), bR = mkBrow(16, 1);
    el('path', { d: 'M-3 6 Q0 14 6 10', fill: 'none', stroke: dk(skin, .35), 'stroke-width': 2.8, 'stroke-linecap': 'round' }, F);
    // mouths: closed variants + open (lip-synced)
    const lips = S.beard ? '#d98a7a' : '#7a2a2a';
    const mClosed = { flat: el('path', { d: 'M-9 27 Q0 29 9 27', fill: 'none', stroke: lips, 'stroke-width': 3.2, 'stroke-linecap': 'round' }, F),
      smile: el('path', { d: 'M-12 24 Q0 36 12 24', fill: 'none', stroke: lips, 'stroke-width': 3.2, 'stroke-linecap': 'round' }, F),
      frown: el('path', { d: 'M-10 32 Q0 22 10 32', fill: 'none', stroke: lips, 'stroke-width': 3.2, 'stroke-linecap': 'round' }, F),
      smirk: el('path', { d: 'M-10 29 Q2 30 12 22', fill: 'none', stroke: lips, 'stroke-width': 3.2, 'stroke-linecap': 'round' }, F) };
    Object.values(mClosed).forEach(m => m.style.opacity = 0); mClosed.flat.style.opacity = 1;
    const mouth = G(F); mouth.style.transformOrigin = '0px 24px'; mouth.style.opacity = 0;
    el('ellipse', { cx: 0, cy: 28, rx: 13, ry: 11, fill: '#5a1414', stroke: OUT, 'stroke-width': 2.6 }, mouth);
    el('ellipse', { cx: 0, cy: 34, rx: 8, ry: 4.5, fill: '#e8736a' }, mouth);
    el('path', { d: 'M-11 21 Q0 25 11 21 L11 24 Q0 28 -11 24 Z', fill: '#fff' }, mouth);
    // hair / hats on top
    const hairTop = G(H);
    const HC = S.hairColor;
    if (S.hair === 'short') path(hairTop, 'M-44 -266 Q-52 -322 0 -320 Q52 -322 44 -266 Q40 -292 20 -300 Q0 -288 -20 -300 Q-40 -292 -44 -266 Z', HC, { 'stroke-width': 2.8 });
    if (S.hair === 'spiky') path(hairTop, 'M-44 -266 Q-54 -300 -32 -312 L-24 -328 L-12 -312 L0 -332 L12 -312 L24 -328 L32 -312 Q54 -300 44 -266 Q40 -292 20 -298 Q0 -286 -20 -298 Q-40 -292 -44 -266 Z', HC, { 'stroke-width': 2.8 });
    if (S.hair === 'swept') path(hairTop, 'M-44 -262 Q-56 -322 8 -326 Q58 -322 46 -262 Q44 -296 20 -298 Q-10 -306 -30 -290 Q-40 -280 -44 -262 Z', HC, { 'stroke-width': 2.8 });
    if (S.hair === 'tonsure') { path(hairTop, 'M-44 -262 Q-50 -290 -34 -300 Q-30 -280 -36 -262 Z', HC, { 'stroke-width': 2.4 }); path(hairTop, 'M44 -262 Q50 -290 34 -300 Q30 -280 36 -262 Z', HC, { 'stroke-width': 2.4 }); }
    if (S.hair === 'mom') { path(hairTop, 'M-44 -262 Q-52 -318 0 -316 Q52 -318 44 -262 Q40 -290 18 -296 Q0 -284 -18 -296 Q-40 -290 -44 -262 Z', HC, { 'stroke-width': 2.8 }); for (let i = -2; i <= 2; i++) el('rect', { x: i * 16 - 6, y: -312, width: 12, height: 16, rx: 6, fill: '#ec8aa8', stroke: OUT, 'stroke-width': 2.2, transform: `rotate(${i * 6} ${i * 16} -300)` }, hairTop); }
    if (S.hat === 'papakha') {
      const hat = G(hairTop, { transform: 'translate(0 -12)' });
      [[-34, -296, 22], [-18, -312, 24], [0, -318, 26], [18, -312, 24], [34, -296, 22], [-40, -280, 17], [40, -280, 17], [-22, -290, 22], [0, -296, 24], [22, -290, 22]].forEach(([cx, cy, r]) => el('circle', { cx, cy, r, fill: S.hatColor || '#f6f6f6', stroke: OUT, 'stroke-width': 3 }, hat));
      el('path', { d: 'M-42 -280 Q0 -296 42 -280 L44 -272 Q0 -288 -44 -272 Z', fill: S.hatColor || '#f6f6f6' }, hat);
      for (let i = 0; i < 9; i++) el('path', { d: `M${-36 + i * 9} ${-300 + (i % 3) * 6} q3 5 0 10`, fill: 'none', stroke: '#c9ccd4', 'stroke-width': 2, 'stroke-linecap': 'round' }, hat);
    }
    if (S.hat === 'cap') { path(hairTop, 'M-44 -292 Q-48 -340 0 -340 Q48 -340 44 -292 Z', S.hatColor || '#243a5e'); path(hairTop, 'M-8 -293 Q40 -300 68 -288 Q52 -280 -8 -282 Z', dk(S.hatColor || '#243a5e', .2)); el('circle', { cx: 0, cy: -316, r: 6, fill: '#d9b44a', stroke: OUT, 'stroke-width': 1.6 }, hairTop); }
    if (S.glasses) { el('circle', { cx: -16, cy: -272, r: 17, fill: '#cfeaff', opacity: .35, stroke: OUT, 'stroke-width': 3.4 }, H); el('circle', { cx: 16, cy: -272, r: 17, fill: '#cfeaff', opacity: .35, stroke: OUT, 'stroke-width': 3.4 }, H); el('path', { d: 'M-0 -272 L0 -272', stroke: OUT, 'stroke-width': 3 }, H); el('line', { x1: -33, y1: -274, x2: -44, y2: -268, stroke: OUT, 'stroke-width': 3 }, H); el('line', { x1: 33, y1: -274, x2: 44, y2: -268, stroke: OUT, 'stroke-width': 3 }, H); }
    if (S.headset) { el('path', { d: 'M-46 -266 Q-50 -328 0 -326 Q50 -328 46 -266', fill: 'none', stroke: '#2a2a30', 'stroke-width': 6, 'stroke-linecap': 'round' }, H); el('rect', { x: -56, y: -280, width: 16, height: 26, rx: 7, fill: '#2a2a30', stroke: OUT, 'stroke-width': 2 }, H); el('path', { d: 'M-48 -256 Q-46 -232 -20 -224', fill: 'none', stroke: '#2a2a30', 'stroke-width': 4, 'stroke-linecap': 'round' }, H); el('circle', { cx: -18, cy: -224, r: 7, fill: '#3a3a42', stroke: OUT, 'stroke-width': 2 }, H); }
    if (S.mustache) path(F, 'M-18 14 Q-8 8 0 14 Q8 8 18 14 Q10 22 0 18 Q-10 22 -18 14 Z', S.mustache, { 'stroke-width': 2 });

    const obj = { root, world, rig, breath, parts, mouth, mClosed, eyes: { L: eL, R: eR }, brows: { L: bL, R: bR }, spec: S, tracks: {}, rootSegs: [], mouthKeys: [], flat: null };

    /* ---------- animation API ---------- */
    // joint rotation keys: obj.rot('armR', [[t, deg], ...])  — everything is merged into one animation per joint at flush()
    obj.rot = (name, pts) => { (obj.tracks[name] = obj.tracks[name] || []).push(...pts.map(p => [p[0], p[1]])); return obj; };
    // root position: obj.go([[t, x, y, flip, rotDeg, scale], ...])
    obj.go = (pts) => { obj.rootSegs.push(pts); return obj; };
    obj.show = (wins) => { E.vis(root, wins); return obj; };
    obj.blink = (seed = 1) => {
      let r = seed * 7919, t = 1.2; const f = [[0, { transform: 'scaleY(1)' }]];
      while (t < E.TOTAL - 1) { f.push([t, { transform: 'scaleY(1)' }], [t + .06, { transform: 'scaleY(.08)' }], [t + .15, { transform: 'scaleY(1)' }]); r = (r * 9301 + 49297) % 233280; t += 2.4 + (r / 233280) * 3.2; }
      [eL, eR].forEach(e => E.anim(e.eye, f)); return obj;
    };
    obj.sleep = (wins) => { E.vis(lids, wins); return obj; };
    obj.look = (pts) => { [eL, eR].forEach(e => E.anim(e.pupil, pts.map(([t, dx, dy]) => [t, { transform: `translate(${dx}px,${dy}px)` }]))); return obj; };
    const MOODS = { neutral: [0, 0, 'flat', 1], happy: [-6, -3, 'smile', 1], angry: [17, 3, 'frown', .92], sad: [-15, 0, 'frown', 1], shock: [-4, -10, 'flat', 1.2], smug: [-10, -4, 'smirk', .95], worry: [-12, -2, 'flat', 1.05] };
    obj.mood = (wins) => {
      const bf = { L: [], R: [] }, vis = { flat: [], smile: [], frown: [], smirk: [] };
      wins.forEach(([a, b, name]) => {
        const [rot, dy, shape] = MOODS[name];
        bf.L.push([a, { transform: `translate(0px,${dy}px) rotate(${rot}deg)` }], [b, { transform: `translate(0px,${dy}px) rotate(${rot}deg)` }]);
        bf.R.push([a, { transform: `translate(0px,${dy}px) rotate(${-rot}deg)` }], [b, { transform: `translate(0px,${dy}px) rotate(${-rot}deg)` }]);
        vis[shape].push([a, b]);
      });
      const prep = (arr, rest) => { const o = []; arr.forEach((p, i) => { if (i % 2 === 0) { o.push([Math.max(0, p[0] - .12), i === 0 ? rest : arr[i - 1][1]]); } o.push(p); }); return o; };
      E.anim(bL, prep(bf.L, { transform: 'rotate(0deg)' })); E.anim(bR, prep(bf.R, { transform: 'rotate(0deg)' }));
      Object.keys(vis).forEach(k => { E.vis(mClosed[k], vis[k].length ? vis[k] : [[-1, -.5]]); });
      return obj;
    };
    // lip-sync: obj.talk([[t0, t1], ...])  and obj.gasp([[t0,t1,open], ...]) (forced open mouth)
    obj.talk = (wins, seed = 3) => {
      let r = seed * 6007;
      wins.forEach(([a, b]) => {
        obj.mouthKeys.push([a - .02, 0, 0]);
        for (let t = a; t < b - .06; t += .115) { r = (r * 9301 + 49297) % 233280; const v = [.25, .85, .5, 1, .2, .75, .6][Math.floor(r / 233280 * 7)]; obj.mouthKeys.push([t, v, 1]); }
        obj.mouthKeys.push([b - .02, .25, 1], [b + .03, 0, 0]);
      }); return obj;
    };
    obj.gasp = (wins) => { wins.forEach(([a, b, o = .95]) => obj.mouthKeys.push([a, o, 1], [b, o, 1], [b + .06, 0, 0])); return obj; };

    obj.flush = () => {
      // joints
      Object.keys(obj.tracks).forEach(name => {
        const pts = obj.tracks[name].slice().sort((a, b) => a[0] - b[0]);
        E.anim(parts[name], pts.map(([t, d]) => [t, { transform: `rotate(${d}deg)` }]));
      });
      // root
      if (obj.rootSegs.length) {
        const segs = obj.rootSegs.slice().sort((a, b) => a[0][0] - b[0][0]);
        let x = 0, y = 0, fl = 1, rt = 0, sc = 1; const frames = [];
        const st = () => ({ transform: `translate(${x}px,${y}px) rotate(${rt}deg) scale(${sc * fl},${sc})` });
        segs.forEach((seg, k) => {
          if (k > 0) frames.push([seg[0][0] - .001, st()]);
          seg.forEach(([t, px, py, pf, pr, ps]) => { if (px !== undefined) x = px; if (py !== undefined) y = py; if (pf !== undefined) fl = pf; if (pr !== undefined) rt = pr; if (ps !== undefined) sc = ps; frames.push([t, st()]); });
        });
        E.anim(root, frames);
      }
      // mouth
      if (obj.mouthKeys.length) {
        const k = obj.mouthKeys.slice().sort((a, b) => a[0] - b[0]);
        E.anim(mouth, k.map(([t, v, o]) => [t, { transform: `scaleY(${Math.max(.08, v)})`, opacity: o }]));
      }
    };
    return obj;
  };

  /* ---------- motion helpers (angles in degrees; for flip=1 "right" is the viewer's right) ---------- */
  // walk: legs and arms swing between t0 and t1 with the given step time
  P.walk = (p, t0, t1, step = .34, amp = 24) => {
    const pts = { thighL: [], thighR: [], shinL: [], shinR: [], armL: [], armR: [] };
    let s = 1;
    pts.thighL.push([t0 - .05, 0]); pts.thighR.push([t0 - .05, 0]); pts.shinL.push([t0 - .05, 0]); pts.shinR.push([t0 - .05, 0]); pts.armL.push([t0 - .05, 0]); pts.armR.push([t0 - .05, 0]);
    for (let t = t0; t < t1; t += step) {
      pts.thighL.push([t, amp * s]); pts.thighR.push([t, -amp * s]);
      pts.shinL.push([t, s > 0 ? 0 : 22]); pts.shinR.push([t, s > 0 ? 22 : 0]);
      pts.armL.push([t, -18 * s]); pts.armR.push([t, 18 * s]); s = -s;
    }
    Object.keys(pts).forEach(k => { pts[k].push([t1, 0]); p.rot(k, pts[k]); });
  };
  // a quick gesture on one joint: ramp to `deg`, hold, come back
  P.pose = (p, name, t0, t1, deg, rest = 0, ramp = .18) => p.rot(name, [[t0 - ramp, rest], [t0, deg], [t1, deg], [t1 + ramp, rest]]);
  // wobble (shake/nod) on a joint
  P.wobble = (p, name, t0, t1, amp = 8, hz = 5, rest = 0) => { const pts = [[t0 - .05, rest]]; let s = 1; for (let t = t0; t < t1; t += 1 / hz / 2) { pts.push([t, rest + amp * s]); s = -s; } pts.push([t1, rest]); p.rot(name, pts); };
  P.pillow = null;
})();
