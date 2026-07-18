/* ═══════════════════════════════════════
   nexus-app.js  –  shared utilities
═══════════════════════════════════════ */

/* ── Global session-expiry guard ──────────────────────────────────────────
   Intercepts every fetch() call platform-wide. If the server returns HTTP 401
   (session expired / not authenticated) we redirect to the sign-in page once.
   The debounce flag prevents a cascade of redirects from concurrent requests.
   Pages that intentionally handle 401 themselves (e.g. login flow) won't be
   affected because they check res.ok / res.status before the interceptor can
   redirect (the actual redirect is a micro-task, not synchronous).
──────────────────────────────────────────────────────────────────────────── */
(function () {
  const SIGN_IN_URL   = '/home/signin?expired';
  const PUBLIC_PATHS  = ['/home/signin', '/home/signup', '/home/landing_page', '/api/auth/login', '/api/auth/register'];
  let   _redirecting  = false;

  const _nativeFetch = window.fetch.bind(window);

  window.fetch = async function (input, init) {
    const url = (typeof input === 'string') ? input : (input instanceof Request ? input.url : String(input));

    // Never intercept auth / public endpoints to avoid redirect loops
    const isPublic = PUBLIC_PATHS.some(p => url.includes(p));

    const response = await _nativeFetch(input, init);

    if (!isPublic && response.status === 401 && !_redirecting) {
      _redirecting = true;
      // Small delay so any in-flight .json() calls on the same response don't crash
      setTimeout(() => { window.location.href = SIGN_IN_URL; }, 100);
    }

    return response;
  };
})();

/* ── API base ── */
const API = {
  base: '/api',
  async req(method, path, body) {
    const opts = { method, headers: { 'Content-Type': 'application/json' }, credentials: 'include' };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(this.base + path, opts);
    const data = await res.json();
    if (!data.success) throw new Error(data.message || 'Request failed');
    return data.data;
  },
  get  (path)       { return this.req('GET',    path); },
  post (path, body) { return this.req('POST',   path, body); },
  put  (path, body) { return this.req('PUT',    path, body); },
  del  (path)       { return this.req('DELETE', path); },
};

/* ── Auth helpers ── */
const Auth = {
  async login(email, password) {
    const user = await API.post('/auth/login', { email, password });
    return user;
  },
  async register(fullName, email, password, referralCode) {
    return API.post('/auth/register', { fullName, email, password, referralCode });
  },
  async logout() {
    await API.post('/auth/logout');
    window.location.href = '/home/landing_page';
  },
  async me() {
    try { return await API.get('/auth/me'); }
    catch { return null; }
  }
};

/* ── Navigate ── */
function go(page) { window.location.href = '/home/' + page; }

/* ── Toast ── */
function showToast(msg, type = 'info') {
  let t = document.getElementById('__toast');
  if (!t) {
    t = document.createElement('div');
    t.id = '__toast';
    t.style.cssText = `position:fixed;bottom:100px;left:50%;transform:translateX(-50%);
      z-index:9999;padding:12px 22px;border-radius:14px;font-weight:700;font-size:14px;
      font-family:'Space Grotesk',sans-serif;transition:opacity .3s;white-space:nowrap;`;
    document.body.appendChild(t);
  }
  const colors = { success:'#34D399', error:'#F87171', info:'#22D3EE' };
  t.style.background = 'rgba(7,11,20,.95)';
  t.style.border = `1px solid ${colors[type] || colors.info}`;
  t.style.color  = colors[type] || colors.info;
  t.textContent  = msg;
  t.style.opacity = '1';
  clearTimeout(t.__timer);
  t.__timer = setTimeout(() => t.style.opacity = '0', 3000);
}

/* ── Copy to clipboard ── */
function copyText(text, label = 'Copied!') {
  navigator.clipboard.writeText(text).then(() => showToast(label, 'success'));
}

/* ── Format numbers ── */
function fmtUsd(n) {
  return '$' + parseFloat(n).toLocaleString(undefined, { minimumFractionDigits:2, maximumFractionDigits:2 });
}
function fmtVol(n) {
  if (n >= 1e9) return (n/1e9).toFixed(2) + 'B';
  if (n >= 1e6) return (n/1e6).toFixed(2) + 'M';
  return n.toLocaleString();
}

