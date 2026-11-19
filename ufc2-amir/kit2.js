/* Characters, rooms and props for "Амир и большой бой". 1280 x 720, detailed flat cartoon style. */
(() => {
  const { el, G, raw } = E;
  const K2 = window.K2 = {};
  const OUT = '#1b1414';

  /* ---------- people ---------- */
  K2.specs = {
    A: { skin: '#f4cfa6', hair: 'spiky', hairColor: '#4a2c17', top: 'tee', topColor: '#2a56c6', emblem: 'ball', bottom: 'leggings+shorts', bottomColor: '#15151a', stripe: '#d62828', legColor: '#1a1a1f', shoes: null, scale: .7, headScale: 1.25, cheeks: true },
    K: { skin: '#d9a273', hair: 'short', hairColor: '#171717', top: 'bare', bottom: 'shorts', bottomColor: '#f4f4f4', stripe: '#141414', towel: true, gloves: '#5b606a', wrist: '#2a56c6', hat: 'papakha', hatColor: '#fbfbfb', beard: '#141414', brow: '#141414', shoes: null, scale: 1.0 },
    C: { skin: '#f4d3b8', hair: 'swept', hairColor: '#c4561c', beard: '#a9461a', beardStyle: 'short', brow: '#8a3a12', top: 'suit', topColor: '#1c2a4a', tie: '#e8b32a', pinstripe: true, bottom: 'pants', bottomColor: '#1c2a4a', shoes: '#2a1a12', scale: .98 },
    R: { skin: '#efc7a2', hair: 'tonsure', hairColor: '#8d8d8d', mustache: '#6b5a48', top: 'ref', topColor: '#f6f6f6', belly: 10, bottom: 'pants', bottomColor: '#17171c', shoes: '#17171c', scale: .88 },
    V: { skin: '#f2c9a0', hair: 'short', hairColor: '#3a2a1a', glasses: true, headset: true, top: 'jacket', topColor: '#4a5568', bottom: 'pants', bottomColor: '#2a2f3a', shoes: '#222', scale: .96 },
    G: { skin: '#e6b48e', hair: 'tonsure', hairColor: '#6a6a6a', mustache: '#4a3d30', hat: 'cap', hatColor: '#243a5e', top: 'uniform', topColor: '#2a3a5a', belly: 30, bottom: 'pants', bottomColor: '#1d2433', shoes: '#111', scale: 1.0, cheeks: true },
    M: { skin: '#f2c9a0', hair: 'mom', hairColor: '#7a4a2a', top: 'dress', topColor: '#c0546a', apron: true, bottom: 'dress', shoes: '#d9d0c0', scale: .95, cheeks: true, belly: 4 },
  };
  K2.person = (parent, who) => P.make(parent, K2.specs[who]);

  /* ---------- the bear (side/three-quarter, friendly, like in the reference) ---------- */
  K2.bear = (parent) => {
    const g = G(parent), body = G(g); E.bob(body, 3, 1400);
    const legs = (x, y, back) => `<g><rect x="${x - 24}" y="${y}" width="48" height="${-y}" rx="22" fill="${back ? '#7a431f' : '#8a4f2a'}" stroke="${OUT}" stroke-width="3.4"/><path d="M${x - 14} -6 l0 8 M${x} -6 l0 9 M${x + 14} -6 l0 8" stroke="#d9b44a" stroke-width="4" stroke-linecap="round"/></g>`;
    raw(body, legs(-92, -90, true) + legs(78, -90, true) +
      `<ellipse cx="0" cy="-138" rx="168" ry="110" fill="#8a4f2a" stroke="${OUT}" stroke-width="3.6"/><path d="M60 -230 Q150 -170 120 -64 Q150 -150 70 -214Z" fill="#000" opacity=".1"/>` + legs(-60, -90, false) + legs(104, -90, false));
    const head = G(body); head.style.transformOrigin = '150px -150px';
    raw(head, `<circle cx="104" cy="-222" r="26" fill="#8a4f2a" stroke="${OUT}" stroke-width="3.4"/><circle cx="104" cy="-222" r="13" fill="#d9a17a"/><circle cx="196" cy="-226" r="26" fill="#8a4f2a" stroke="${OUT}" stroke-width="3.4"/><circle cx="196" cy="-226" r="13" fill="#d9a17a"/>` +
      `<ellipse cx="150" cy="-170" rx="80" ry="70" fill="#8a4f2a" stroke="${OUT}" stroke-width="3.6"/><ellipse cx="164" cy="-146" rx="46" ry="36" fill="#e3b88e" stroke="${OUT}" stroke-width="3"/><ellipse cx="172" cy="-162" rx="14" ry="10" fill="${OUT}"/><path d="M172 -152 L172 -140 M150 -134 Q168 -118 190 -134" fill="none" stroke="${OUT}" stroke-width="3.4" stroke-linecap="round"/>`);
    const eyesOpen = G(head), eyesShut = G(head);
    raw(eyesOpen, `<ellipse cx="124" cy="-190" rx="11" ry="12" fill="#fff" stroke="${OUT}" stroke-width="2.6"/><circle cx="127" cy="-189" r="5.4" fill="#7a1010"/><circle cx="128" cy="-191" r="1.8" fill="#fff"/><ellipse cx="168" cy="-190" rx="11" ry="12" fill="#fff" stroke="${OUT}" stroke-width="2.6"/><circle cx="171" cy="-189" r="5.4" fill="#7a1010"/><circle cx="172" cy="-191" r="1.8" fill="#fff"/>`);
    raw(eyesShut, `<path d="M112 -190 Q124 -180 136 -190 M156 -190 Q168 -180 180 -190" fill="none" stroke="${OUT}" stroke-width="3.6" stroke-linecap="round"/>`);
    eyesShut.style.opacity = 0;
    const zzz = G(g); raw(zzz, '<text x="190" y="-300" font-size="46" font-weight="900" fill="#fff" stroke="#1b1414" stroke-width="3" paint-order="stroke">Z</text><text x="228" y="-340" font-size="34" font-weight="900" fill="#fff" stroke="#1b1414" stroke-width="3" paint-order="stroke">z</text>'); zzz.style.opacity = 0;
    return { g, body, head, eyesOpen, eyesShut, zzz };
  };

  /* ---------- small props ---------- */
  K2.sandwich = (parent) => { const g = G(parent); raw(g, `<path d="M-34 0 Q-38 -20 -10 -22 L20 -22 Q40 -20 36 0 Z" fill="#d9a35a" stroke="${OUT}" stroke-width="3"/><rect x="-36" y="-4" width="74" height="9" rx="4" fill="#d9625a" stroke="${OUT}" stroke-width="2.4"/><rect x="-34" y="3" width="70" height="6" rx="3" fill="#8ac14f" stroke="${OUT}" stroke-width="2"/><path d="M-34 8 L36 8 Q38 22 14 24 L-12 24 Q-36 22 -34 8 Z" fill="#d9a35a" stroke="${OUT}" stroke-width="3"/>`); return g; };
  K2.plane = (parent) => { const g = G(parent); raw(g, `<path d="M-40 0 L40 -16 L10 8 Z" fill="#fff" stroke="${OUT}" stroke-width="3" stroke-linejoin="round"/><path d="M-40 0 L10 8 L-6 22 Z" fill="#dfe6f5" stroke="${OUT}" stroke-width="3" stroke-linejoin="round"/><path d="M-30 2 l24 3" stroke="#d62828" stroke-width="2.4"/>`); return g; };
  K2.letter = (parent) => { const g = G(parent); raw(g, `<rect x="-60" y="-80" width="120" height="150" rx="6" fill="#fffbe8" stroke="${OUT}" stroke-width="3"/><g stroke="#2a56c6" stroke-width="3.4" stroke-linecap="round" fill="none"><path d="M-44 -54 q10 -12 20 0 t20 0 t20 0 t20 0"/><path d="M-44 -28 q10 -12 20 0 t20 0 t20 0"/><path d="M-44 -2 q10 -12 20 0 t20 0 t20 0 t20 0"/><path d="M-44 24 q10 -12 20 0 t20 0"/></g><circle cx="30" cy="48" r="12" fill="#d62828"/>`); return g; };
  K2.pin = (parent) => { const g = G(parent); raw(g, `<rect x="-6" y="-70" width="12" height="52" rx="6" fill="#c8914f" stroke="${OUT}" stroke-width="3"/><rect x="-22" y="-98" width="44" height="34" rx="14" fill="#d9a35a" stroke="${OUT}" stroke-width="3.4"/><rect x="-22" y="-50" width="44" height="34" rx="14" fill="#d9a35a" stroke="${OUT}" stroke-width="3.4"/><rect x="-18" y="-96" width="36" height="140" rx="16" fill="#d9a35a" stroke="${OUT}" stroke-width="3.4"/>`.replace(/<rect x="-22"[^>]*>/g, '')); return g; };
  K2.bigPapakha = (parent) => {
    const g = G(parent); let c = '';
    [[-60, -250, 56], [0, -266, 62], [60, -250, 56], [-78, -190, 56], [78, -190, 56], [-84, -126, 52], [84, -126, 52], [-70, -66, 48], [70, -66, 48], [0, -190, 74], [0, -110, 70]].forEach(([x, y, r]) => c += `<circle cx="${x}" cy="${y}" r="${r}" fill="#fbfbfb" stroke="${OUT}" stroke-width="3.4"/>`);
    raw(g, c + `<rect x="-86" y="-80" width="172" height="40" rx="14" fill="#fbfbfb"/><g stroke="#c9ccd4" stroke-width="2.4" stroke-linecap="round" fill="none"><path d="M-40 -240 q4 8 0 16 M10 -250 q4 8 0 16 M50 -220 q4 8 0 16 M-60 -170 q4 8 0 16 M60 -150 q4 8 0 16 M-20 -120 q4 8 0 16 M30 -100 q4 8 0 16"/></g>`);
    return g;
  };
  K2.trophy = (parent) => { const g = G(parent); raw(g, `<path d="M-18 -50 L18 -50 L14 -20 Q0 -10 -14 -20 Z" fill="#e8b32a" stroke="${OUT}" stroke-width="3"/><rect x="-5" y="-22" width="10" height="14" fill="#e8b32a" stroke="${OUT}" stroke-width="2.4"/><rect x="-16" y="-10" width="32" height="10" rx="3" fill="#8a5a34" stroke="${OUT}" stroke-width="2.6"/><path d="M-18 -44 Q-34 -44 -26 -28 M18 -44 Q34 -44 26 -28" fill="none" stroke="${OUT}" stroke-width="3"/>`); return g; };
  K2.stars = (parent) => { const g = G(parent); raw(g, `<polygon points="-46,0 -40,-14 -26,-14 -37,-23 -33,-37 -46,-28 -59,-37 -55,-23 -66,-14 -52,-14" fill="#ffd23f" stroke="${OUT}" stroke-width="2.4"/><polygon points="40,-10 46,-24 60,-24 49,-33 53,-47 40,-38 27,-47 31,-33 20,-24 34,-24" fill="#ffd23f" stroke="${OUT}" stroke-width="2.4"/><polygon points="0,-50 6,-64 20,-64 9,-73 13,-87 0,-78 -13,-87 -9,-73 -20,-64 -6,-64" fill="#ffd23f" stroke="${OUT}" stroke-width="2.4"/>`); g.animate([{ transform: 'rotate(-14deg)' }, { transform: 'rotate(14deg)' }], { duration: 420, iterations: Infinity, direction: 'alternate' }); return g; };
  K2.tie = (parent) => { const g = G(parent); raw(g, `<path d="M-6 0 L6 0 L9 20 L0 28 L-9 20 Z" fill="#e8b32a" stroke="${OUT}" stroke-width="2.4"/><path d="M-8 26 L8 26 L14 90 L0 104 L-14 90 Z" fill="#e8b32a" stroke="${OUT}" stroke-width="2.4"/>`); return g; };
  K2.pot = (parent) => { const g = G(parent); raw(g, `<ellipse cx="0" cy="0" rx="70" ry="16" fill="#8a8f98" stroke="${OUT}" stroke-width="3.2"/><path d="M-70 0 L-62 -70 L62 -70 L70 0 Z" fill="#b4b9c2" stroke="${OUT}" stroke-width="3.2" stroke-linejoin="round"/><ellipse cx="0" cy="-70" rx="62" ry="13" fill="#e8a13a" stroke="${OUT}" stroke-width="3"/><circle cx="-22" cy="-76" r="14" fill="#f6efd8" stroke="${OUT}" stroke-width="2.6"/><circle cx="10" cy="-80" r="14" fill="#f6efd8" stroke="${OUT}" stroke-width="2.6"/><circle cx="36" cy="-72" r="12" fill="#f6efd8" stroke="${OUT}" stroke-width="2.6"/><path d="M-70 -50 q-20 0 -20 14 M70 -50 q20 0 20 14" fill="none" stroke="${OUT}" stroke-width="5" stroke-linecap="round"/>`); return g; };

  /* ---------- rooms ---------- */
  const floorLines = (y0, y1, col, n) => { let s = ''; for (let i = 1; i < n; i++) { const y = y0 + (y1 - y0) * (i / n) ** 1.3; s += `<line x1="0" y1="${y}" x2="1280" y2="${y}" stroke="${col}" stroke-width="2.4" opacity=".7"/>`; } return s; };
  const sofa = (x, y, col = '#8a4a26') => `<g transform="translate(${x} ${y})"><rect x="0" y="40" width="300" height="130" rx="20" fill="${col}" stroke="${OUT}" stroke-width="3.6"/><rect x="14" y="0" width="272" height="80" rx="24" fill="${col}" stroke="${OUT}" stroke-width="3.6"/><rect x="-26" y="50" width="62" height="116" rx="26" fill="${col}" stroke="${OUT}" stroke-width="3.6"/><rect x="264" y="50" width="62" height="116" rx="26" fill="${col}" stroke="${OUT}" stroke-width="3.6"/><rect x="38" y="92" width="112" height="62" rx="14" fill="#a05a30" stroke="${OUT}" stroke-width="3"/><rect x="150" y="92" width="112" height="62" rx="14" fill="#a05a30" stroke="${OUT}" stroke-width="3"/><rect x="20" y="164" width="22" height="14" fill="${OUT}"/><rect x="262" y="164" width="22" height="14" fill="${OUT}"/><path d="M40 8 Q150 30 260 8" fill="none" stroke="#000" opacity=".12" stroke-width="10"/></g>`;
  const painting = (x, y, w = 190, h = 120) => `<g transform="translate(${x} ${y})"><rect x="-8" y="-8" width="${w + 16}" height="${h + 16}" fill="#b4742f" stroke="${OUT}" stroke-width="3"/><rect width="${w}" height="${h}" fill="#7fb4e8"/><polygon points="0,${h} ${w * .22},${h * .4} ${w * .4},${h * .72} ${w * .62},${h * .24} ${w},${h * .8} ${w},${h}" fill="#5f7f96" stroke="${OUT}" stroke-width="2"/><polygon points="${w * .62},${h * .24} ${w * .56},${h * .42} ${w * .68},${h * .4}" fill="#fff"/><polygon points="${w * .22},${h * .4} ${w * .17},${h * .55} ${w * .27},${h * .55}" fill="#fff"/><rect y="${h * .8}" width="${w}" height="${h * .2}" fill="#4f8a58"/></g>`;
  K2.home = (g) => {
    raw(g, `<rect width="1280" height="720" fill="#f3d79d"/>` + Array.from({ length: 22 }, (_, i) => `<rect x="${i * 60}" y="0" width="30" height="540" fill="#f9e3b2" opacity=".6"/>`).join('') +
      `<rect y="520" width="1280" height="24" fill="#b4742f" stroke="${OUT}" stroke-width="3"/><rect y="544" width="1280" height="176" fill="#c88f55"/>` + floorLines(544, 720, '#a8723c', 6) +
      `<ellipse cx="560" cy="660" rx="330" ry="50" fill="#b83d3d" stroke="${OUT}" stroke-width="3"/><ellipse cx="560" cy="660" rx="260" ry="36" fill="#e8b32a" opacity=".85"/><ellipse cx="560" cy="660" rx="170" ry="22" fill="#b83d3d" opacity=".8"/>` +
      // window
      `<rect x="330" y="96" width="250" height="250" fill="#8fd0ff" stroke="${OUT}" stroke-width="4"/><rect x="320" y="86" width="270" height="270" fill="none" stroke="#fff" stroke-width="12"/><line x1="455" y1="96" x2="455" y2="346" stroke="#fff" stroke-width="8"/><line x1="330" y1="221" x2="580" y2="221" stroke="#fff" stroke-width="8"/><circle cx="520" cy="150" r="26" fill="#fff4a8"/><path d="M300 80 Q330 200 310 360 L350 360 Q344 200 360 80Z" fill="#d9788a" stroke="${OUT}" stroke-width="3"/><path d="M610 80 Q580 200 600 360 L560 360 Q566 200 550 80Z" fill="#d9788a" stroke="${OUT}" stroke-width="3"/><rect x="316" y="352" width="278" height="16" fill="#e8e4d4" stroke="${OUT}" stroke-width="3"/><path d="M380 352 L380 330 M398 352 L398 322 M420 352 L420 334" stroke="#3a8a4a" stroke-width="7" stroke-linecap="round"/>` +
      // shelf with trophies
      `<rect x="660" y="230" width="270" height="14" rx="3" fill="#8a5a34" stroke="${OUT}" stroke-width="3"/>` +
      // TV stand + TV
      `<rect x="680" y="500" width="230" height="30" fill="#6a4a2c" stroke="${OUT}" stroke-width="3.4"/><rect x="700" y="530" width="20" height="14" fill="${OUT}"/><rect x="870" y="530" width="20" height="14" fill="${OUT}"/><rect x="680" y="298" width="230" height="170" rx="10" fill="#1d1d22" stroke="${OUT}" stroke-width="4"/><rect x="694" y="312" width="202" height="142" rx="4" fill="#2a3a52"/><rect x="760" y="468" width="70" height="30" fill="#2a2a30" stroke="${OUT}" stroke-width="3"/>` +
      sofa(960, 400) + painting(1010, 190, 200, 130) +
      // kitchen doorway on the left
      `<rect x="30" y="170" width="250" height="360" fill="${OUT}"/><rect x="42" y="182" width="226" height="348" fill="#f6ead0"/><rect x="42" y="440" width="226" height="90" fill="#c7b58c"/><rect x="190" y="230" width="60" height="150" rx="6" fill="#e8e4d4" stroke="${OUT}" stroke-width="3"/><rect x="196" y="236" width="48" height="60" fill="#fff"/><circle cx="238" cy="320" r="4" fill="${OUT}"/><rect x="60" y="380" width="110" height="60" rx="6" fill="#e8b32a" stroke="${OUT}" stroke-width="3"/><circle cx="90" cy="360" r="14" fill="#c0c6cf" stroke="${OUT}" stroke-width="3"/>`);
    const tv = G(g, { transform: 'translate(694 312)' });
    const trophies = [[690, 230], [790, 230], [890, 230]]; trophies.forEach(([x, y]) => { const t = K2.trophy(g); t.setAttribute('transform', `translate(${x} ${y}) scale(1.2)`); });
    return { tv };
  };
  K2.gym = (g) => {
    raw(g, `<rect width="1280" height="720" fill="#ffd98a"/><rect y="520" width="1280" height="24" fill="#c9634a" stroke="${OUT}" stroke-width="3"/><rect y="544" width="1280" height="176" fill="#cc8a52"/>` + floorLines(544, 720, '#a86d3c', 6) +
      // the cave (the bear's home)
      `<path d="M-20 560 L-20 330 Q40 230 160 250 Q260 180 360 290 Q430 360 420 560 Z" fill="#f3d9a0" stroke="${OUT}" stroke-width="4"/><path d="M60 330 Q120 290 150 340 M250 270 Q300 250 330 300" fill="none" stroke="#d9b270" stroke-width="10" stroke-linecap="round"/><path d="M70 560 L70 420 Q90 330 190 330 Q290 336 310 430 L330 560 Z" fill="#1a1414" stroke="${OUT}" stroke-width="4"/><ellipse cx="200" cy="560" rx="150" ry="18" fill="#7a4a2a" opacity=".5"/>` +
      // punching bag + mats + weights
      `<line x1="900" y1="0" x2="900" y2="270" stroke="${OUT}" stroke-width="5"/><rect x="860" y="270" width="80" height="210" rx="34" fill="#c2392b" stroke="${OUT}" stroke-width="4"/><rect x="860" y="320" width="80" height="12" fill="#8a2218"/><rect x="860" y="400" width="80" height="12" fill="#8a2218"/>` +
      `<ellipse cx="800" cy="640" rx="190" ry="34" fill="#2f66bd" stroke="${OUT}" stroke-width="3.4"/><ellipse cx="800" cy="640" rx="120" ry="20" fill="#3d7fe0" opacity=".7"/>` +
      `<g transform="translate(1100 600)"><rect x="-60" y="-14" width="120" height="14" rx="6" fill="#8d939c" stroke="${OUT}" stroke-width="3"/><rect x="-80" y="-34" width="22" height="54" rx="6" fill="#444850" stroke="${OUT}" stroke-width="3"/><rect x="58" y="-34" width="22" height="54" rx="6" fill="#444850" stroke="${OUT}" stroke-width="3"/></g>` +
      sofa(960, 400, '#8a4a26') + painting(1010, 190, 200, 130) + `<rect x="470" y="30" width="360" height="62" rx="10" fill="#27272d" stroke="#e8b32a" stroke-width="4"/><text x="650" y="76" text-anchor="middle" font-size="34" font-weight="900" fill="#e8b32a" font-family="DejaVu Sans, sans-serif">EAGLE GYM</text>`);
    const tvw = G(g, { transform: 'translate(470 150)' });
    raw(tvw, `<rect x="-6" y="-6" width="360" height="224" rx="12" fill="#1d1d22" stroke="${OUT}" stroke-width="4"/><rect x="8" y="8" width="332" height="196" rx="4" fill="#2a3a52"/>`);
    const screen = G(tvw);
    const cp = el('clipPath', { id: 'tvclip' }, E.defs); el('rect', { x: 8, y: 8, width: 332, height: 196 }, cp);
    const tvc = G(tvw); tvc.setAttribute('clip-path', 'url(#tvclip)');
    return { tvc, tv: tvw };
  };
  K2.weigh = (g) => {
    let flash = '';
    raw(g, `<rect width="1280" height="720" fill="#2a1a3a"/><g opacity=".2" fill="#fff"><polygon points="180,0 300,0 560,560 0,560"/><polygon points="980,0 1100,0 1280,560 760,560"/></g>` +
      `<path d="M0 0 L300 0 Q260 200 300 440 L0 440Z" fill="#7a1f3a" stroke="${OUT}" stroke-width="3"/><path d="M1280 0 L980 0 Q1020 200 980 440 L1280 440Z" fill="#7a1f3a" stroke="${OUT}" stroke-width="3"/>` +
      `<rect y="520" width="1280" height="200" fill="#3a2a52"/><rect y="508" width="1280" height="16" fill="#6a4a8a" stroke="${OUT}" stroke-width="3"/>` +
      `<rect x="390" y="60" width="500" height="90" rx="16" fill="#d62828" stroke="#fff" stroke-width="5"/><text x="640" y="125" text-anchor="middle" font-size="66" font-weight="900" fill="#fff" font-family="DejaVu Sans, sans-serif">WEIGH-IN</text>` +
      `<g transform="translate(120 560)"><rect x="-60" y="-80" width="120" height="14" rx="4" fill="#8a5a34" stroke="${OUT}" stroke-width="3"/><rect x="-54" y="-66" width="10" height="66" fill="#8a5a34" stroke="${OUT}" stroke-width="3"/><rect x="44" y="-66" width="10" height="66" fill="#8a5a34" stroke="${OUT}" stroke-width="3"/></g>`);
    // old doctor's scale
    const mk = () => { const s = G(g); raw(s, `<rect x="510" y="610" width="260" height="30" rx="8" fill="#9aa0aa" stroke="${OUT}" stroke-width="3.6"/><rect x="618" y="330" width="44" height="284" fill="#c8cdd4" stroke="${OUT}" stroke-width="3.6"/><circle cx="640" cy="300" r="76" fill="#f6efd8" stroke="${OUT}" stroke-width="4.4"/><circle cx="640" cy="300" r="62" fill="none" stroke="${OUT}" stroke-width="2"/><g stroke="${OUT}" stroke-width="3" stroke-linecap="round">` + Array.from({ length: 11 }, (_, i) => { const a = (-135 + i * 27) * Math.PI / 180; return `<line x1="${640 + Math.cos(a) * 52}" y1="${300 + Math.sin(a) * 52}" x2="${640 + Math.cos(a) * 62}" y2="${300 + Math.sin(a) * 62}"/>`; }).join('') + '</g>');
      const needleBase = G(s, { transform: 'translate(640 300)' }); const nd = G(needleBase); raw(nd, `<line x1="0" y1="0" x2="0" y2="-54" stroke="#d62828" stroke-width="6" stroke-linecap="round"/><circle r="8" fill="${OUT}"/>`); return { s, nd }; };
    return { scaleA: mk(), scaleB: mk() };
  };
  K2.arena = (g) => {
    let crowd = ''; const cols = ['#d62828', '#2a56c6', '#e8b32a', '#3aa56a', '#8a4fc6', '#e8e4d4', '#e87a2a'];
    const signs = { 3: 'ДАЙТЕ ПОЕСТЬ', 9: 'МИША, МЫ С ТОБОЙ', 15: 'Я ПРИШЁЛ ЗА ЧЕБУРЕКОМ', 21: 'ГЕНА, ПРОСНИСЬ' };
    for (let r = 0; r < 3; r++) for (let i = 0; i < 26; i++) {
      const x = 24 + i * 49 + (r % 2) * 22, y = 92 + r * 66, hc = ['#e8d4b8', '#d9b48a', '#f2d6b8', '#c99a6a'][(i * 3 + r) % 4];
      crowd += `<circle cx="${x}" cy="${y}" r="17" fill="${hc}" stroke="${OUT}" stroke-width="2.6"/><rect x="${x - 19}" y="${y + 17}" width="38" height="36" rx="12" fill="${cols[(i + r * 3) % 7]}" stroke="${OUT}" stroke-width="2.6"/><circle cx="${x - 6}" cy="${y - 2}" r="2.4" fill="${OUT}"/><circle cx="${x + 6}" cy="${y - 2}" r="2.4" fill="${OUT}"/><path d="M${x - 6} ${y + 7} q6 5 12 0" fill="none" stroke="${OUT}" stroke-width="2.4"/>`;
    }
    raw(g, `<rect width="1280" height="720" fill="#0d1226"/><g opacity=".18" fill="#fff"><polygon points="260,0 340,0 660,520 80,520"/><polygon points="940,0 1020,0 1240,520 700,520"/></g><g>${crowd}</g><rect width="1280" height="260" fill="#0d1226" opacity=".3"/>` +
      Object.entries(signs).map(([i, t]) => { const x = 24 + i * 49, y = 60; return `<g transform="translate(${x} ${y - 70}) rotate(${(i % 2 ? 4 : -4)})"><rect x="-70" y="-26" width="150" height="44" rx="4" fill="#fffbe8" stroke="${OUT}" stroke-width="2.6"/><text x="5" y="3" text-anchor="middle" font-size="15" font-weight="900" fill="#1b1414" font-family="DejaVu Sans, sans-serif">${t}</text></g>`; }).join('') +
      `<polygon points="120,560 1160,560 1280,700 0,700" fill="#2b4f8f" stroke="${OUT}" stroke-width="4"/><ellipse cx="640" cy="632" rx="170" ry="44" fill="none" stroke="#e8e4d4" stroke-width="5" opacity=".55"/>` +
      `<rect x="130" y="270" width="1020" height="290" fill="none" stroke="#c8cdd4" stroke-width="7"/><g stroke="#aab0b8" stroke-width="2.4" opacity=".7">` + Array.from({ length: 24 }, (_, i) => `<line x1="${130 + i * 43}" y1="270" x2="${130 + i * 43 + 90}" y2="560"/><line x1="${130 + i * 43 + 90}" y1="270" x2="${130 + i * 43}" y2="560"/>`).join('') + `</g>` +
      `<rect x="118" y="256" width="20" height="316" fill="#d62828" stroke="${OUT}" stroke-width="3"/><rect x="1142" y="256" width="20" height="316" fill="#2a56c6" stroke="${OUT}" stroke-width="3"/>` +
      `<rect x="470" y="20" width="340" height="86" rx="12" fill="#10141c" stroke="#e8b32a" stroke-width="5"/><text x="640" y="80" text-anchor="middle" font-size="46" font-weight="900" fill="#ffd23f" font-family="DejaVu Sans, sans-serif">FIGHT!</text>`);
    // commentator's desk (left, in front of the cage)
    const desk = G(g); raw(desk, `<rect x="30" y="560" width="250" height="110" rx="6" fill="#4a3a2c" stroke="${OUT}" stroke-width="4"/><rect x="20" y="548" width="270" height="20" rx="4" fill="#8a6a44" stroke="${OUT}" stroke-width="3.4"/><rect x="150" y="530" width="12" height="20" fill="#333"/><ellipse cx="156" cy="528" rx="14" ry="10" fill="#222" stroke="${OUT}" stroke-width="2.4"/>`);
    return { desk };
  };
  K2.kitchen = (g) => {
    raw(g, `<rect width="1280" height="720" fill="#f6e0a8"/><rect y="500" width="1280" height="24" fill="#b4742f" stroke="${OUT}" stroke-width="3"/><rect y="524" width="1280" height="196" fill="#cda072"/>` + floorLines(524, 720, '#b08455', 6) +
      `<rect x="70" y="110" width="300" height="250" fill="#8fd0ff" stroke="${OUT}" stroke-width="4"/><rect x="58" y="98" width="324" height="274" fill="none" stroke="#fff" stroke-width="12"/><line x1="220" y1="110" x2="220" y2="360" stroke="#fff" stroke-width="8"/><path d="M40 90 Q70 200 52 380 L92 380 Q84 200 104 90Z" fill="#d9788a" stroke="${OUT}" stroke-width="3"/><path d="M400 90 Q370 200 388 380 L348 380 Q356 200 336 90Z" fill="#d9788a" stroke="${OUT}" stroke-width="3"/>` +
      `<rect x="930" y="130" width="230" height="370" rx="14" fill="#e8ecf2" stroke="${OUT}" stroke-width="4"/><rect x="930" y="290" width="230" height="8" fill="#b9c0cc"/><rect x="1130" y="190" width="10" height="70" rx="5" fill="#8d939c"/><rect x="1130" y="330" width="10" height="90" rx="5" fill="#8d939c"/><rect x="960" y="150" width="60" height="60" fill="#fff" stroke="${OUT}" stroke-width="2.4"/><text x="990" y="190" text-anchor="middle" font-size="28" fill="#d62828">❤</text>` +
      `<rect x="470" y="130" width="280" height="40" rx="8" fill="#e8c05a" stroke="${OUT}" stroke-width="3.4"/><text x="610" y="160" text-anchor="middle" font-size="26" font-weight="900" fill="#8a5a1a" font-family="DejaVu Sans, sans-serif">❤ HOME ❤</text>`);
    return {};
  };
  K2.table = (g) => { raw(g, `<rect x="120" y="560" width="1040" height="30" rx="10" fill="#a2703f" stroke="${OUT}" stroke-width="4"/><rect x="150" y="590" width="980" height="130" fill="#8a5a34" stroke="${OUT}" stroke-width="4"/><rect x="120" y="556" width="1040" height="10" fill="#f4efe0" opacity=".5"/>` +
    [240, 440, 840, 1040].map(x => `<ellipse cx="${x}" cy="556" rx="52" ry="12" fill="#f4efe0" stroke="${OUT}" stroke-width="3"/><circle cx="${x - 14}" cy="548" r="9" fill="#fff6dc" stroke="${OUT}" stroke-width="2"/><circle cx="${x + 12}" cy="550" r="9" fill="#fff6dc" stroke="${OUT}" stroke-width="2"/>`).join('')); };
})();
