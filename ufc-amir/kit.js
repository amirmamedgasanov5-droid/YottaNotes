/* Characters, backgrounds and props for the "Amir in UFC" parody. Needs ../shared/engine.js */
(() => {
  const { el, G, raw } = E;
  const K = window.K = {};
  const limb = (g, pts, col) => {
    const o = G(g), w = G(g);
    pts.forEach(([x1, y1, x2, y2, c]) => {
      el('line', { x1, y1, x2, y2, stroke: '#111', 'stroke-width': 10.5, 'stroke-linecap': 'round' }, o);
      el('line', { x1, y1, x2, y2, stroke: c || col || '#fff', 'stroke-width': 6, 'stroke-linecap': 'round' }, w);
    });
  };
  const headCircle = (g, cy, r) => el('circle', { cx: 0, cy, r, fill: '#fff', stroke: '#111', 'stroke-width': 3.5 }, g);

  /* ---------- characters (feet at 0,0) ---------- */
  K.Amir = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 2.5, 300);
    limb(body, [[-7, -34, -8, 0, '#1d1d22'], [7, -34, 8, 0, '#1d1d22']]);
    raw(body, '<path d="M-15 -50 L15 -50 L17 -28 L-17 -28Z" fill="#17171c" stroke="#111" stroke-width="3" stroke-linejoin="round"/><path d="M-15 -50 L-17 -28 M15 -50 L17 -28" stroke="#d62828" stroke-width="3"/>' +
      '<path d="M-15 -78 L15 -78 L17 -48 L-17 -48Z" fill="#2a56c6" stroke="#111" stroke-width="3" stroke-linejoin="round"/><rect x="-6" y="-70" width="12" height="13" rx="2" fill="#e8eefc" opacity=".92"/>');
    limb(body, [[-15, -72, -27, -48], [15, -72, 27, -48]]);
    const head = G(body); headCircle(head, -92, 19);
    el('path', { d: 'M-19 -96 Q-17 -121 -6 -112 Q0 -124 8 -112 Q19 -118 19 -96 Q0 -105 -19 -96Z', fill: '#5a3a22', stroke: '#111', 'stroke-width': 1.5 }, head);
    const belt = G(body, { opacity: 0 }); raw(belt, '<rect x="-19" y="-54" width="38" height="9" rx="3" fill="#e8b32a" stroke="#111" stroke-width="2"/><circle cx="0" cy="-49.5" r="4.6" fill="#f6d86a" stroke="#111"/>');
    return { g, body, head, belt, faceY: -91, eyeR: 4.4 };
  };
  K.Khabib = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 2, 520);
    limb(body, [[-8, -40, -9, 0, '#1d1d22'], [8, -40, 9, 0, '#1d1d22']]);
    raw(body, '<path d="M-20 -88 L20 -88 L23 -42 L-23 -42Z" fill="#26262c" stroke="#111" stroke-width="3" stroke-linejoin="round"/>');
    limb(body, [[-20, -80, -33, -48], [20, -80, 33, -50]]);
    const hand = G(body, { transform: 'translate(35 -52)' });
    const head = G(body); headCircle(head, -106, 21);
    el('path', { d: 'M-20 -104 Q-23 -76 0 -74 Q23 -76 20 -104 Q10 -91 0 -91 Q-10 -91 -20 -104Z', fill: '#111' }, head);
    raw(head, '<path d="M-24 -112 Q-27 -148 0 -150 Q27 -148 24 -112 Q0 -122 -24 -112Z" fill="#1c1c20" stroke="#333" stroke-width="2"/><path d="M-16 -128 l3 6 M-6 -138 l2 7 M6 -138 l-2 7 M16 -128 l-3 6 M0 -126 l0 6" stroke="#444" stroke-width="2" stroke-linecap="round"/>');
    return { g, body, head, hand, faceY: -106, eyeR: 4.4 };
  };
  K.Conor = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 3, 280);
    limb(body, [[-8, -44, -9, 0, '#1c2a4a'], [8, -44, 9, 0, '#1c2a4a']]);
    raw(body, '<path d="M-20 -88 L20 -88 L23 -42 L-23 -42Z" fill="#1c2a4a" stroke="#111" stroke-width="3" stroke-linejoin="round"/><path d="M0 -88 L-8 -70 L0 -50 L8 -70Z" fill="#fff" stroke="#111" stroke-width="1.5"/><path d="M-2.5 -80 L2.5 -80 L3.5 -56 L0 -52 L-3.5 -56Z" fill="#e8b32a" stroke="#111" stroke-width="1"/>');
    limb(body, [[-20, -80, -33, -50, '#1c2a4a'], [20, -80, 33, -50, '#1c2a4a']]);
    const belt = G(body); raw(belt, '<rect x="-22" y="-60" width="44" height="10" rx="3" fill="#e8b32a" stroke="#111" stroke-width="2"/><circle cx="0" cy="-55" r="5" fill="#f6d86a" stroke="#111"/>');
    const head = G(body); headCircle(head, -108, 21);
    el('path', { d: 'M-19 -104 Q-20 -84 0 -82 Q20 -84 19 -104 Q10 -96 0 -96 Q-10 -96 -19 -104Z', fill: '#a9461a' }, head);
    el('path', { d: 'M-22 -110 Q-26 -140 0 -138 Q26 -140 22 -110 Q10 -124 0 -118 Q-10 -124 -22 -110Z', fill: '#c4561c', stroke: '#111', 'stroke-width': 1.5 }, head);
    return { g, body, head, belt, faceY: -108, eyeR: 4.4 };
  };
  K.Ref = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 2, 400);
    limb(body, [[-7, -38, -8, 0, '#17171c'], [7, -38, 8, 0, '#17171c']]);
    raw(body, '<path d="M-17 -82 L17 -82 L19 -40 L-19 -40Z" fill="#fff" stroke="#111" stroke-width="3" stroke-linejoin="round"/><g fill="#111"><rect x="-11" y="-82" width="4" height="42"/><rect x="-3" y="-82" width="4" height="42"/><rect x="5" y="-82" width="4" height="42"/></g><path d="M-6 -80 L0 -76 L-6 -72Z M6 -80 L0 -76 L6 -72Z" fill="#d62828"/>');
    limb(body, [[-17, -75, -29, -50], [17, -75, 29, -50]]);
    const head = G(body); headCircle(head, -100, 19);
    raw(head, '<path d="M-16 -112 Q0 -126 16 -112 Q0 -116 -16 -112Z" fill="#17171c"/><circle cx="14" cy="-72" r="3" fill="#e8b32a" stroke="#111"/>');
    return { g, body, head, faceY: -99, eyeR: 4.2 };
  };

  /* busts for the TV and the picture-in-picture */
  K.bustComm = (g) => raw(g, '<path d="M-34 40 Q-30 8 0 6 Q30 8 34 40Z" fill="#4a5568" stroke="#111" stroke-width="3"/><circle cx="0" cy="-14" r="20" fill="#fff" stroke="#111" stroke-width="3.5"/><path d="M-20 -18 Q0 -42 20 -18 Q0 -26 -20 -18Z" fill="#2a2a2a"/><path d="M-22 -14 Q-24 -40 0 -40 Q24 -40 22 -14" fill="none" stroke="#111" stroke-width="3.5"/><circle cx="-22" cy="-10" r="5" fill="#e8b32a" stroke="#111" stroke-width="2"/><path d="M-22 -6 Q-18 6 -6 4" fill="none" stroke="#111" stroke-width="2.5"/><circle cx="-5" cy="4" r="3.5" fill="#222" stroke="#111"/><circle cx="-7" cy="-14" r="2.6" fill="#111"/><circle cx="7" cy="-14" r="2.6" fill="#111"/><ellipse cx="0" cy="-1" rx="4.5" ry="3.5" fill="#111"/>');
  K.bustConor = (g) => raw(g, '<path d="M-34 40 Q-30 8 0 6 Q30 8 34 40Z" fill="#1c2a4a" stroke="#111" stroke-width="3"/><path d="M0 6 L-8 24 L0 40 L8 24Z" fill="#fff"/><path d="M-3 12 L3 12 L4 34 L0 38 L-4 34Z" fill="#e8b32a"/><circle cx="0" cy="-14" r="20" fill="#fff" stroke="#111" stroke-width="3.5"/><path d="M-18 -10 Q-19 8 0 10 Q19 8 18 -10 Q9 -3 0 -3 Q-9 -3 -18 -10Z" fill="#a9461a"/><path d="M-21 -16 Q-25 -42 0 -40 Q25 -42 21 -16 Q9 -30 0 -24 Q-9 -30 -21 -16Z" fill="#c4561c" stroke="#111" stroke-width="1.5"/><path d="M-10 -22 l7 3 M10 -22 l-7 3" stroke="#111" stroke-width="2.4" stroke-linecap="round"/><circle cx="-7" cy="-14" r="2.4" fill="#111"/><circle cx="7" cy="-14" r="2.4" fill="#111"/><path d="M-6 -3 q6 6 12 0" fill="none" stroke="#111" stroke-width="2.4"/>');

  /* ---------- props ---------- */
  K.glass = (parent) => { const g = G(parent); raw(g, '<path d="M-7 -14 L7 -14 L5 4 L-5 4Z" fill="#e8c27a" stroke="#111" stroke-width="2"/><path d="M-7 -14 L7 -14 L6 -10 L-6 -10Z" fill="#f4efe0" opacity=".7"/><path d="M7 -10 q7 2 0 9" fill="none" stroke="#111" stroke-width="2"/>'); return g; };
  K.steam = (parent) => { const g = G(parent); raw(g, '<path d="M-3 -18 q-4 -6 0 -12 q4 -6 0 -12" fill="none" stroke="#fff" stroke-width="2" opacity=".75"/><path d="M3 -18 q4 -6 0 -12 q-4 -6 0 -12" fill="none" stroke="#fff" stroke-width="2" opacity=".6"/>'); E.flicker(g, .8, 4); return g; };
  K.bear = (parent) => { const g = G(parent); raw(g, '<circle cx="-12" cy="-44" r="7" fill="#7a5230" stroke="#111" stroke-width="2.5"/><circle cx="12" cy="-44" r="7" fill="#7a5230" stroke="#111" stroke-width="2.5"/><ellipse cx="0" cy="-24" rx="22" ry="24" fill="#8a5e36" stroke="#111" stroke-width="3"/><circle cx="0" cy="-42" r="16" fill="#8a5e36" stroke="#111" stroke-width="3"/><ellipse cx="0" cy="-36" rx="7" ry="5" fill="#d9b48a" stroke="#111" stroke-width="2"/><circle cx="0" cy="-38" r="2.4" fill="#111"/><circle cx="-6" cy="-46" r="2" fill="#111"/><circle cx="6" cy="-46" r="2" fill="#111"/><ellipse cx="0" cy="-8" rx="13" ry="6" fill="#e8e4d4" stroke="#111" stroke-width="2.5"/><circle cx="-5" cy="-11" r="3" fill="#6bbf59"/><circle cx="3" cy="-12" r="3" fill="#d94f4f"/><circle cx="8" cy="-10" r="2.6" fill="#6bbf59"/>'); return g; };
  K.banana = (parent) => { const g = G(parent); raw(g, '<path d="M-14 -6 Q0 8 16 -10 Q4 4 -10 -2Z" fill="#f6d530" stroke="#111" stroke-width="2.5" stroke-linejoin="round"/><path d="M-14 -6 l-3 -3" stroke="#111" stroke-width="3" stroke-linecap="round"/>'); return g; };
  K.peel = (parent) => { const g = G(parent); raw(g, '<path d="M-16 0 Q-8 -8 0 0 Q8 -8 16 0 Q8 4 0 2 Q-8 4 -16 0Z" fill="#f6d530" stroke="#111" stroke-width="2.5" stroke-linejoin="round"/><path d="M-6 0 l-4 6 M4 0 l4 6" stroke="#d9b524" stroke-width="3" stroke-linecap="round"/>'); return g; };
  K.stars = (parent) => { const g = G(parent); raw(g, '<polygon points="-26,0 -23,-8 -15,-8 -21,-13 -19,-21 -26,-16 -33,-21 -31,-13 -37,-8 -29,-8" fill="#ffd23f" stroke="#111" stroke-width="1.5"/><polygon points="22,-6 25,-14 33,-14 27,-19 29,-27 22,-22 15,-27 17,-19 11,-14 19,-14" fill="#ffd23f" stroke="#111" stroke-width="1.5"/><polygon points="0,-30 3,-38 11,-38 5,-43 7,-51 0,-46 -7,-51 -5,-43 -11,-38 -3,-38" fill="#ffd23f" stroke="#111" stroke-width="1.5"/>'); g.animate([{ transform: 'rotate(-12deg)' }, { transform: 'rotate(12deg)' }], { duration: 400, iterations: Infinity, direction: 'alternate' }); return g; };
  K.pot = (parent) => { const g = G(parent); raw(g, '<ellipse cx="0" cy="0" rx="26" ry="7" fill="#8a8f98" stroke="#111" stroke-width="2.5"/><path d="M-26 0 L-22 -28 L22 -28 L26 0Z" fill="#a8adb6" stroke="#111" stroke-width="2.5" stroke-linejoin="round"/><ellipse cx="0" cy="-28" rx="22" ry="5" fill="#d9a441" stroke="#111" stroke-width="2"/><circle cx="-8" cy="-31" r="5" fill="#f4efe0" stroke="#111"/><circle cx="4" cy="-32" r="5" fill="#f4efe0" stroke="#111"/><circle cx="12" cy="-29" r="4" fill="#f4efe0" stroke="#111"/>'); return g; };
  K.num = (t0, t1, text, x, y, color = '#fff') => {
    const g = G(E.layers.ui); const tx = el('text', { x, y, 'text-anchor': 'middle', 'font-size': 40, 'font-weight': 900, fill: color, stroke: '#111', 'stroke-width': 7, 'paint-order': 'stroke' }, g); tx.textContent = text; E.pop(g, t0, t1);
  };

  /* ---------- backgrounds (full height, floor from y=400) ---------- */
  K.bedroom = (g) => {
    raw(g, '<rect width="360" height="640" fill="#9db4d8"/><rect y="400" width="360" height="240" fill="#b08a5a"/><g stroke="#8a6a40" stroke-width="2"><line x1="0" y1="440" x2="360" y2="440"/><line x1="0" y1="490" x2="360" y2="490"/><line x1="0" y1="550" x2="360" y2="550"/><line x1="0" y1="620" x2="360" y2="620"/></g><rect y="396" width="360" height="8" fill="#e8e4d4"/>' +
      '<rect x="36" y="96" width="104" height="140" fill="#cfeaff" stroke="#e8e4d4" stroke-width="7"/><line x1="88" y1="96" x2="88" y2="236" stroke="#e8e4d4" stroke-width="5"/><line x1="36" y1="166" x2="140" y2="166" stroke="#e8e4d4" stroke-width="5"/><circle cx="116" cy="130" r="15" fill="#fff4a8"/><path d="M28 90 Q40 160 32 244 L52 244 Q46 160 52 90Z" fill="#d9788a"/><path d="M148 90 Q136 160 144 244 L124 244 Q130 160 124 90Z" fill="#d9788a"/>' +
      '<rect x="170" y="110" width="76" height="100" fill="#10213d" stroke="#e8b32a" stroke-width="6"/><path d="M186 176 L208 132 L230 176 L218 170 L208 192 L198 170Z" fill="#e8b32a"/><circle cx="208" cy="128" r="9" fill="#e8b32a"/><path d="M200 126 l-8 3 l8 2Z" fill="#d9a441"/>' +
      '<ellipse cx="130" cy="520" rx="120" ry="34" fill="#c9506a" opacity=".85"/><ellipse cx="130" cy="520" rx="86" ry="22" fill="#e8b32a" opacity=".5"/>');
  };
  K.wardrobe = (g) => {
    raw(g, '<rect x="246" y="190" width="108" height="232" fill="#7a4e2c" stroke="#2a1a0d" stroke-width="5"/><rect x="254" y="200" width="46" height="212" fill="#8a5a34" stroke="#2a1a0d" stroke-width="3"/><circle cx="294" cy="310" r="4" fill="#d4a93c"/><rect x="246" y="414" width="108" height="10" fill="#2a1a0d"/>');
    const door = G(g); raw(door, '<rect x="304" y="200" width="46" height="212" fill="#8a5a34" stroke="#2a1a0d" stroke-width="3"/><circle cx="310" cy="310" r="4" fill="#d4a93c"/>');
    door.style.transformOrigin = '350px 300px'; door.style.transformBox = 'view-box';
    return { door };
  };
  K.gym = (g) => {
    raw(g, '<rect width="360" height="640" fill="#d9d6cc"/><rect y="400" width="360" height="240" fill="#2f66bd"/><g stroke="#cfe0ff" stroke-width="3" opacity=".8"><line x1="0" y1="470" x2="360" y2="470"/><line x1="0" y1="560" x2="360" y2="560"/><line x1="120" y1="400" x2="60" y2="640"/><line x1="240" y1="400" x2="300" y2="640"/></g><rect y="396" width="360" height="8" fill="#7a8396"/>' +
      '<rect x="100" y="56" width="160" height="36" rx="8" fill="#26262c" stroke="#e8b32a" stroke-width="4"/><text x="180" y="82" text-anchor="middle" font-size="22" font-weight="900" fill="#e8b32a">EAGLE GYM</text>' +
      '<line x1="52" y1="0" x2="52" y2="190" stroke="#333" stroke-width="4"/><rect x="30" y="190" width="44" height="130" rx="18" fill="#a2332b" stroke="#111" stroke-width="4"/><rect x="30" y="228" width="44" height="8" fill="#7a2620"/><rect x="30" y="268" width="44" height="8" fill="#7a2620"/>' +
      '<rect x="200" y="112" width="140" height="92" rx="6" fill="#16161a" stroke="#555" stroke-width="5"/>' +
      '<rect x="262" y="330" width="86" height="70" fill="#8a5a34" stroke="#2a1a0d" stroke-width="4"/><rect x="258" y="322" width="94" height="12" fill="#a2703f" stroke="#2a1a0d" stroke-width="3"/><path d="M282 322 L282 286 Q298 270 314 286 L314 322Z" fill="#c8cdd4" stroke="#111" stroke-width="3"/><rect x="292" y="268" width="12" height="8" fill="#888" stroke="#111"/>');
    const tv = G(g, { transform: 'translate(270 168)' }); // content area of the TV
    raw(tv, '<rect x="-64" y="-50" width="128" height="84" fill="#2a3a52"/>');
    const comm = G(tv); E.raw(comm, ''); const cb = G(comm, { transform: 'translate(0 10) scale(.9)' }); K.bustComm(cb);
    const con = G(tv); const cn = G(con, { transform: 'translate(0 10) scale(.9)' }); K.bustConor(cn);
    const live = G(tv); raw(live, '<rect x="-64" y="18" width="128" height="16" fill="#d62828"/><text x="-56" y="30" font-size="11" font-weight="900" fill="#fff">LIVE</text>');
    return { tv, comm, con, live, conBust: cn, commBust: cb };
  };
  K.weigh = (g) => {
    raw(g, '<rect width="360" height="640" fill="#2a1a3a"/><rect y="400" width="360" height="240" fill="#3a2a52"/><g opacity=".22" fill="#fff"><polygon points="60,0 120,0 200,400 0,400"/><polygon points="240,0 300,0 360,400 160,400"/></g>' +
      '<rect x="124" y="56" width="224" height="50" rx="10" fill="#d62828" stroke="#fff" stroke-width="3"/><text x="236" y="92" text-anchor="middle" font-size="30" font-weight="900" fill="#fff">WEIGH-IN</text><rect y="396" width="360" height="8" fill="#6a4a8a"/>');
  };
  K.scale = (g, ok) => {
    const s = G(g); raw(s, '<rect x="124" y="414" width="112" height="22" rx="6" fill="#9aa0aa" stroke="#111" stroke-width="3"/><rect x="164" y="300" width="32" height="116" fill="#c8cdd4" stroke="#111" stroke-width="3"/><circle cx="180" cy="290" r="44" fill="#f4efe0" stroke="#111" stroke-width="4"/>');
    const needle = G(s, { transform: 'translate(180 290)' }); const nd = G(needle); raw(nd, '<line x1="0" y1="0" x2="0" y2="-34" stroke="#d62828" stroke-width="4" stroke-linecap="round"/><circle r="5" fill="#111"/>');
    return { s, nd, needle };
  };
  K.arena = (g) => {
    let crowd = ''; const cols = ['#d62828', '#2a56c6', '#e8b32a', '#3aa56a', '#8a4fc6', '#e8e4d4'];
    for (let r = 0; r < 3; r++) for (let i = 0; i < 12; i++) { const x = 14 + i * 30 + (r % 2) * 14, y = 60 + r * 46; crowd += `<circle cx="${x}" cy="${y}" r="9" fill="#e8d4b8" stroke="#111" stroke-width="2"/><rect x="${x - 10}" y="${y + 9}" width="20" height="20" rx="6" fill="${cols[(i + r * 2) % 6]}" stroke="#111" stroke-width="2"/>`; }
    raw(g, '<rect width="360" height="640" fill="#0b1020"/><g opacity=".2" fill="#fff"><polygon points="50,0 90,0 170,330 10,330"/><polygon points="270,0 310,0 350,330 190,330"/></g>' +
      '<g>' + crowd + '</g><rect y="0" width="360" height="190" fill="#0b1020" opacity=".25"/>' +
      '<polygon points="20,430 340,430 360,610 0,610" fill="#2b4f8f" stroke="#111" stroke-width="3"/><circle cx="180" cy="515" r="46" fill="none" stroke="#e8e4d4" stroke-width="3" opacity=".6"/>' +
      '<rect x="20" y="214" width="320" height="222" fill="none" stroke="#c8cdd4" stroke-width="5"/><g stroke="#aab0b8" stroke-width="1.6" opacity=".75">' + Array.from({ length: 14 }, (_, i) => `<line x1="${20 + i * 25}" y1="214" x2="${20 + i * 25 + 60}" y2="436"/><line x1="${20 + i * 25 + 60}" y1="214" x2="${20 + i * 25}" y2="436"/>`).join('') + '</g>' +
      '<rect x="14" y="206" width="12" height="236" fill="#d62828" stroke="#111" stroke-width="2"/><rect x="334" y="206" width="12" height="236" fill="#2a56c6" stroke="#111" stroke-width="2"/>');
  };
  K.dinner = (g) => {
    raw(g, '<rect width="360" height="640" fill="#f1dba5"/><rect y="420" width="360" height="220" fill="#c9a97a"/><g stroke="#b18c5c" stroke-width="2"><line x1="0" y1="470" x2="360" y2="470"/><line x1="0" y1="540" x2="360" y2="540"/></g><rect x="40" y="86" width="110" height="130" fill="#cfeaff" stroke="#8a6a40" stroke-width="7"/><line x1="95" y1="86" x2="95" y2="216" stroke="#8a6a40" stroke-width="5"/><rect x="210" y="96" width="110" height="70" rx="6" fill="#e8c05a" stroke="#8a6a40" stroke-width="4"/><text x="265" y="140" text-anchor="middle" font-size="26" font-weight="900" fill="#8a5a1a">❤</text>');
  };
  K.table = (g) => { raw(g, '<rect x="10" y="404" width="340" height="18" rx="6" fill="#a2703f" stroke="#2a1a0d" stroke-width="4"/><rect x="26" y="420" width="308" height="86" fill="#8a5a34" stroke="#2a1a0d" stroke-width="4"/><g><ellipse cx="70" cy="402" rx="22" ry="6" fill="#f4efe0" stroke="#111" stroke-width="2"/><ellipse cx="150" cy="402" rx="22" ry="6" fill="#f4efe0" stroke="#111" stroke-width="2"/><ellipse cx="230" cy="402" rx="22" ry="6" fill="#f4efe0" stroke="#111" stroke-width="2"/><ellipse cx="305" cy="402" rx="20" ry="6" fill="#f4efe0" stroke="#111" stroke-width="2"/></g><g fill="#fff6dc" stroke="#111" stroke-width="1.5"><circle cx="62" cy="398" r="5"/><circle cx="74" cy="399" r="5"/><circle cx="144" cy="398" r="5"/><circle cx="156" cy="399" r="5"/><circle cx="224" cy="398" r="5"/><circle cx="236" cy="399" r="5"/></g>'); };
})();
