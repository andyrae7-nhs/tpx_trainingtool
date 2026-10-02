/*
 * Lightweight in-memory mock of the TPX Grow API for front-end development without Java.
 * Mirrors the Spring Boot endpoints closely enough to click through the UI.
 *   node tools/mock-api/server.js   (listens on :8080)
 */
const http = require('http');
const path = require('path');
const fs = require('fs');
const data = (f) => JSON.parse(fs.readFileSync(path.join(__dirname, '../../backend/src/main/resources/data', f), 'utf8'));
const FW = data('framework.json');
const CAT = data('catalogue.json');
const gacha = require('./gacha')(CAT);
const G = FW.grades.map((g) => g.code);
const L = FW.skillLevels;

let seq = 100;
const users = [];
const assessments = {}; // userId -> {ref: level}
const journal = [];
const learning = [];
const posts = [];
const follows = [];
const achievements = {}; // userId -> [{code, earnedAt, seen}]
const plans = {};

const ACH = [
  ['WELCOME', 'Hello, world', 'Join TPX Grow', '👋', 'GETTING_STARTED', 10],
  ['ROLE_SET', "Know where you're going", 'Choose your role, current grade and target grade', '🧭', 'GETTING_STARTED', 20],
  ['GAP_HUNTER', 'Gap hunter', 'Generate your first personalised training plan', '🎯', 'GETTING_STARTED', 20],
  ['FIRST_EVIDENCE', 'Receipts', 'Add your first journal entry', '🧾', 'EVIDENCE', 15],
  ['EXPORTER', 'Assessment ready', 'Export your journal', '📤', 'EVIDENCE', 25],
  ['FIRST_LEARNING', 'Curious cat', 'Log your first learning', '🐈', 'LEARNING', 10],
  ['FIRST_POST', 'Hot off the press', 'Write your first post', '📰', 'SOCIAL', 10],
  ['BETTER_TOGETHER', 'Better together', 'Follow 3 colleagues', '🤝', 'SOCIAL', 15],
  ['ON_A_ROLL', 'On a roll', 'Visit 7 days in a row', '🔥', 'MASTERY', 50],
];
const levelFor = (xp) => { let l = 1; while (50 * (l + 1) * l <= xp) l++; return l; };
const TITLES = ['Seedling', 'Sprout', 'Sapling', 'Grower', 'In bloom', 'Branching out', 'Deep roots', 'Evergreen', 'Mighty oak', 'Forest'];
const levelInfo = (xp) => { const l = levelFor(xp); return { level: l, title: TITLES[Math.min(l, 10) - 1], xp, currentLevelXp: 50 * l * (l - 1), nextLevelXp: 50 * (l + 1) * l }; };
const role = (id) => FW.roles.find((r) => r.id === id);
const skill = (id) => FW.skills.find((s) => s.id === id);
const gname = (c) => FW.grades.find((g) => g.code === c)?.name;
const certs = require('./certifications')({ FW, role: (id) => role(id), skill: (id) => skill(id), refName: (r) => refName(r) });
const refName = (ref) => { const [t, id] = ref.split(':'); const src = t === 'SKILL' ? FW.skills : t === 'BEHAVIOUR' ? FW.behaviours : FW.impacts; return src.find((x) => x.id === id)?.name; };

