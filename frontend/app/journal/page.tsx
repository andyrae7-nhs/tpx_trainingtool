'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { AppShell } from '@/components/AppShell';
import { CertBadges } from '@/components/CertBadges';
import { ErrorBox, Loading, Modal } from '@/components/ui';
import { del, get, post, put } from '@/lib/api';
import { itemTypeLabel, ukDate } from '@/lib/format';
import type { Certification, CertificationBadge, GapReport, JournalEntry } from '@/lib/types';

type Option = { ref: string; name: string; type: string };

const badge = (c: Certification): CertificationBadge => ({ id: c.id, name: c.name, issuer: c.issuer, status: c.status, expiry: c.expiry, expiresOn: c.expiresOn });

/** Certifications tagged to any of the given refs, without duplicates. */
function certsFor(byRef: Map<string, CertificationBadge[]>, refs: string[]): CertificationBadge[] {
  const seen = new Map<number, CertificationBadge>();
  refs.forEach((r) => (byRef.get(r) || []).forEach((c) => seen.set(c.id, c)));
  return [...seen.values()];
}

export default function JournalPage() {
  return (
    <AppShell>
      <Journal />
    </AppShell>
  );
}

function Journal() {
  const [entries, setEntries] = useState<JournalEntry[] | null>(null);
  const [options, setOptions] = useState<Option[]>([]);
  const [editing, setEditing] = useState<Partial<JournalEntry> & { refList?: string[] } | null>(null);
  const [exporting, setExporting] = useState(false);
  const [filter, setFilter] = useState('');
  const [certs, setCerts] = useState<Certification[]>([]);

  const load = () => get<JournalEntry[]>('/journal').then(setEntries);

  useEffect(() => {
    load();
    get<Certification[]>('/certifications').then(setCerts).catch(() => {});
    get<GapReport>('/progression/gap')
      .then((r) => setOptions([...r.skills, ...r.behaviours, ...r.impacts].map((i) => ({ ref: i.ref, name: i.name, type: i.type }))))
      .catch(() => {});
    const params = new URLSearchParams(window.location.search);
    if (params.get('new')) setEditing({ refList: params.get('ref') ? [params.get('ref')!] : [] });
  }, []);

  const shown = useMemo(() => (entries || []).filter((e) => !filter || e.refs.some((r) => r.ref === filter)), [entries, filter]);
  const certsByRef = useMemo(() => {
    const m = new Map<string, CertificationBadge[]>();
    certs.forEach((c) => c.refs.forEach((r) => m.set(r.ref, [...(m.get(r.ref) || []), badge(c)])));
    return m;
  }, [certs]);
  const usedRefs = useMemo(() => {
    const m = new Map<string, string>();
    (entries || []).forEach((e) => e.refs.forEach((r) => m.set(r.ref, r.name)));
    certs.forEach((c) => c.refs.forEach((r) => m.set(r.ref, r.name)));
    return [...m.entries()];
  }, [entries, certs]);
  const filterName = usedRefs.find(([ref]) => ref === filter)?.[1];
  const filterCerts = filter ? certsFor(certsByRef, [filter]) : [];
  const currentCerts = certs.filter((c) => c.status === 'EARNED' && c.expiry !== 'EXPIRED').map(badge);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Evidence journal</h1>
          <p className="subtitle">Capture what you did and the difference it made, as it happens. Export it all when assessment time comes.</p>
        </div>
        <div className="row">
          <button className="btn secondary" onClick={() => setExporting(true)} disabled={!entries?.length}>
            📤 Export for assessment
          </button>
          <button className="btn blue" onClick={() => setEditing({ refList: [] })}>
            + New entry
          </button>
        </div>
      </div>

      {usedRefs.length > 0 && (
        <div className="row" style={{ gap: '0.35rem', marginBottom: '1rem' }}>
          <button className={`chip${!filter ? ' selected' : ''}`} onClick={() => setFilter('')}>
            All
          </button>
          {usedRefs.map(([ref, name]) => (
            <button key={ref} className={`chip${filter === ref ? ' selected' : ''}`} onClick={() => setFilter(ref)}>
              {name}
            </button>
          ))}
        </div>
      )}

      {filter && filterCerts.length > 0 && (
        <div className="card flat cert-strip" style={{ marginBottom: '1rem' }}>
          <strong className="small">📜 Certifications for {filterName}:</strong>
          <CertBadges certs={filterCerts} />
        </div>
      )}
      {!filter && certs.length > 0 && (
        <div className="card flat cert-strip" style={{ marginBottom: '1rem' }}>
          <strong className="small">
            📜 {currentCerts.length} current certification{currentCerts.length === 1 ? '' : 's'} back up your evidence:
          </strong>
          <CertBadges certs={currentCerts} compact />
          <Link href="/certifications" className="small">
            Manage
          </Link>
        </div>
      )}

      {!entries ? (
        <Loading />
      ) : shown.length === 0 ? (
        <div className="card empty">
          <h2>No evidence yet</h2>
          <p>Did something you&apos;re proud of this week? Led a workshop, fixed a tricky bug, got kind feedback from a client? Write it down.</p>
          <button className="btn blue" onClick={() => setEditing({ refList: [] })}>
            Write my first entry
          </button>
        </div>
      ) : (
        <div className="list">
          {shown.map((e, i) => (
            <article key={e.id} className="card">
              <div className="row between" style={{ alignItems: 'flex-start' }}>
                <div>
                  <div className="xs muted">{ukDate(e.entryDate)}</div>
                  <h3 style={{ color: 'var(--black)', margin: '0.2rem 0 0.4rem' }}>{e.title}</h3>
                </div>
                <div className="row" style={{ gap: '0.25rem' }}>
                  <button className="btn ghost small" onClick={() => setEditing({ ...e, refList: e.refs.map((r) => r.ref) })}>
                    Edit
                  </button>
                  <button
                    className="btn ghost small"
                    onClick={async () => {
                      if (window.confirm('Delete this entry?')) {
                        await del(`/journal/${e.id}`);
                        load();
                      }
                    }}
                  >
                    Delete
                  </button>
                </div>
              </div>
              {e.body && <p style={{ whiteSpace: 'pre-wrap', margin: '0 0 0.5rem' }}>{e.body}</p>}
              {e.impact && (
                <p className={`small ${['bg-mint', 'bg-blue', 'bg-lilac'][i % 3]}`} style={{ padding: '0.5rem 0.7rem', borderRadius: 8, margin: '0 0 0.5rem' }}>
                  <strong>Impact: </strong>
                  {e.impact}
                </p>
              )}
              <div className="row" style={{ gap: '0.35rem' }}>
                {e.refs.map((r) => (
                  <span key={r.ref} className={`chip ${r.ref.startsWith('SKILL') ? 'blue' : r.ref.startsWith('BEHAVIOUR') ? 'lilac' : 'pink'}`}>
                    {r.name}
                  </span>
                ))}
                <CertBadges certs={certsFor(certsByRef, e.refs.map((r) => r.ref))} compact />
              </div>
            </article>
          ))}
        </div>
      )}

      {editing && (
        <EntryForm
          initial={editing}
          options={options}
          certsByRef={certsByRef}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
      {exporting && <ExportModal onClose={() => setExporting(false)} />}
    </>
  );
}

function EntryForm({
  initial,
  options,
  certsByRef,
  onClose,
  onSaved,
}: {
  initial: Partial<JournalEntry> & { refList?: string[] };
  options: Option[];
  certsByRef: Map<string, CertificationBadge[]>;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [title, setTitle] = useState(initial.title || '');
  const [date, setDate] = useState(initial.entryDate || new Date().toISOString().slice(0, 10));
  const [body, setBody] = useState(initial.body || '');
  const [impact, setImpact] = useState(initial.impact || '');
  const [refs, setRefs] = useState<string[]>(initial.refList || []);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');

  const toggle = (ref: string) => setRefs((r) => (r.includes(ref) ? r.filter((x) => x !== ref) : [...r, ref]));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const payload = { title, entryDate: date, body, impact, refs };
    try {
      if (initial.id) await put(`/journal/${initial.id}`, payload);
      else await post('/journal', payload);
      onSaved();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const groups = ['SKILL', 'BEHAVIOUR', 'IMPACT'];
  return (
    <Modal title={initial.id ? 'Edit entry' : 'New evidence'} onClose={onClose}>
      <form onSubmit={submit}>
        <ErrorBox error={error} />
        <div className="grid grid-2">
          <div className="field">
            <label htmlFor="t">Title</label>
            <input id="t" type="text" value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} placeholder="e.g. Ran the sprint review with the client" />
          </div>
          <div className="field">
            <label htmlFor="d">Date</label>
            <input id="d" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="b">What happened and what did you do?</label>
          <textarea id="b" value={body} onChange={(e) => setBody(e.target.value)} maxLength={5000} placeholder="The situation, and the part you played." />
        </div>
        <div className="field">
          <label htmlFor="i">What was the impact?</label>
          <textarea id="i" value={impact} onChange={(e) => setImpact(e.target.value)} maxLength={3000} style={{ minHeight: 70 }} placeholder="What changed because of it? Numbers and feedback are great." />
        </div>
        <div className="field">
          <label>Which parts of the framework does this show?</label>
          <input type="search" placeholder="Filter…" value={search} onChange={(e) => setSearch(e.target.value)} style={{ marginBottom: '0.5rem' }} aria-label="Filter framework items" />
          {groups.map((g) => {
            const list = options.filter((o) => o.type === g && (!search || o.name.toLowerCase().includes(search.toLowerCase())));
            if (!list.length) return null;
            return (
              <div key={g} style={{ marginBottom: '0.5rem' }}>
                <div className="xs muted">{itemTypeLabel[g]?.toUpperCase()}</div>
                <div className="row" style={{ gap: '0.3rem' }}>
                  {list.map((o) => (
                    <button type="button" key={o.ref} className={`chip${refs.includes(o.ref) ? ' selected' : ''}`} onClick={() => toggle(o.ref)} aria-pressed={refs.includes(o.ref)}>
                      {refs.includes(o.ref) ? '✓ ' : ''}
                      {o.name}
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
          {certsFor(certsByRef, refs).length > 0 && (
            <div className="cert-strip mt">
              <span className="xs muted">Your certifications for these items:</span>
              <CertBadges certs={certsFor(certsByRef, refs)} compact />
            </div>
          )}
        </div>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn blue" disabled={busy}>
            {busy ? 'Saving…' : 'Save entry'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function ExportModal({ onClose }: { onClose: () => void }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [polish, setPolish] = useState(false);
  const [text, setText] = useState<string | null>(null);
  const [by, setBy] = useState('');
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);
  const [ai, setAi] = useState(false);

  useEffect(() => {
    get<{ enabled: boolean }>('/ai/status').then((s) => setAi(s.enabled)).catch(() => {});
  }, []);

  const run = async () => {
    setBusy(true);
    setCopied(false);
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    if (polish) params.set('polish', 'true');
    try {
      const res = await get<{ text: string; generatedBy: string }>(`/journal/export?${params}`);
      setText(res.text);
      setBy(res.generatedBy);
    } finally {
      setBusy(false);
    }
  };

  const copy = async () => {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const ta = document.createElement('textarea');
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      ta.remove();
    }
    setCopied(true);
  };

  const download = () => {
    if (!text) return;
    const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'progression-evidence.txt';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Modal title="Export for your assessment" onClose={onClose}>
      <p className="small">Your evidence and certifications, grouped by skill, behaviour and impact, ready to paste into your progression assessment.</p>
      <div className="grid grid-2">
        <div className="field">
          <label htmlFor="f">From (optional)</label>
          <input id="f" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="to">To (optional)</label>
          <input id="to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>
      <label className="check" style={{ marginBottom: '1rem', opacity: ai ? 1 : 0.6 }}>
        <input type="checkbox" checked={polish} onChange={(e) => setPolish(e.target.checked)} disabled={!ai} />
        Draft a summary paragraph for each item with AI {ai ? '' : '(needs an Anthropic API key on the server)'}
      </label>
      <button className="btn blue" onClick={run} disabled={busy}>
        {busy ? 'Preparing…' : text ? 'Regenerate' : 'Generate export'}
      </button>
      {text && (
        <>
          <pre className="mt" aria-label="Exported evidence">{text}</pre>
          <div className="row">
            <button className="btn" onClick={copy}>
              {copied ? '✓ Copied!' : '📋 Copy to clipboard'}
            </button>
            <button className="btn secondary" onClick={download}>
              ⬇ Download .txt
            </button>
            {by === 'claude' && <span className="chip mint">✨ Summaries drafted by AI. Check them before you paste.</span>}
          </div>
        </>
      )}
    </Modal>
  );
}
