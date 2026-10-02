/* In-memory mock of /api/gacha, mirroring GachaService in the Spring Boot backend. */
const R = { COMMON: [1, 600, 3, 10, 'Common'], RARE: [2, 280, 8, 25, 'Rare'], EPIC: [3, 100, 25, 60, 'Epic'], LEGENDARY: [4, 20, 100, 200, 'Legendary'] };
const RARITIES = Object.keys(R);
const BANNERS = [
  ['STANDARD', 'Standard 10-pull', 1000, 1, false, 'Ten ideas at standard odds. At least one Rare or better.'],
  ['BOOSTED', 'Boosted 10-pull', 1500, 3, false, 'Triple Epic and Legendary odds. For people who want it more.'],
  ['WHALE', 'Whale 10-pull', 5000, 3, true, 'Boosted odds plus a guaranteed Legendary. Money can buy happiness.'],
];
const SHOP = [
  { code: 'pocket-change', name: 'Pocket change', pricePence: 99, gems: 300, tagline: 'Less than a coffee. Probably.' },
  { code: 'consultants-coffer', name: "Consultant's coffer", pricePence: 999, gems: 3300, tagline: 'Billable to the client? (No.)' },
  { code: 'partner-track', name: 'Partner track bundle', pricePence: 4999, gems: 18000, tagline: 'Skip the promotion panel.' },
  { code: 'whale-of-a-time', name: 'Whale of a time', pricePence: 9999, gems: 40000, tagline: 'Best value! Unlocks VIP 4 instantly.' },
];
const VIP = [[0, 'Free-to-play', 0], [1, 'Minnow', 99], [2, 'Dolphin', 999], [3, 'Shark', 4999], [4, 'Whale', 9999], [5, 'Leviathan', 49999]].map(([level, title, minSpendPence]) => ({ level, title, minSpendPence }));
const PITY = 50, DAILY = 300, GEMS_PER_XP = 5;
const ICON = { BOOK: '📖', COURSE: '💻', EVENT: '🎟️', PROGRAMME: '🎓', ARTICLE: '📰' };
const HAND = [
  ['act:playback', 'ACTION', 'COMMON', '🔁', 'Play it back', 'Summarise what you heard in one sentence before responding.'],
  ['act:five-whys', 'ACTION', 'COMMON', '❓', 'Five whys', "Ask 'why?' five times about one backlog request."],
  ['act:lunch-learn', 'ACTION', 'RARE', '🥪', 'Host a lunch and learn', 'Run a 20-minute session on something you know well.'],
  ['act:adr', 'ACTION', 'RARE', '🏛️', 'Write an ADR', 'Document one architectural decision on your project.'],
  ['act:coach-someone', 'ACTION', 'EPIC', '🌱', 'Coach someone for a month', 'Offer four 30-minute coaching sessions to a junior colleague.'],
  ['prj:skills-graph', 'PROJECT', 'EPIC', '🕸️', 'Skills graph', 'Visualise who knows what across the company.'],
  ['prj:meeting-cost', 'PROJECT', 'COMMON', '⏱️', 'Meeting cost clock', 'Show the running cost of a meeting.'],
  ['act:shadow-partner', 'ACTION', 'LEGENDARY', '👑', 'Shadow a partner on a pitch', 'Join a senior leader on a real client pitch.'],
  ['act:conference-talk', 'ACTION', 'LEGENDARY', '🏟️', 'Speak at a conference', 'Submit a talk to an external conference.'],
  ['prj:open-source', 'PROJECT', 'LEGENDARY', '🐙', 'Open-source a tool', 'Release something your team built as open source.'],
].map(([id, kind, rarity, icon, title, description]) => ({ id, kind, rarity, icon, title, description }));