function summary(u) { const r = role(u.roleId); return { id: u.id, displayName: u.displayName, avatarColor: u.avatarColor, roleName: r?.name, capability: r?.capability, currentGradeName: gname(u.currentGrade), level: levelFor(u.xp) }; }
function view(u, viewer) {
  const r = role(u.roleId);
  return { id: u.id, email: viewer === u.id ? u.email : undefined, displayName: u.displayName, bio: u.bio, avatarColor: u.avatarColor, roleId: u.roleId, roleName: r?.name, capability: r?.capability, practice: r?.practice,
    currentGrade: u.currentGrade, currentGradeName: gname(u.currentGrade), targetGrade: u.targetGrade, targetGradeName: gname(u.targetGrade), onboarded: !!(u.roleId && u.currentGrade && u.targetGrade),
    level: levelInfo(u.xp), streakDays: u.streakDays, followers: follows.filter((f) => f[1] === u.id).length, following: follows.filter((f) => f[0] === u.id).length,
    isFollowing: viewer === u.id ? undefined : follows.some((f) => f[0] === viewer && f[1] === u.id), isMe: viewer === u.id };
}
function award(u, code) {
  const list = (achievements[u.id] ||= []);
  if (list.some((a) => a.code === code)) return;
  const a = ACH.find((x) => x[0] === code);
  list.push({ code, earnedAt: new Date().toISOString(), seen: false });
  u.xp += a[5];
  if (code !== 'WELCOME') posts.unshift({ id: ++seq, authorId: u.id, content: `unlocked the ${a[3]} ${a[1]} badge: ${a[2].toLowerCase()}`, kind: 'ACHIEVEMENT', createdAt: new Date().toISOString(), likes: [], comments: [] });
}
function expected(rs, g) { if (g in rs.expected) return rs.expected[g]; return Object.values(rs.expected).filter(Boolean).pop() || null; }
function report(u, target) { return certs.attach(reportBase(u, target), u.id); }
function reportBase(u, target) {
  const r = role(u.roleId); const cur = u.currentGrade; const tgt = target || u.targetGrade; const mine = assessments[u.id] || {};
  const ev = {}; journal.filter((j) => j.userId === u.id).forEach((j) => j.refs.forEach((x) => (ev[x] = (ev[x] || 0) + 1)));
  const skills = r.skills.map((rs) => { const s = skill(rs.skillId); const ce = expected(rs, cur), te = expected(rs, tgt); if (!ce && !te) return null; const ref = 'SKILL:' + s.id; const self = mine[ref] || ce;
    const gap = te ? Math.max(0, L.indexOf(te) - L.indexOf(self)) : 0;
    return { ref, type: 'SKILL', id: s.id, name: s.name, definition: s.definition, currentExpected: ce, targetExpected: te, selfLevel: self, selfAssessed: !!mine[ref], gap, status: !te ? 'NOT_REQUIRED' : gap ? 'GAP' : 'MET', selfDescriptor: s.levels[self], targetDescriptor: s.levels[te], evidenceCount: ev[ref] || 0 }; }).filter(Boolean);
  const graded = (items, type) => items.map((g) => { const ref = type + ':' + g.id; const self = mine[ref] || cur; const tt = g.levels[tgt]; let gap = tt ? Math.max(0, G.indexOf(tgt) - G.indexOf(self)) : 0; if (gap && g.levels[self] === tt) gap = 0;
    return { ref, type, id: g.id, name: g.name, definition: g.definition, currentExpected: gname(cur), targetExpected: gname(tgt), selfLevel: self, selfAssessed: !!mine[ref], gap, status: !tt ? 'NOT_REQUIRED' : gap ? 'GAP' : 'MET', selfDescriptor: g.levels[self], targetDescriptor: tt, evidenceCount: ev[ref] || 0 }; });
  const behaviours = graded(FW.behaviours, 'BEHAVIOUR'), impacts = graded(FW.impacts, 'IMPACT');
  const all = [...skills, ...behaviours, ...impacts]; const req = all.filter((i) => i.status !== 'NOT_REQUIRED').length; const met = all.filter((i) => i.status === 'MET').length;
  const cg = (l) => l.filter((i) => i.status === 'GAP').length;
  return { roleId: r.id, roleName: r.name, capability: r.capability, practice: r.practice, currentGrade: cur, currentGradeName: gname(cur), targetGrade: tgt, targetGradeName: gname(tgt), skillScale: L, gradeScale: FW.grades, skills, behaviours, impacts,
    summary: { required: req, met, gaps: req - met, readinessPercent: req ? Math.round((100 * met) / req) : 100, assessed: all.filter((i) => i.selfAssessed).length, totalItems: all.length, skillGaps: cg(skills), behaviourGaps: cg(behaviours), impactGaps: cg(impacts) } };
}
function postView(p, me) { return { id: p.id, content: p.content, kind: p.kind, createdAt: p.createdAt, author: summary(users.find((u) => u.id === p.authorId)), likeCount: p.likes.length, likedByMe: p.likes.includes(me), comments: p.comments.map((c) => ({ ...c, author: summary(users.find((u) => u.id === c.authorId)), mine: c.authorId === me })), mine: p.authorId === me }; }
function jview(j) { return { ...j, refs: j.refs.map((r) => ({ ref: r, name: refName(r) })) }; }

