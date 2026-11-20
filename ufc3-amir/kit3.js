/* Extra specs, props and the detailed arena for version 3. Needs kit2.js (K2) and engine/puppet. */
(() => {
  const { el, G, raw } = E;
  const OUT = '#1b1414';
  const K3 = window.K3 = {};

  /* more people: Khabib gets eagle wings; ring girl, fan, two corner men */
  Object.assign(K2.specs, {
    K: Object.assign({}, K2.specs.K, { wings: true }),
    Y: { skin: '#f1c7a2', hair: 'long', hairColor: '#2c1a10', top: 'tee', topColor: '#d62828', sash: '#f6f6f6', bottom: 'pants', bottomColor: '#17171c', shoes: '#d62828', scale: .92, cheeks: true, brow: '#2c1a10' },
    F: { skin: '#e6b48e', hair: 'short', hairColor: '#6a3a1a', hat: 'cap', hatColor: '#d62828', top: 'tee', topColor: '#2f9e44', emblem: 'ball', bottom: 'pants', bottomColor: '#3a4a6a', shoes: '#f4f4f4', belly: 16, scale: .96, cheeks: true },
    S1: { skin: '#d9a273', hair: 'short', hairColor: '#181818', top: 'tee', topColor: '#1d1d22', bottom: 'pants', bottomColor: '#2a2a30', shoes: '#111', scale: .98, brow: '#181818' },
    S2: { skin: '#f2c9a0', hair: 'tonsure', hairColor: '#999', mustache: '#777', top: 'tee', topColor: '#e8e8ee', bottom: 'pants', bottomColor: '#33333a', shoes: '#222', scale: .93, belly: 10 },
  });

  /* props */
  K3.sign = (parent, text = 'ROUND 1') => { const g = G(parent); raw(g, `<rect x="-5" y="-6" width="10" height="170" rx="4" fill="#c8914f" stroke="${OUT}" stroke-width="3"/><rect x="-92" y="-72" width="184" height="74" rx="8" fill="#fff8e6" stroke="${OUT}" stroke-width="4"/><text x="0" y="-26" text-anchor="middle" font-size="34" font-weight="900" fill="#d62828" font-family="DejaVu Sans, sans-serif">${text}</text><rect x="-78" y="-14" width="156" height="6" fill="#e8b32a"/>`); return g; };
  K3.kebab = (parent) => { const g = G(parent); raw(g, `<line x1="-30" y1="0" x2="48" y2="0" stroke="#c8cdd4" stroke-width="5" stroke-linecap="round"/><rect x="-18" y="-10" width="22" height="20" rx="8" fill="#9a4a22" stroke="${OUT}" stroke-width="3"/><rect x="6" y="-10" width="22" height="20" rx="8" fill="#b9602a" stroke="${OUT}" stroke-width="3"/><rect x="30" y="-9" width="14" height="18" rx="6" fill="#9a4a22" stroke="${OUT}" stroke-width="3"/>`); return g; };
  K3.towel = (parent) => { const g = G(parent); raw(g, `<rect x="-30" y="-12" width="60" height="26" rx="6" fill="#f4f4f4" stroke="${OUT}" stroke-width="3"/><rect x="-30" y="-2" width="60" height="6" fill="#2a56c6"/>`); return g; };
  K3.bottle = (parent) => { const g = G(parent); raw(g, `<rect x="-9" y="-30" width="18" height="40" rx="7" fill="#7fd0ff" stroke="${OUT}" stroke-width="3"/><rect x="-5" y="-38" width="10" height="10" rx="3" fill="#e8e4d4" stroke="${OUT}" stroke-width="2.4"/>`); return g; };
  K3.rollingPin = (parent) => { const g = G(parent); raw(g, `<rect x="-62" y="-9" width="124" height="18" rx="9" fill="#d9a35a" stroke="${OUT}" stroke-width="3.4"/><rect x="-86" y="-5" width="30" height="10" rx="5" fill="#c8914f" stroke="${OUT}" stroke-width="3"/><rect x="56" y="-5" width="30" height="10" rx="5" fill="#c8914f" stroke="${OUT}" stroke-width="3"/>`); return g; };
  K3.mesh = (parent, x, y, w, h, op = .85) => raw(parent, `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="url(#meshpat)" opacity="${op}"/>`);

  /* the arena: crowd, lights, jumbotron, and a real cage with a back fence you can climb */
  K3.arena = (g) => {
    const defs = E.defs;
    if (!document.getElementById('meshpat')) {
      const pt = el('pattern', { id: 'meshpat', width: 22, height: 22, patternUnits: 'userSpaceOnUse', patternTransform: 'rotate(45)' }, defs);
      pt.innerHTML = '<rect width="22" height="22" fill="none"/><path d="M0 0 H22 M0 0 V22" stroke="#b8bfc9" stroke-width="2.6"/><path d="M0 0 H22 M0 0 V22" stroke="#fff" stroke-width="1" opacity=".7"/>';
      const lg = el('linearGradient', { id: 'matgrad', x1: 0, y1: 0, x2: 0, y2: 1 }, defs); lg.innerHTML = '<stop offset="0" stop-color="#3a63a8"/><stop offset="1" stop-color="#1f3d78"/>';
      const lg2 = el('linearGradient', { id: 'pipegrad', x1: 0, y1: 0, x2: 1, y2: 0 }, defs); lg2.innerHTML = '<stop offset="0" stop-color="#f1f3f6"/><stop offset=".5" stop-color="#b8bfc9"/><stop offset="1" stop-color="#7d8592"/>';
      const rg = el('radialGradient', { id: 'spot' }, defs); rg.innerHTML = '<stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>';
      const bg = el('linearGradient', { id: 'arenabg', x1: 0, y1: 0, x2: 0, y2: 1 }, defs); bg.innerHTML = '<stop offset="0" stop-color="#05081a"/><stop offset=".6" stop-color="#121a3a"/><stop offset="1" stop-color="#1b2550"/>';
    }
    const cols = ['#d62828', '#2a56c6', '#e8b32a', '#3aa56a', '#8a4fc6', '#e8e4d4', '#e87a2a', '#2fb8c9'];
    const signs = { 3: 'ДАЙТЕ ПОЕСТЬ', 9: 'МИША, МЫ С ТОБОЙ', 15: 'ГЕНА, ПРОСНИСЬ', 21: 'ОРЁЛ ЛЕТИ!' };
    let crowd = '';
    for (let r = 0; r < 4; r++) for (let i = 0; i < 28; i++) {
      const x = 22 + i * 46 + (r % 2) * 20, y = 70 + r * 52, hc = ['#e8d4b8', '#d9b48a', '#f2d6b8', '#c99a6a', '#8a5a3a'][(i * 3 + r * 2) % 5], sh = cols[(i + r * 3) % 8];
      const up = (i * 7 + r * 5) % 6 === 0;
      crowd += `<g opacity="${1 - r * .08}"><rect x="${x - 16}" y="${y + 14}" width="32" height="30" rx="11" fill="${sh}" stroke="${OUT}" stroke-width="2.4"/><circle cx="${x}" cy="${y}" r="15" fill="${hc}" stroke="${OUT}" stroke-width="2.4"/><circle cx="${x - 5}" cy="${y - 2}" r="2.2" fill="${OUT}"/><circle cx="${x + 5}" cy="${y - 2}" r="2.2" fill="${OUT}"/><path d="M${x - 6} ${y + 6} q6 ${up ? 7 : 4} 12 0" fill="none" stroke="${OUT}" stroke-width="2.2"/>${up ? `<path d="M${x - 14} ${y + 20} L${x - 22} ${y - 14} M${x + 14} ${y + 20} L${x + 22} ${y - 14}" stroke="${hc}" stroke-width="7" stroke-linecap="round"/>` : ''}</g>`;
    }
    raw(g, `<rect width="1280" height="720" fill="url(#arenabg)"/>` +
      `<g opacity=".5"><ellipse cx="260" cy="40" rx="150" ry="260" fill="url(#spot)"/><ellipse cx="1020" cy="40" rx="150" ry="260" fill="url(#spot)"/><ellipse cx="640" cy="20" rx="220" ry="300" fill="url(#spot)"/></g>` + crowd +
      Object.entries(signs).map(([i, t]) => { const x = 22 + i * 46, y = 36; return `<g transform="translate(${x} ${y}) rotate(${(i % 2 ? 5 : -5)})"><rect x="-74" y="-26" width="156" height="44" rx="4" fill="#fffbe8" stroke="${OUT}" stroke-width="2.8"/><text x="4" y="3" text-anchor="middle" font-size="15" font-weight="900" fill="${OUT}" font-family="DejaVu Sans, sans-serif">${t}</text></g>`; }).join('') +
      // jumbotron
      `<rect x="470" y="8" width="340" height="104" rx="12" fill="#0b0f1c" stroke="#e8b32a" stroke-width="5"/><rect x="480" y="18" width="320" height="84" rx="6" fill="#142040"/><text x="640" y="72" text-anchor="middle" font-size="40" font-weight="900" fill="#ffd23f" font-family="DejaVu Sans, sans-serif">FIGHT NIGHT</text>` +
      // floor / mat
      `<polygon points="160,588 1120,588 1262,704 18,704" fill="url(#matgrad)" stroke="${OUT}" stroke-width="4"/><ellipse cx="640" cy="648" rx="230" ry="46" fill="none" stroke="#e8e4d4" stroke-width="5" opacity=".5"/><g stroke="#fff" opacity=".12" stroke-width="2"><line x1="400" y1="590" x2="330" y2="704"/><line x1="880" y1="590" x2="950" y2="704"/><line x1="640" y1="590" x2="640" y2="704"/></g>` +
      // the cage: back fence + angled side panels, mesh, rails, posts
      `<polygon points="96,284 262,250 262,590 96,640" fill="#0b0f1c" opacity=".35"/><polygon points="1184,284 1018,250 1018,590 1184,640" fill="#0b0f1c" opacity=".35"/>` +
      `<rect x="262" y="250" width="756" height="340" fill="#0b0f1c" opacity=".25"/>`);
    K3.mesh(g, 262, 252, 756, 338); raw(g, `<g>` + `<polygon points="96,284 262,252 262,590 96,640" fill="url(#meshpat)" opacity=".8"/><polygon points="1184,284 1018,252 1018,590 1184,640" fill="url(#meshpat)" opacity=".8"/></g>` +
      `<rect x="258" y="238" width="764" height="18" rx="6" fill="url(#pipegrad)" stroke="${OUT}" stroke-width="3.4"/><polygon points="96,276 262,238 262,256 96,296" fill="url(#pipegrad)" stroke="${OUT}" stroke-width="3"/><polygon points="1184,276 1018,238 1018,256 1184,296" fill="url(#pipegrad)" stroke="${OUT}" stroke-width="3"/>` +
      `<rect x="246" y="238" width="22" height="364" rx="8" fill="url(#pipegrad)" stroke="${OUT}" stroke-width="3.4"/><rect x="1012" y="238" width="22" height="364" rx="8" fill="url(#pipegrad)" stroke="${OUT}" stroke-width="3.4"/>` +
      `<rect x="88" y="270" width="20" height="378" rx="8" fill="#d62828" stroke="${OUT}" stroke-width="3.4"/><rect x="1172" y="270" width="20" height="378" rx="8" fill="#2a56c6" stroke="${OUT}" stroke-width="3.4"/>` +
      `<rect x="96" y="590" width="1088" height="14" fill="#1b1414" opacity=".5"/>`);
    // commentary desk (in front of the cage, left)
    const desk = G(g); raw(desk, `<rect x="14" y="590" width="290" height="124" rx="8" fill="#3e2e22" stroke="${OUT}" stroke-width="4"/><rect x="4" y="574" width="310" height="22" rx="5" fill="#8a6a44" stroke="${OUT}" stroke-width="3.6"/><rect x="170" y="540" width="14" height="34" fill="#333"/><ellipse cx="177" cy="538" rx="18" ry="12" fill="#222" stroke="${OUT}" stroke-width="2.6"/><rect x="40" y="540" width="80" height="34" rx="4" fill="#10141c" stroke="${OUT}" stroke-width="3"/><rect x="46" y="546" width="68" height="22" fill="#2a5a8a"/>`);
    return { desk };
  };
})();