/* ── Coin data ── */
const COINS = [
  { name:'Bitcoin',     sym:'BTC',  price:68520.00, change:2.77,  icon:'https://cryptologos.cc/logos/bitcoin-btc-logo.png',        vol:32450000000 },
  { name:'Ethereum',    sym:'ETH',  price:2131.90,  change:1.12,  icon:'https://cryptologos.cc/logos/ethereum-eth-logo.png',       vol:15200000000 },
  { name:'Solana',      sym:'SOL',  price:145.67,   change:5.12,  icon:'https://cryptologos.cc/logos/solana-sol-logo.png',         vol:4200000000  },
  { name:'BNB',         sym:'BNB',  price:580.45,   change:-0.45, icon:'https://cryptologos.cc/logos/bnb-bnb-logo.png',            vol:1800000000  },
  { name:'XRP',         sym:'XRP',  price:0.620,    change:1.20,  icon:'https://cryptologos.cc/logos/xrp-xrp-logo.png',            vol:1200000000  },
  { name:'Cardano',     sym:'ADA',  price:0.452,    change:0.85,  icon:'https://cryptologos.cc/logos/cardano-ada-logo.png',        vol:450000000   },
  { name:'Avalanche',   sym:'AVAX', price:35.21,    change:-2.10, icon:'https://cryptologos.cc/logos/avalanche-avax-logo.png',     vol:680000000   },
  { name:'Dogecoin',    sym:'DOGE', price:0.165,    change:12.45, icon:'https://cryptologos.cc/logos/dogecoin-doge-logo.png',      vol:2100000000  },
  { name:'Tether',      sym:'USDT', price:1.00,     change:0.00,  icon:'https://cryptologos.cc/logos/tether-usdt-logo.png',        vol:55000000000 },
  { name:'Polkadot',    sym:'DOT',  price:7.21,     change:1.45,  icon:'https://cryptologos.cc/logos/polkadot-new-dot-logo.png',   vol:250000000   },
  { name:'Chainlink',   sym:'LINK', price:18.45,    change:-0.85, icon:'https://cryptologos.cc/logos/chainlink-link-logo.png',     vol:450000000   },
  { name:'Polygon',     sym:'MATIC',price:0.92,     change:2.10,  icon:'https://cryptologos.cc/logos/polygon-matic-logo.png',      vol:320000000   },
  { name:'Litecoin',    sym:'LTC',  price:88.45,    change:1.15,  icon:'https://cryptologos.cc/logos/litecoin-ltc-logo.png',       vol:550000000   },
  { name:'Shiba Inu',   sym:'SHIB', price:0.000027, change:5.45,  icon:'https://cryptologos.cc/logos/shiba-inu-shib-logo.png',     vol:850000000   },
  { name:'Bitcoin Cash',sym:'BCH',  price:420.12,   change:3.20,  icon:'https://cryptologos.cc/logos/bitcoin-cash-bch-logo.png',   vol:380000000   },
  { name:'Uniswap',     sym:'UNI',  price:11.25,    change:-1.45, icon:'https://cryptologos.cc/logos/uniswap-uni-logo.png',        vol:150000000   },
  { name:'Cosmos',      sym:'ATOM', price:10.45,    change:0.75,  icon:'https://cryptologos.cc/logos/cosmos-atom-logo.png',        vol:120000000   },
  { name:'Near',        sym:'NEAR', price:6.45,     change:8.12,  icon:'https://cryptologos.cc/logos/near-protocol-near-logo.png', vol:350000000   },
  { name:'Aptos',       sym:'APT',  price:14.25,    change:6.45,  icon:'https://cryptologos.cc/logos/aptos-apt-logo.png',          vol:185000000   },
  { name:'Arbitrum',    sym:'ARB',  price:1.85,     change:2.15,  icon:'https://cryptologos.cc/logos/arbitrum-arb-logo.png',       vol:245000000   },
];

/* Live price jitter (client-side simulation) */
function startPriceJitter(onUpdate) {
  setInterval(() => {
    COINS.forEach(c => {
      if (c.sym === 'USDT') return;
      c.price  = Math.max(0.0001, c.price + c.price * (Math.random() - 0.5) * 0.002);
      c.change = c.change + (Math.random() - 0.5) * 0.1;
    });
    if (onUpdate) onUpdate(COINS);
  }, 3000);
}

function getCoin(sym) { return COINS.find(c => c.sym === sym) || COINS[0]; }

function fmtPrice(c) {
  if (!c) return '0.00';
  const p = c.price;
  if (p < 0.001) return p.toFixed(8);
  if (p < 1)     return p.toFixed(4);
  return p.toLocaleString(undefined, { minimumFractionDigits:2, maximumFractionDigits:2 });
}

/* ── Networks / Fees ── */
const NETWORKS = {
  BTC:  ['Bitcoin','BEP20'],
  ETH:  ['ERC20','BEP20','Arbitrum One'],
  USDT: ['TRC20','ERC20','BEP20','Solana'],
  SOL:  ['Solana','BEP20'],
  BNB:  ['BEP20','BEP2'],
  XRP:  ['Ripple','BEP20'],
  ADA:  ['Cardano','BEP20'],
  AVAX: ['Avalanche C-Chain','BEP20'],
  DOGE: ['Dogecoin','BEP20'],
};
const NET_FEES = {
  BTC:  { Bitcoin:0.0005, BEP20:0.0001 },
  ETH:  { ERC20:0.005, BEP20:0.0005, 'Arbitrum One':0.0002 },
  USDT: { TRC20:1, ERC20:15, BEP20:0.5, Solana:0.1 },
  SOL:  { Solana:0.01, BEP20:0.005 },
  BNB:  { BEP20:0.0005, BEP2:0.0005 },
  XRP:  { Ripple:0.2, BEP20:0.1 },
  ADA:  { Cardano:1, BEP20:0.5 },
  AVAX: { 'Avalanche C-Chain':0.01, BEP20:0.005 },
  DOGE: { Dogecoin:5, BEP20:1 },
};