function mkUser(name, email, roleId, cur, tgt, colour, xp) { const u = { id: ++seq, displayName: name, email, password: 'password123', roleId, currentGrade: cur, targetGrade: tgt, avatarColor: colour, xp, streakDays: Math.floor(Math.random() * 9), bio: '' }; users.push(u); return u; }
const priya = mkUser('Priya Sharma', 'priya.demo@example.com', 'software-engineer', 'G9', 'G10', '#c8e9ff', 1480);
const tom = mkUser('Tom Okafor', 'tom.demo@example.com', 'delivery-manager', 'G8', 'G9', '#ffcfca', 960);
const sophie = mkUser('Sophie Green', 'sophie.demo@example.com', 'service-designer', 'G9', 'G10', '#cafce5', 1210);
mkUser('Dev Patel', 'dev.demo@example.com', 'cloud-engineer', 'G10', 'G11', '#e7d2ff', 1830);
const alex = mkUser('Alex Morgan', 'demo@example.com', 'technology-consultant', 'G9', 'G10', '#cafce5', 40);
posts.push({ id: ++seq, authorId: tom.id, content: 'Ran my first fully remote retro with the new client team today. Anyone got favourite retro formats?', kind: 'GENERAL', createdAt: new Date(Date.now() - 7200e3).toISOString(), likes: [priya.id, sophie.id], comments: [{ id: ++seq, authorId: sophie.id, content: "Try 'Start, stop, continue' with dot voting!", createdAt: new Date().toISOString() }] });
posts.push({ id: ++seq, authorId: sophie.id, content: "reviewed 'Workshop design and facilitation' ★★★★★: Loved the practical bits.", kind: 'LEARNING', createdAt: new Date(Date.now() - 86400e3).toISOString(), likes: [tom.id], comments: [] });
learning.push({ id: ++seq, userId: sophie.id, type: 'PROGRAMME', title: 'Workshop design and facilitation', provider: 'TPXimpact', catalogueId: 'workshop-design-and-facilitation', status: 'COMPLETED', rating: 5, review: 'Loved the practical bits. Used the silent start with a client the next week.', shared: true, refs: [], createdAt: new Date().toISOString(), completedOn: '2026-09-10' });
follows.push([alex.id, priya.id], [alex.id, tom.id]);
users.forEach((u) => award(u, 'WELCOME'));
achievements[alex.id].forEach((a) => (a.seen = true));
journal.push({ id: ++seq, userId: alex.id, title: 'Led the architecture options workshop', body: "Facilitated a half-day workshop with the client's CTO to compare hosting options.", impact: 'Client signed off the alpha plan a week early.', entryDate: '2026-08-20', refs: ['SKILL:solution-architecture', 'BEHAVIOUR:communicating-and-collaborating'], createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() });