module.exports = function gacha(CAT) {
  const pool = [...HAND, ...CAT.map((r) => {
    let rarity = r.level === 'Advanced' ? 'EPIC' : r.level === 'Intermediate' ? 'RARE' : 'COMMON';
    if (r.type === 'EVENT' && rarity !== 'EPIC') rarity = RARITIES[RARITIES.indexOf(rarity) + 1];
    return { id: 'res:' + r.id, kind: 'RESOURCE', rarity, icon: ICON[r.type] || '📚', title: r.title, description: r.description || '', url: r.url || undefined, resourceId: r.id };
  })];
  const wallets = {}; const pulls = [];
  const today = () => new Date().toISOString().slice(0, 10);
  const wal = (u) => (wallets[u.id] ||= { gems: 1000, pity: 0, total: 0, spend: 0, daily: null });
  const vipFor = (s) => VIP.filter((t) => s >= t.minSpendPence).pop();
  const weight = (b, r) => (r === 'EPIC' || r === 'LEGENDARY' ? R[r][1] * b[3] : R[r][1]);
  const odds = (b) => { const t = RARITIES.reduce((a, r) => a + weight(b, r), 0); return Object.fromEntries(RARITIES.map((r) => [r, Math.round((weight(b, r) * 1000) / t) / 10])); };
  const ideaView = (i) => ({ ...i, rarityLabel: R[i.rarity][4], stars: R[i.rarity][0] });
  const view = (w, u) => { const v = vipFor(w.spend); const nx = VIP[v.level + 1]; return { gems: w.gems, xp: u.xp, pityCount: w.pity, pityThreshold: PITY, dailyAvailable: w.daily !== today(), dailyGems: DAILY, vipLevel: v.level, vipTitle: v.title, xpBonusPercent: v.level * 10, fakeSpendPence: w.spend, nextVipTitle: nx?.title, nextVipPence: nx?.minSpendPence, totalPulls: w.total, gemsPerXp: GEMS_PER_XP }; };

  return function handle(p, m, body, me, send, res) {
    if (!p.startsWith('/gacha')) return false;
    const w = wal(me);
    if (p === '/gacha' && m === 'GET') return send(res, 200, { wallet: view(w, me), banners: BANNERS.map((b) => ({ code: b[0], name: b[1], cost: b[2], description: b[5], guaranteedLegendary: b[4], odds: odds(b) })), shop: SHOP, vipTiers: VIP, poolSize: pool.length, pullsPerBatch: 10, minXpExchange: 10 }), true;
    if (p === '/gacha/daily') { if (w.daily === today()) return send(res, 400, { message: "You've already claimed today's gems. Come back tomorrow (or visit the shop 👀)" }), true; w.daily = today(); w.gems += DAILY; return send(res, 200, view(w, me)), true; }
    if (p === '/gacha/exchange') { const xp = +body.xp; if (!(xp >= 10) || xp > me.xp) return send(res, 400, { message: `You only have ${me.xp} XP` }), true; me.xp -= xp; w.gems += xp * GEMS_PER_XP; return send(res, 200, view(w, me)), true; }
    let mm;
    if ((mm = p.match(/^\/gacha\/shop\/(.+)$/))) { const pk = SHOP.find((s) => s.code === mm[1]); if (!pk) return send(res, 404, { message: 'Gem pack not found' }), true; w.gems += pk.gems; w.spend += pk.pricePence; return send(res, 200, view(w, me)), true; }
    if (p === '/gacha/pull') {
      const b = BANNERS.find((x) => x[0] === (body.banner || 'STANDARD')); if (!b) return send(res, 400, { message: 'Unknown banner. Use STANDARD, BOOSTED or WHALE' }), true;
      if (w.gems < b[2]) return send(res, 400, { message: `Not enough gems: you need ${b[2]} 💎 and have ${w.gems}. Claim your daily gems, trade XP or visit the shop.` }), true;
      w.gems -= b[2];
      const total = RARITIES.reduce((a, r) => a + weight(b, r), 0);
      const rs = []; const pity = []; let since = w.pity;
      for (let i = 0; i < 10; i++) {
        let r; if (since + 1 >= PITY) { r = 'LEGENDARY'; pity[i] = true; } else { let n = Math.floor(Math.random() * total); r = RARITIES.find((x) => (n -= weight(b, x)) < 0); }
        rs.push(r); since = r === 'LEGENDARY' ? 0 : since + 1;
      }
      if (b[4] && !rs.includes('LEGENDARY')) { rs[9] = 'LEGENDARY'; pity[9] = true; since = 0; } else if (rs.every((r) => r === 'COMMON')) { rs[9] = 'RARE'; pity[9] = true; }
      const bonus = vipFor(w.spend).level * 10; const owned = new Set(pulls.filter((x) => x.userId === me.id).map((x) => x.ideaId));
      const batchId = 'b' + Math.random().toString(36).slice(2); let xpT = 0, refT = 0;
      const cards = rs.map((r, i) => { const c = pool.filter((x) => x.rarity === r); const idea = c[Math.floor(Math.random() * c.length)]; const isNew = !owned.has(idea.id); owned.add(idea.id); const xp = Math.floor((R[r][2] * (100 + bonus)) / 100); const refund = isNew ? 0 : R[r][3]; xpT += xp; refT += refund; pulls.push({ userId: me.id, batchId, banner: b[0], ideaId: idea.id, rarity: r, xp, duplicate: !isNew, createdAt: new Date().toISOString() }); return { idea: ideaView(idea), isNew, xp, gemRefund: refund, pity: !!pity[i] }; });
      w.gems += refT; w.pity = since; w.total += 10; me.xp += xpT;
      return send(res, 200, { batchId, banner: b[0], cards, gemsSpent: b[2], gemsRefunded: refT, xpGained: xpT, badgesUnlocked: [], wallet: view(w, me) }), true;
    }
    if (p === '/gacha/collection') {
      const mine = pulls.filter((x) => x.userId === me.id); const byId = {}; mine.forEach((x) => { (byId[x.ideaId] ||= { copies: 0, firstPulledAt: x.createdAt }).copies++; });
      const items = pool.map((i) => (byId[i.id] ? { ...i, stars: R[i.rarity][0], owned: true, ...byId[i.id] } : { id: i.id, kind: i.kind, rarity: i.rarity, stars: R[i.rarity][0], owned: false, copies: 0 }))
        .sort((a, b) => R[b.rarity][0] - R[a.rarity][0] || (b.owned - a.owned) || (a.title || '').localeCompare(b.title || ''));
      return send(res, 200, { poolSize: pool.length, owned: Object.keys(byId).length, byRarity: RARITIES.map((r) => ({ rarity: r, label: R[r][4], owned: pool.filter((i) => i.rarity === r && byId[i.id]).length, total: pool.filter((i) => i.rarity === r).length })), items }), true;
    }
    if (p === '/gacha/history') {
      const groups = {}; pulls.filter((x) => x.userId === me.id).forEach((x) => (groups[x.batchId] ||= []).push(x));
      return send(res, 200, Object.entries(groups).reverse().slice(0, 10).map(([id, l]) => ({ batchId: id, banner: l[0].banner, createdAt: l[0].createdAt, xpGained: l.reduce((a, x) => a + x.xp, 0), cards: l.map((x) => { const i = pool.find((y) => y.id === x.ideaId); return { ideaId: x.ideaId, title: i.title, icon: i.icon, rarity: x.rarity, duplicate: x.duplicate }; }) }))), true;
    }
    return false;
  };
};
