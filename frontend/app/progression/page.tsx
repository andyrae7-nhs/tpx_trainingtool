'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { Descriptor, ErrorBox, Loading, Ring } from '@/components/ui';
import { get, put } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { GapItem, GapReport, ItemType } from '@/lib/types';

type Tab = 'SKILL' | 'BEHAVIOUR' | 'IMPACT';
const TABS: { key: Tab; label: string }[] = [
  { key: 'SKILL', label: 'Technical skills' },
  { key: 'BEHAVIOUR', label: 'Behaviours' },
  { key: 'IMPACT', label: 'Impact' },
];

export default function ProgressionPage() {
  return (
    <AppShell>
      <Progression />
    </AppShell>
  );
}

function Progression() {
  const { user } = useAuth();
  const [report, setReport] = useState<GapReport | null>(null);
  const [preview, setPreview] = useState<string>('');
  const [tab, setTab] = useState<Tab>('SKILL');
  const [filter, setFilter] = useState<'all' | 'gaps' | 'met'>('all');
  const [edits, setEdits] = useState<Record<string, { level: string; note?: string }>>({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [welcome, setWelcome] = useState(false);

  useEffect(() => {
    setWelcome(new URLSearchParams(window.location.search).has('welcome'));
  }, []);

  useEffect(() => {
    const q = preview ? `?targetGrade=${preview}` : '';
    get<GapReport>(`/progression/gap${q}`).then(setReport).catch((e) => setError(e.message));
  }, [preview]);

  const items: GapItem[] = useMemo(() => {
    if (!report) return [];
    const list = tab === 'SKILL' ? report.skills : tab === 'BEHAVIOUR' ? report.behaviours : report.impacts;
    return list.filter((i) => (filter === 'gaps' ? i.status === 'GAP' : filter === 'met' ? i.status === 'MET' : true));
  }, [report, tab, filter]);

  const dirty = Object.keys(edits).length;

  const save = async () => {
    if (!report) return;
    setSaving(true);
    setError(null);
    try {
      const payload = Object.entries(edits).map(([ref, v]) => {
        const [itemType, itemId] = ref.split(':');
        return { itemType: itemType as ItemType, itemId, level: v.level, note: v.note };
      });
      await put<GapReport>('/progression/assessments', payload);
      setEdits({});
      const q = preview ? `?targetGrade=${preview}` : '';
      setReport(await get<GapReport>(`/progression/gap${q}`));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  if (!report) return error ? <ErrorBox error={error} /> : <Loading />;

  const counts = { SKILL: report.summary.skillGaps, BEHAVIOUR: report.summary.behaviourGaps, IMPACT: report.summary.impactGaps };

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Gap analysis</h1>
          <p className="subtitle">
            {report.roleName} · {report.practice}
          </p>
        </div>
        <Link href="/plan" className="btn blue">
          🎯 Find training for my gaps
        </Link>
      </div>

      {welcome && (
        <div className="notice" style={{ marginBottom: '1rem' }}>
          <strong>Welcome!</strong> We&apos;ve pre-filled your ratings with what&apos;s expected at your current grade. Adjust anything that doesn&apos;t feel right, then save.
        </div>
      )}

      <section className="card bg-blue">
        <div className="row" style={{ gap: '1.5rem' }}>
          <Ring percent={report.summary.readinessPercent} size={120} label="ready" />
          <div style={{ flex: 1, minWidth: 220 }}>
            <h2>
              {report.currentGradeName} → {report.targetGradeName}
            </h2>
            <p style={{ margin: '0 0 0.6rem' }}>
              You meet <strong>{report.summary.met}</strong> of <strong>{report.summary.required}</strong> expectations for {report.targetGradeName}.{' '}
              {report.summary.assessed < report.summary.totalItems && (
                <>
                  You&apos;ve rated yourself on {report.summary.assessed} of {report.summary.totalItems} items.
                </>
              )}
            </p>
            <div className="row small">
              <label htmlFor="preview" style={{ margin: 0 }}>
                Compare against:
              </label>
              <select id="preview" value={preview || report.targetGrade} onChange={(e) => setPreview(e.target.value === user?.targetGrade ? '' : e.target.value)} style={{ width: 'auto' }}>
                {report.gradeScale.map((g) => (
                  <option key={g.code} value={g.code}>
                    {g.name} ({g.number}){g.code === user?.targetGrade ? ' - your target' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </section>

      <div className="tabs mt" role="tablist">
        {TABS.map((t) => (
          <button key={t.key} role="tab" aria-selected={tab === t.key} className={tab === t.key ? 'active' : ''} onClick={() => setTab(t.key)}>
            {t.label} {counts[t.key] > 0 && <span className="chip pink">{counts[t.key]}</span>}
          </button>
        ))}
        <span className="spacer" />
        <div className="seg" style={{ alignSelf: 'center' }}>
          {(['all', 'gaps', 'met'] as const).map((f) => (
            <button key={f} className={filter === f ? 'active' : ''} onClick={() => setFilter(f)}>
              {f === 'all' ? 'All' : f === 'gaps' ? 'Gaps' : 'Met'}
            </button>
          ))}
        </div>
      </div>

      <ErrorBox error={error} />
      <div className="list">
        {items.map((item) => (
          <GapCard
            key={item.ref}
            item={item}
            report={report}
            edit={edits[item.ref]}
            onEdit={(level) => setEdits((e) => ({ ...e, [item.ref]: { level } }))}
          />
        ))}
        {items.length === 0 && <div className="empty">Nothing to show with this filter.</div>}
      </div>

      {dirty > 0 && (
        <div style={{ position: 'sticky', bottom: '1rem', marginTop: '1rem', zIndex: 20 }}>
          <div className="card row between" style={{ border: '2px solid var(--black)' }}>
            <span>
              {dirty} unsaved change{dirty > 1 ? 's' : ''}
            </span>
            <div className="row">
              <button className="btn secondary" onClick={() => setEdits({})}>
                Discard
              </button>
              <button className="btn blue" onClick={save} disabled={saving}>
                {saving ? 'Saving…' : 'Save my ratings'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function GapCard({ item, report, edit, onEdit }: { item: GapItem; report: GapReport; edit?: { level: string }; onEdit: (level: string) => void }) {
  const isSkill = item.type === 'SKILL';
  const scale = isSkill ? report.skillScale.map((s) => ({ code: s, name: s })) : report.gradeScale.map((g) => ({ code: g.code, name: g.name }));
  const self = edit?.level ?? item.selfLevel ?? '';
  const selfIdx = scale.findIndex((s) => s.code === self);
  const targetCode = isSkill ? item.targetExpected : report.targetGrade;
  const targetIdx = scale.findIndex((s) => s.code === targetCode);
  const selfName = scale.find((s) => s.code === self)?.name || self || 'Not yet started';
  const status = edit ? (targetIdx < 0 ? 'NOT_REQUIRED' : selfIdx >= targetIdx ? 'MET' : 'GAP') : item.status;

  return (
    <div className={`card gap-item ${status}`}>
      <div className="row between" style={{ alignItems: 'flex-start' }}>
        <div style={{ flex: 1, minWidth: 220 }}>
          <div className="row" style={{ gap: '0.5rem' }}>
            <h3 style={{ margin: 0, color: 'var(--black)' }}>{item.name}</h3>
            {status === 'GAP' && <span className="chip pink">Gap</span>}
            {status === 'MET' && <span className="chip mint">✓ Met</span>}
            {status === 'NOT_REQUIRED' && <span className="chip outline">Not required</span>}
            {!item.selfAssessed && !edit && <span className="chip outline">Not rated yet</span>}
          </div>
          {item.definition && <p className="small muted" style={{ margin: '0.3rem 0 0' }}>{item.definition}</p>}
        </div>
        <div style={{ minWidth: 200 }}>
          <label htmlFor={`sel-${item.ref}`} className="xs">
            Where I am now
          </label>
          <select id={`sel-${item.ref}`} value={self} onChange={(e) => e.target.value && onEdit(e.target.value)}>
            {!self && <option value="">Not yet started</option>}
            {scale.map((s) => (
              <option key={s.code} value={s.code}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mt">
        <div className="ladder" aria-hidden>
          {scale.map((s, i) => (
            <div key={s.code} title={s.name} className={`step${i <= selfIdx ? ' self' : ''}${i === targetIdx ? ' target' : ''}`} />
          ))}
        </div>
        <div className="row between xs muted" style={{ marginTop: 4 }}>
          <span>
            You: <strong>{selfName}</strong>
          </span>
          <span>
            Target: <strong>{item.targetExpected || 'not defined'}</strong>
          </span>
        </div>
      </div>

      <details className="mt">
        <summary>Compare what&apos;s expected</summary>
        <div className="grid grid-2 mt">
          <div className="card flat">
            <div className="xs muted">WHERE YOU ARE: {selfName.toUpperCase()}</div>
            <Descriptor text={edit ? undefined : item.selfDescriptor} />
            {edit && <p className="muted small">Save to refresh this description.</p>}
          </div>
          <div className="card flat bg-pink">
            <div className="xs">TARGET: {(item.targetExpected || '').toUpperCase()}</div>
            <Descriptor text={item.targetDescriptor} />
          </div>
        </div>
      </details>

      <div className="row mt small">
        <span className="chip blue">
          📝 {item.evidenceCount} piece{item.evidenceCount === 1 ? '' : 's'} of evidence
        </span>
        <Link href={`/journal?new=1&ref=${encodeURIComponent(item.ref)}`}>+ Add evidence</Link>
        <Link href={`/library?q=${encodeURIComponent(item.name.replace(/\(.*\)/, '').trim())}`}>Find training</Link>
      </div>
    </div>
  );
}