function plan(u) {
  const rep = report(u); const gaps = [...rep.skills, ...rep.behaviours, ...rep.impacts].filter((g) => g.status === 'GAP').sort((a, b) => b.gap - a.gap).slice(0, 8);
  const items = gaps.map((g) => { const words = g.name.toLowerCase().split(/[^a-z]+/).filter((w) => w.length > 3);
    const res = CAT.map((r) => ({ r, s: r.tags.filter((t) => words.some((w) => t.includes(w.slice(0, 5)))).length + (r.title.toLowerCase().split(' ').some((w) => words.includes(w)) ? 1 : 0) })).filter((x) => x.s > 0).sort((a, b) => b.s - a.s).slice(0, 3);
    return { gap: { ref: g.ref, type: g.type, name: g.name, from: g.type === 'SKILL' ? g.selfLevel : gname(g.selfLevel), to: g.targetExpected, gap: g.gap, targetDescriptor: g.targetDescriptor }, suggestions: res.map((x) => ({ resource: x.r, reason: `Covers ${x.r.tags.slice(0, 3).join(', ')}, which supports moving towards ${g.targetExpected} in ${g.name}.` })), action: `Pick one bullet from the ${g.targetExpected} description and try it on your project this month.` }; });
  return { generatedBy: 'rules', generatedAt: new Date().toISOString(), roleName: rep.roleName, currentGradeName: rep.currentGradeName, targetGradeName: rep.targetGradeName, summary: `You have ${rep.summary.gaps} gaps to close to reach ${rep.targetGradeName}. Start with ${gaps[0]?.name}.`, items, quickWins: ['Book 30 minutes with your line manager.', 'Add one journal entry this week.', 'Share a review.'] };
}

