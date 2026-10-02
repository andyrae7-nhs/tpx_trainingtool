'use client';

import Link from 'next/link';
import { useEffect, useMemo, useRef, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { ErrorBox, Loading, Modal } from '@/components/ui';
import { del, get, post, put } from '@/lib/api';
import { itemTypeLabel, ukDate } from '@/lib/format';
import type { Certification, CertificationStatus, CertificationSuggestion, GapReport, KnownCertification } from '@/lib/types';

type Option = { ref: string; name: string; type: string };
type Draft = Partial<Certification> & { refList?: string[] };

export default function CertificationsPage() {
  return (
    <AppShell>
      <Certifications />
    </AppShell>
  );
}

function Certifications() {
  const [certs, setCerts] = useState<Certification[] | null>(null);
  const [options, setOptions] = useState<Option[]>([]);
  const [known, setKnown] = useState<KnownCertification[]>([]);
  const [editing, setEditing] = useState<Draft | null>(null);

  const load = () => get<Certification[]>('/certifications').then(setCerts);

  useEffect(() => {
    load();
    get<KnownCertification[]>('/certifications/known').then(setKnown).catch(() => {});
    get<GapReport>('/progression/gap')
      .then((r) => setOptions([...r.skills, ...r.behaviours, ...r.impacts].map((i) => ({ ref: i.ref, name: i.name, type: i.type }))))
      .catch(() => {});
    const params = new URLSearchParams(window.location.search);
    if (params.get('new')) setEditing({ refList: params.get('ref') ? [params.get('ref')!] : [] });
  }, []);

  // Scroll to #cert-<id> once the list has loaded (links from gap analysis and the journal).
  useEffect(() => {
    if (!certs || !window.location.hash) return;
    document.getElementById(window.location.hash.slice(1))?.scrollIntoView({ block: 'center' });
  }, [certs]);

  const earned = certs?.filter((c) => c.status === 'EARNED' && c.expiry !== 'EXPIRED').length ?? 0;
  const working = certs?.filter((c) => c.status === 'IN_PROGRESS').length ?? 0;
  const expiring = certs?.filter((c) => c.status === 'EARNED' && c.expiry === 'EXPIRING_SOON').length ?? 0;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Certifications</h1>
          <p className="subtitle">
            List the certifications you hold or are working towards. They show up next to the skills they support in your gap analysis and evidence journal.
          </p>
        </div>
        <button className="btn blue" onClick={() => setEditing({ refList: [], status: 'EARNED' })}>
          + Add certification
        </button>
      </div>

      {certs && certs.length > 0 && (
        <div className="grid grid-3" style={{ marginBottom: '1rem' }}>
          <div className="card bg-mint stat">
            <span className="num">{earned}</span>
            <span className="lbl">Current certifications</span>
          </div>
          <div className="card bg-blue stat">
            <span className="num">{working}</span>
            <span className="lbl">Working towards</span>
          </div>
          <div className={`card stat ${expiring ? 'bg-pink' : 'bg-lilac'}`}>
            <span className="num">{expiring}</span>
            <span className="lbl">Expiring in the next 90 days</span>
          </div>
        </div>
      )}

      {!certs ? (
        <Loading />
      ) : certs.length === 0 ? (
        <div className="card empty">
          <h2>No certifications yet</h2>
          <p>AWS, Azure, PRINCE2, Scrum, ISTQB, BCS… add any you hold, or one you&apos;re studying for. We&apos;ll suggest which skills each one supports.</p>
          <button className="btn blue" onClick={() => setEditing({ refList: [], status: 'EARNED' })}>
            Add my first certification
          </button>
        </div>
      ) : (
        <div className="list">
          {certs.map((c) => (
            <article key={c.id} id={`cert-${c.id}`} className={`card cert-card${c.expiry === 'EXPIRED' ? ' expired' : ''}`}>
              <div className="row between" style={{ alignItems: 'flex-start' }}>
                <div className="row" style={{ alignItems: 'flex-start', flex: 1, minWidth: 240 }}>
                  <span className="cert-icon" aria-hidden>
                    📜
                  </span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="row" style={{ gap: '0.4rem' }}>
                      <h3 style={{ margin: 0, color: 'var(--black)' }}>{c.name}</h3>
                      <StatusChip cert={c} />
                    </div>
                    <div className="cert-meta">
                      {c.issuer && <span>{c.issuer}</span>}
                      {c.issuedOn && <span>{c.status === 'IN_PROGRESS' ? 'Started' : 'Achieved'} {ukDate(c.issuedOn)}</span>}
                      {c.expiresOn && <span>{c.expiry === 'EXPIRED' ? 'Expired' : 'Valid until'} {ukDate(c.expiresOn)}</span>}
                      {c.credentialId && <span>ID: {c.credentialId}</span>}
                      {c.credentialUrl && (
                        <a href={c.credentialUrl} target="_blank" rel="noreferrer">
                          Verify ↗
                        </a>
                      )}
                    </div>
                  </div>
                </div>
                <div className="row" style={{ gap: '0.25rem' }}>
                  <button className="btn ghost small" onClick={() => setEditing({ ...c, refList: c.refs.map((r) => r.ref) })}>
                    Edit
                  </button>
                  <button
                    className="btn ghost small"
                    onClick={async () => {
                      if (window.confirm(`Delete ${c.name}?`)) {
                        await del(`/certifications/${c.id}`);
                        load();
                      }
                    }}
                  >
                    Delete
                  </button>
                </div>
              </div>
              {c.notes && <p className="small" style={{ margin: '0 0 0.5rem', whiteSpace: 'pre-wrap' }}>{c.notes}</p>}
              {c.refs.length > 0 ? (
                <div className="row" style={{ gap: '0.35rem' }}>
                  <span className="xs muted">Supports:</span>
                  {c.refs.map((r) => (
                    <span key={r.ref} className={`chip ${r.ref.startsWith('SKILL') ? 'blue' : r.ref.startsWith('BEHAVIOUR') ? 'lilac' : 'pink'}`}>
                      {r.name}
                    </span>
                  ))}
                </div>
              ) : (
                <p className="xs muted" style={{ margin: 0 }}>
                  Not linked to any skills yet, so it won&apos;t show in gap analysis.{' '}
                  <button className="linkish" onClick={() => setEditing({ ...c, refList: [] })}>
                    Link it
                  </button>
                </p>
              )}
            </article>
          ))}
        </div>
      )}

      <p className="small muted mt">
        Certifications appear next to matching items in <Link href="/progression">Gap analysis</Link> and the <Link href="/journal">Evidence journal</Link>, and in
        your evidence export.
      </p>

      {editing && (
        <CertificationForm
          initial={editing}
          options={options}
          known={known}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
    </>
  );
}

function StatusChip({ cert }: { cert: Certification }) {
  if (cert.status === 'IN_PROGRESS') return <span className="chip outline">📚 Working towards</span>;
  if (cert.expiry === 'EXPIRED') return <span className="chip">Expired</span>;
  if (cert.expiry === 'EXPIRING_SOON') return <span className="chip pink">⚠ Expiring soon</span>;
  return <span className="chip mint">✓ Current</span>;
}

function CertificationForm({
  initial,
  options,
  known,
  onClose,
  onSaved,
}: {
  initial: Draft;
  options: Option[];
  known: KnownCertification[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [name, setName] = useState(initial.name || '');
  const [issuer, setIssuer] = useState(initial.issuer || '');
  const [status, setStatus] = useState<CertificationStatus>(initial.status || 'EARNED');
  const [issuedOn, setIssuedOn] = useState(initial.issuedOn || '');
  const [expiresOn, setExpiresOn] = useState(initial.expiresOn || '');
  const [credentialId, setCredentialId] = useState(initial.credentialId || '');
  const [credentialUrl, setCredentialUrl] = useState(initial.credentialUrl || '');
  const [notes, setNotes] = useState(initial.notes || '');
  const [refs, setRefs] = useState<string[]>(initial.refList || []);
  const [suggested, setSuggested] = useState<CertificationSuggestion[]>([]);
  const [suggesting, setSuggesting] = useState(false);
  const [search, setSearch] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // Auto-apply suggestions to new certifications until the person picks items themselves.
  const touched = useRef(Boolean(initial.id) || (initial.refList?.length ?? 0) > 0);

  const suggest = async (apply: boolean) => {
    if (!name.trim()) return;
    setSuggesting(true);
    try {
      const s = await post<CertificationSuggestion[]>('/certifications/suggest', { name, issuer });
      setSuggested(s);
      if (apply) setRefs((r) => Array.from(new Set([...r, ...s.map((x) => x.ref)])));
    } catch {
      /* suggestions are optional */
    } finally {
      setSuggesting(false);
    }
  };

  useEffect(() => {
    if (!name.trim()) return;
    const t = setTimeout(() => suggest(!touched.current), 600);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [name, issuer]);

  const pickName = (value: string) => {
    setName(value);
    const match = known.find((k) => k.name === value);
    if (match && !issuer) setIssuer(match.issuer);
  };

  const toggle = (ref: string) => {
    touched.current = true;
    setRefs((r) => (r.includes(ref) ? r.filter((x) => x !== ref) : [...r, ref]));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const payload = { name, issuer, status, issuedOn: issuedOn || null, expiresOn: expiresOn || null, credentialId, credentialUrl, notes, refs };
    try {
      if (initial.id) await put(`/certifications/${initial.id}`, payload);
      else await post('/certifications', payload);
      onSaved();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const suggestedRefs = useMemo(() => new Set(suggested.map((s) => s.ref)), [suggested]);
  // Items outside your role (e.g. suggested for someone not yet onboarded) still need to be pickable.
  const allOptions = useMemo(() => {
    const extra = suggested.filter((s) => !options.some((o) => o.ref === s.ref)).map((s) => ({ ref: s.ref, name: s.name, type: s.type }));
    return [...options, ...extra];
  }, [options, suggested]);
  const groups = ['SKILL', 'BEHAVIOUR', 'IMPACT'];

  return (
    <Modal title={initial.id ? 'Edit certification' : 'Add a certification'} onClose={onClose}>
      <form onSubmit={submit}>
        <ErrorBox error={error} />
        <div className="field">
          <label htmlFor="cn">Certification</label>
          <input id="cn" type="text" list="known-certs" value={name} onChange={(e) => pickName(e.target.value)} required maxLength={200} placeholder="e.g. AWS Certified Solutions Architect - Associate" />
          <datalist id="known-certs">
            {known.map((k) => (
              <option key={k.name} value={k.name}>
                {k.issuer}
              </option>
            ))}
          </datalist>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label htmlFor="ci">Issuer</label>
            <input id="ci" type="text" value={issuer} onChange={(e) => setIssuer(e.target.value)} maxLength={120} placeholder="e.g. Amazon Web Services" />
          </div>
          <div className="field">
            <label>Status</label>
            <div className="seg" role="radiogroup" aria-label="Status">
              {(
                [
                  ['EARNED', '✓ Earned'],
                  ['IN_PROGRESS', '📚 Working towards'],
                ] as [CertificationStatus, string][]
              ).map(([k, l]) => (
                <button type="button" key={k} role="radio" aria-checked={status === k} className={status === k ? 'active' : ''} onClick={() => setStatus(k)}>
                  {l}
                </button>
              ))}
            </div>
          </div>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label htmlFor="cd">{status === 'IN_PROGRESS' ? 'Started (optional)' : 'Date achieved'}</label>
            <input id="cd" type="date" value={issuedOn} onChange={(e) => setIssuedOn(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="ce">Expires (optional)</label>
            <input id="ce" type="date" value={expiresOn} onChange={(e) => setExpiresOn(e.target.value)} min={issuedOn || undefined} />
          </div>
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label htmlFor="cid">Credential ID (optional)</label>
            <input id="cid" type="text" value={credentialId} onChange={(e) => setCredentialId(e.target.value)} maxLength={120} />
          </div>
          <div className="field">
            <label htmlFor="cu">Verification link (optional)</label>
            <input id="cu" type="url" value={credentialUrl} onChange={(e) => setCredentialUrl(e.target.value)} maxLength={500} placeholder="https://" />
          </div>
        </div>
        <div className="field">
          <label htmlFor="cno">Notes (optional)</label>
          <textarea id="cno" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000} style={{ minHeight: 60 }} placeholder="What you learned, or how you've used it on projects." />
        </div>
        <div className="field">
          <div className="row between">
            <label style={{ margin: 0 }}>Which parts of the framework does it support?</label>
            <button type="button" className="btn secondary small" onClick={() => suggest(true)} disabled={!name.trim() || suggesting}>
              {suggesting ? 'Thinking…' : '✨ Suggest'}
            </button>
          </div>
          <div className="hint" style={{ marginBottom: '0.5rem' }}>
            {suggested.length > 0 ? 'Items marked ✨ match this certification. Tap any item to add or remove it.' : 'Type the certification name and we’ll suggest matching items.'}
          </div>
          <input type="search" placeholder="Filter…" value={search} onChange={(e) => setSearch(e.target.value)} style={{ marginBottom: '0.5rem' }} aria-label="Filter framework items" />
          {groups.map((g) => {
            const list = allOptions
              .filter((o) => o.type === g && (!search || o.name.toLowerCase().includes(search.toLowerCase())))
              .sort((a, b) => Number(suggestedRefs.has(b.ref)) - Number(suggestedRefs.has(a.ref)));
            if (!list.length) return null;
            return (
              <div key={g} style={{ marginBottom: '0.5rem' }}>
                <div className="xs muted">{itemTypeLabel[g]?.toUpperCase()}</div>
                <div className="row" style={{ gap: '0.3rem' }}>
                  {list.map((o) => (
                    <button
                      type="button"
                      key={o.ref}
                      className={`chip${refs.includes(o.ref) ? ' selected' : ''}${suggestedRefs.has(o.ref) ? ' suggested' : ''}`}
                      onClick={() => toggle(o.ref)}
                      aria-pressed={refs.includes(o.ref)}
                    >
                      {refs.includes(o.ref) ? '✓ ' : suggestedRefs.has(o.ref) ? '✨ ' : ''}
                      {o.name}
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn blue" disabled={busy}>
            {busy ? 'Saving…' : 'Save certification'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
