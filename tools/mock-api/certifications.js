/* In-memory mock of /api/certifications, mirroring the Spring Boot CertificationController. */
const KNOWN = [
  ['AWS Certified Solutions Architect - Associate', 'Amazon Web Services'], ['AWS Certified Developer - Associate', 'Amazon Web Services'],
  ['Microsoft Certified: Azure Fundamentals (AZ-900)', 'Microsoft'], ['HashiCorp Certified: Terraform Associate', 'HashiCorp'],
  ['CompTIA Security+', 'CompTIA'], ['ISTQB Certified Tester Foundation Level', 'ISTQB'], ['ITIL 4 Foundation', 'PeopleCert'],
  ['PRINCE2 Practitioner', 'PeopleCert'], ['Professional Scrum Master I (PSM I)', 'Scrum.org'], ['Professional Scrum Product Owner I (PSPO I)', 'Scrum.org'],
  ['BCS Foundation Certificate in Business Analysis', 'BCS'], ['IAAP Certified Professional in Accessibility Core Competencies (CPACC)', 'IAAP'],
].map(([name, issuer]) => ({ name, issuer }));
// Certification keyword -> words to look for in skill names (a simplified version of the real matcher).
const RULES = [
  [/aws|azure|google cloud|cloud/, ['cloud', 'infrastructure', 'availability']], [/architect/, ['architecture', 'systems design']],
  [/developer|terraform/, ['programming', 'coding', 'infrastructure as code']], [/security|cissp/, ['security']],
  [/istqb|test/, ['test']], [/itil/, ['service', 'problem management']], [/prince2|pmp|project/, ['project', 'lifecycle', 'momentum']],
  [/scrum|agile|safe/, ['agile', 'lifecycle']], [/product owner|pspo/, ['product', 'value release', 'outcomes']],
  [/business analysis|cbap/, ['business analysis', 'business modelling', 'requirements']], [/accessib|cpacc/, ['accessibility']], [/data/, ['data']],
];

module.exports = function certifications({ FW, role, skill, refName }) {
  const certs = [];
  let seq = 5000;
  const today = () => new Date().toISOString().slice(0, 10);
  const expiry = (c) => {
    if (!c.expiresOn) return 'NO_EXPIRY';
    if (c.expiresOn < today()) return 'EXPIRED';
    const soon = new Date(Date.now() + 90 * 864e5).toISOString().slice(0, 10);
    return c.expiresOn <= soon ? 'EXPIRING_SOON' : 'ACTIVE';
  };
  const view = (c) => ({ ...c, expiry: expiry(c), refs: c.refs.map((r) => ({ ref: r, name: refName(r) || r })) });
  const badge = (c) => ({ id: c.id, name: c.name, issuer: c.issuer, status: c.status, expiry: expiry(c), expiresOn: c.expiresOn });
  const apply = (c, b) => Object.assign(c, {
    name: (b.name || '').trim(), issuer: b.issuer || undefined, status: b.status || 'EARNED', credentialId: b.credentialId || undefined,
    credentialUrl: b.credentialUrl || undefined, issuedOn: b.issuedOn || undefined, expiresOn: b.expiresOn || undefined, notes: b.notes || undefined,
    refs: [...new Set((b.refs || []).filter((r) => refName(r)))], updatedAt: new Date().toISOString(),
  });
  const order = (a, b) => (expiry(a) === 'EXPIRED') - (expiry(b) === 'EXPIRED') || (b.status === 'IN_PROGRESS') - (a.status === 'IN_PROGRESS') || (b.issuedOn || '').localeCompare(a.issuedOn || '');

  function suggest(me, name, issuer) {
    const text = `${name || ''} ${issuer || ''}`.toLowerCase();
    const words = RULES.filter(([re]) => re.test(text)).flatMap(([, w]) => w);
    const r = role(me.roleId);
    const pool = r ? r.skills.map((rs) => skill(rs.skillId)) : FW.skills;
    const out = words.length ? pool.filter((s) => words.some((w) => s.name.toLowerCase().includes(w))).slice(0, 4).map((s) => ({ ref: 'SKILL:' + s.id, name: s.name, type: 'SKILL', score: 3 })) : [];
    out.push({ ref: 'BEHAVIOUR:developing-your-craft', name: 'Developing your craft', type: 'BEHAVIOUR', score: 1 });
    return out;
  }

  return {
    /** Adds `certifications` to every item in a gap report. */
    attach(report, userId) {
      const mine = certs.filter((c) => c.userId === userId);
      [...report.skills, ...report.behaviours, ...report.impacts].forEach((i) => (i.certifications = mine.filter((c) => c.refs.includes(i.ref)).map(badge)));
      return report;
    },
    /** Earned certifications as export lines, per ref and as a summary section. */
    exportLines(userId) {
      const mine = certs.filter((c) => c.userId === userId && c.status === 'EARNED');
      const line = (c) => `• Certification: ${c.name}${c.issuer ? ` (${c.issuer})` : ''}${c.issuedOn ? `, achieved ${c.issuedOn}` : ''}\n`;
      const byRef = {};
      mine.forEach((c) => c.refs.forEach((r) => (byRef[r] ||= []).push(line(c))));
      return { byRef, summary: mine.length ? '== CERTIFICATIONS ==\n\n' + mine.map(line).join('') + '\n' : '' };
    },
    handle(p, m, body, me, send, res) {
      if (!p.startsWith('/certifications')) return false;
      let mm;
      if (p === '/certifications' && m === 'GET') return send(res, 200, certs.filter((c) => c.userId === me.id).sort(order).map(view)), true;
      if (p === '/certifications/known') return send(res, 200, KNOWN), true;
      if (p === '/certifications/suggest') return send(res, 200, suggest(me, body.name, body.issuer)), true;
      if (p === '/certifications' && m === 'POST') {
        if (!body.name) return send(res, 400, { message: 'name: must not be blank' }), true;
        const c = apply({ id: ++seq, userId: me.id, createdAt: new Date().toISOString() }, body); certs.push(c);
        return send(res, 201, view(c)), true;
      }
      if ((mm = p.match(/^\/certifications\/(\d+)$/))) {
        const i = certs.findIndex((c) => c.id === +mm[1] && c.userId === me.id);
        if (i < 0) return send(res, 404, { message: 'Certification not found' }), true;
        if (m === 'DELETE') { certs.splice(i, 1); return send(res, 204), true; }
        return send(res, 200, view(apply(certs[i], body))), true;
      }
      return false;
    },
  };
};