const STAKING_POOLS = [
  { id:'1', sym:'BTC',  apr:4.5, days:30,  min:0.001 },
  { id:'2', sym:'ETH',  apr:6.2, days:60,  min:0.01  },
  { id:'3', sym:'SOL',  apr:8.5, days:90,  min:1     },
  { id:'4', sym:'BNB',  apr:7.8, days:30,  min:0.1   },
  { id:'5', sym:'ADA',  apr:5.4, days:60,  min:10    },
  { id:'6', sym:'AVAX', apr:9.2, days:90,  min:2     },
];

const WITHDRAWAL_LIMITS = {
  'VIP Level 1': { perTx:50000,  daily:200000  },
  'VIP Level 2': { perTx:200000, daily:1000000 },
  'Verified':    { perTx:10000,  daily:50000   },
  'Basic':       { perTx:1000,   daily:2000    },
  'Unverified':  { perTx:0,      daily:0       },
};

/* ── SVG Icons ── */
const ICONS = {
  zap:     `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>`,
  home:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
  chart:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>`,
  repeat:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>`,
  wallet:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"/><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"/><path d="M18 12a2 2 0 0 0 0 4h4v-4z"/></svg>`,
  user:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
  arrow_r: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>`,
  arrow_l: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>`,
  copy:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>`,
  eye:     `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`,
  eye_off: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`,
  check:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>`,
  x:       `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`,
  bell:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>`,
  search:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`,
  plus:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>`,
  minus:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>`,
  logout:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>`,
  shield:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
  gift:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/><line x1="12" y1="22" x2="12" y2="7"/><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/></svg>`,
  activity:`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`,
  history: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="12 8 12 12 14 14"/><path d="M3.05 11a9 9 0 1 0 .5-4.5L1 4v4h4"/></svg>`,
  share:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>`,
  upload:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 16 12 12 8 16"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"/></svg>`,
  camera:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>`,
  lock:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>`,
  mail:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>`,
  ticket:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v2z"/><line x1="9" y1="12" x2="15" y2="12"/></svg>`,
  alert:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
  file:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>`,
  settings:`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`,
  star:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
  users:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  phone:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 13.5 19.79 19.79 0 0 1 1.62 4.9a2 2 0 0 1 1.99-2.18h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 10.09A16 16 0 0 0 14 16.09l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>`,
  globe:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`,
  map_pin: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`,
  qr:      `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="3" height="3"/><line x1="17" y1="14" x2="17" y2="14"/><line x1="20" y1="14" x2="20" y2="17"/><line x1="20" y1="20" x2="17" y2="20"/></svg>`,
  credit:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>`,
  trend_up:`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>`,
  briefcase:`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/><line x1="12" y1="12" x2="12" y2="12"/></svg>`,
  pie:     `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></svg>`,
  calc:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="2" width="16" height="20" rx="2"/><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="10" x2="8" y2="10"/><line x1="12" y1="10" x2="12" y2="10"/><line x1="16" y1="10" x2="16" y2="10"/><line x1="8" y1="14" x2="8" y2="14"/><line x1="12" y1="14" x2="12" y2="14"/><line x1="16" y1="14" x2="16" y2="14"/><line x1="8" y1="18" x2="16" y2="18"/></svg>`,
};

function icon(name, size=20, color='currentColor') {
  const svg = ICONS[name];
  if (!svg) return '';
  return svg.replace('<svg', `<svg width="${size}" height="${size}" style="color:${color}"`);
}

/* ── Bottom nav builder ── */
function buildBottomNav(active) {
  const items = [
    { label:'Home',   key:'home',   page:'dashboard' },
    { label:'Market', key:'pie',    page:'market'    },
    { label:'Trade',  key:'repeat', page:'trade'     },
    { label:'Assets', key:'wallet', page:'assets'    },
    { label:'Mine',   key:'user',   page:'profile'   },
  ];
  return `<nav class="bottom-nav">
    <div class="bottom-nav-inner">
      ${items.map(i => `
        <a href="/home/${i.page}" class="nav-btn ${active===i.page?'active':''}">
          ${icon(i.key, 24)}
          <span>${i.label}</span>
        </a>`).join('')}
    </div>
  </nav>`;
}