const tokens = {};
function send(res, code, body) { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(body === undefined ? '' : JSON.stringify(body)); }
http.createServer((req, res) => {
  let raw = ''; req.on('data', (c) => (raw += c)); req.on('end', () => {
    const url = new URL(req.url, 'http://x'); const p = url.pathname.replace(/^\/api/, ''); const m = req.method; const body = raw ? JSON.parse(raw) : {};
    const q = (k) => url.searchParams.get(k);
    const auth = (req.headers.authorization || '').replace('Bearer ', ''); const me = users.find((u) => u.id === tokens[auth]);
    try {
      if (p === '/framework/roles') return send(res, 200, FW.roles.map((r) => ({ id: r.id, name: r.name, capability: r.capability, practice: r.practice, skillCount: r.skills.length })));
      if (p === '/framework/grades') return send(res, 200, FW.grades);
      if (p === '/catalogue') return send(res, 200, CAT);
      if (p === '/auth/login' || p === '/auth/register') {
        let u = users.find((x) => x.email === body.email);
        if (p === '/auth/register') { if (u) return send(res, 409, { message: 'An account with that email already exists' }); u = mkUser(body.displayName, body.email, null, null, null, '#c8e9ff', 10); u.password = body.password; award(u, 'WELCOME'); }
        else if (!u || u.password !== body.password) return send(res, 401, { message: 'Email or password is incorrect' });
        const t = 'tok' + Math.random(); tokens[t] = u.id; return send(res, p.endsWith('register') ? 201 : 200, { token: t, user: view(u, u.id) });
      }
      if (!me) return send(res, 401, { message: 'Please sign in' });
      if (p === '/me' && m === 'GET') return send(res, 200, view(me, me.id));
      if (p === '/me' && m === 'PUT') { Object.entries(body).forEach(([k, v]) => v != null && v !== '' && (me[k] = v)); if (me.roleId && me.currentGrade && me.targetGrade) award(me, 'ROLE_SET'); return send(res, 200, view(me, me.id)); }
      if (p === '/users') return send(res, 200, users.filter((u) => !q('q') || u.displayName.toLowerCase().includes(q('q').toLowerCase())).map((u) => view(u, me.id)));
      let mm;
      if ((mm = p.match(/^\/users\/(\d+)$/))) return send(res, 200, view(users.find((u) => u.id === +mm[1]), me.id));
      if (p === '/progression/gap') return send(res, 200, report(me, q('targetGrade')));
      if (p === '/progression/assessments' && m === 'PUT') { const a = (assessments[me.id] ||= {}); body.forEach((i) => (a[i.itemType + ':' + i.itemId] = i.level)); me.xp += 5; return send(res, 200, report(me)); }
      if (p === '/recommendations' && m === 'POST') { plans[me.id] = plan(me); award(me, 'GAP_HUNTER'); me.xp += 10; return send(res, 200, plans[me.id]); }
      if (p === '/recommendations/latest') return plans[me.id] ? send(res, 200, plans[me.id]) : send(res, 204);
      if (p === '/ai/status') return send(res, 200, { enabled: false, model: 'built-in rules' });
      if (p === '/pip/tip') return send(res, 200, { tip: 'Tip: write journal entries while they are fresh!' });
      if (p === '/pip/chat') return send(res, 200, { reply: `You asked: "${body.message}". (Mock Pip) Your biggest gap is ${report(me).skills.find((s) => s.status === 'GAP')?.name || 'none'}.`, source: 'pip', suggestions: ["What's my biggest gap?", 'How do I write good evidence?'] });
      if (p === '/journal' && m === 'GET') return send(res, 200, journal.filter((j) => j.userId === me.id).sort((a, b) => b.entryDate.localeCompare(a.entryDate)).map(jview));
      if (p === '/journal' && m === 'POST') { const j = { id: ++seq, userId: me.id, ...body, entryDate: body.entryDate || new Date().toISOString().slice(0, 10), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }; journal.push(j); me.xp += 15; award(me, 'FIRST_EVIDENCE'); return send(res, 201, jview(j)); }
      if ((mm = p.match(/^\/journal\/(\d+)$/))) { const i = journal.findIndex((j) => j.id === +mm[1]); if (m === 'DELETE') { journal.splice(i, 1); return send(res, 204); } Object.assign(journal[i], body); return send(res, 200, jview(journal[i])); }
      if (p === '/journal/export') { const mine = journal.filter((j) => j.userId === me.id); const groups = {}; mine.forEach((j) => j.refs.forEach((r) => (groups[r] ||= []).push(j))); const cx = certs.exportLines(me.id); Object.keys(cx.byRef).forEach((r) => (groups[r] ||= []));
        let t = `PROGRESSION ASSESSMENT EVIDENCE - ${me.displayName}\n\n`; Object.entries(groups).forEach(([r, js]) => { t += refName(r) + '\n'; (cx.byRef[r] || []).forEach((l) => (t += l)); js.forEach((j) => (t += `• ${j.entryDate} - ${j.title}: ${j.body || ''} Impact: ${j.impact || ''}\n`)); t += '\n'; });
        t += cx.summary; award(me, 'EXPORTER'); return send(res, 200, { text: t, entryCount: mine.length, generatedBy: 'plain' }); }
      if (p === '/learning' && m === 'GET') return send(res, 200, learning.filter((l) => l.userId === me.id));
      if (p === '/learning' && m === 'POST') { const c = body.catalogueId && CAT.find((r) => r.id === body.catalogueId); const l = { id: ++seq, userId: me.id, type: c ? (['COURSE', 'BOOK', 'EVENT', 'PROGRAMME'].includes(c.type) ? c.type : 'OTHER') : body.type, title: c ? c.title : body.title, provider: c?.provider || body.provider, url: c?.url || body.url, catalogueId: c?.id, status: body.status || 'PLANNED', shared: false, refs: [], createdAt: new Date().toISOString() }; learning.unshift(l); award(me, 'FIRST_LEARNING'); return send(res, 201, l); }
      if ((mm = p.match(/^\/learning\/(\d+)$/))) { const i = learning.findIndex((l) => l.id === +mm[1]); if (m === 'DELETE') { learning.splice(i, 1); return send(res, 204); } Object.entries(body).forEach(([k, v]) => v !== undefined && (learning[i][k] = v)); if (body.status === 'COMPLETED') { learning[i].completedOn = new Date().toISOString().slice(0, 10); me.xp += 30; } return send(res, 200, learning[i]); }
      if (p === '/learning/reviews') return send(res, 200, learning.filter((l) => l.shared && l.review).map((l) => ({ ...l, author: summary(users.find((u) => u.id === l.userId)) })));
      if (p === '/social/feed') { const sc = q('scope'); let list = posts; if (sc === 'following') { const ids = [me.id, ...follows.filter((f) => f[0] === me.id).map((f) => f[1])]; list = posts.filter((x) => ids.includes(x.authorId)); } if (sc === 'user') list = posts.filter((x) => x.authorId === +q('userId')); return send(res, 200, list.slice(0, +(q('size') || 30)).map((x) => postView(x, me.id))); }
      if (p === '/social/posts') { const x = { id: ++seq, authorId: me.id, content: body.content, kind: 'GENERAL', createdAt: new Date().toISOString(), likes: [], comments: [] }; posts.unshift(x); award(me, 'FIRST_POST'); return send(res, 201, postView(x, me.id)); }
      if ((mm = p.match(/^\/social\/posts\/(\d+)(\/like|\/comments)?$/))) { const x = posts.find((y) => y.id === +mm[1]); if (mm[2] === '/like') x.likes = x.likes.includes(me.id) ? x.likes.filter((i) => i !== me.id) : [...x.likes, me.id]; else if (mm[2]) x.comments.push({ id: ++seq, authorId: me.id, content: body.content, createdAt: new Date().toISOString() }); else { posts.splice(posts.indexOf(x), 1); return send(res, 204); } return send(res, 200, postView(x, me.id)); }
      if ((mm = p.match(/^\/social\/follow\/(\d+)$/))) { const id = +mm[1]; const i = follows.findIndex((f) => f[0] === me.id && f[1] === id); if (m === 'POST' && i < 0) follows.push([me.id, id]); if (m === 'DELETE' && i >= 0) follows.splice(i, 1); if (follows.filter((f) => f[0] === me.id).length >= 3) award(me, 'BETTER_TOGETHER'); return send(res, 200, view(users.find((u) => u.id === id), me.id)); }
      if (p === '/achievements') return send(res, 200, { level: levelInfo(me.xp), streakDays: me.streakDays, achievements: ACH.map((a) => { const e = (achievements[me.id] || []).find((x) => x.code === a[0]); return { code: a[0], title: a[1], description: a[2], icon: a[3], category: a[4], bonusXp: a[5], earned: !!e, earnedAt: e?.earnedAt }; }) });
      if (p === '/achievements/unseen') { const list = (achievements[me.id] || []).filter((a) => !a.seen); list.forEach((a) => (a.seen = true)); return send(res, 200, list.map((e) => { const a = ACH.find((x) => x[0] === e.code); return { code: a[0], title: a[1], description: a[2], icon: a[3], category: a[4], bonusXp: a[5], earned: true, earnedAt: e.earnedAt }; })); }
      if ((mm = p.match(/^\/achievements\/users\/(\d+)$/))) return send(res, 200, (achievements[+mm[1]] || []).map((e) => { const a = ACH.find((x) => x[0] === e.code); return { code: a[0], title: a[1], description: a[2], icon: a[3], category: a[4], bonusXp: a[5], earned: true }; }));
      if (p === '/leaderboard') return send(res, 200, [...users].sort((a, b) => b.xp - a.xp).map((u, i) => ({ rank: i + 1, userId: u.id, displayName: u.displayName, avatarColor: u.avatarColor, roleName: role(u.roleId)?.name, xp: u.xp, periodXp: u.xp, level: levelFor(u.xp), levelTitle: levelInfo(u.xp).title, badges: (achievements[u.id] || []).length, streakDays: u.streakDays, isMe: u.id === me.id })));
      if (gacha(p, m, body, me, send, res)) return;
      if (certs.handle(p, m, body, me, send, res)) return;
      send(res, 404, { message: 'Not found in mock: ' + m + ' ' + p });
    } catch (e) { console.error(e); send(res, 500, { message: e.message }); }
  });
}).listen(8080, () => console.log('Mock TPX Grow API on http://localhost:8080 (demo@example.com / password123)'));
