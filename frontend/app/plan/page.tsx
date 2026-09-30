'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { PipCharacter } from '@/components/PipCharacter';
import { ResourceCard } from '@/components/ResourceCard';
import { Descriptor, ErrorBox } from '@/components/ui';
import { get, post } from '@/lib/api';
import { itemTypeLabel, ukDate } from '@/lib/format';
import type { Plan } from '@/lib/types';

export default function PlanPage() {
  return (
    <AppShell>
      <PlanInner />
    </AppShell>
  );
}

function PlanInner() {
  const [plan, setPlan] = useState<Plan | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ai, setAi] = useState<{ enabled: boolean; model: string } | null>(null);

  useEffect(() => {
    get<Plan>('/recommendations/latest')
      .then((p) => setPlan(p || null))
      .catch(() => {})
      .finally(() => setLoaded(true));
    get<{ enabled: boolean; model: string }>('/ai/status').then(setAi).catch(() => {});
  }, []);

  const generate = async () => {
    setBusy(true);
    setError(null);
    try {
      setPlan(await post<Plan>('/recommendations'));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h1>My training plan</h1>
          <p className="subtitle">Training matched to the gaps between where you are and where you want to be.</p>
        </div>
        <button className="btn blue" onClick={generate} disabled={busy}>
          {busy ? 'Thinking…' : plan ? '↻ Refresh my plan' : '🎯 Build my plan'}
        </button>
      </div>
      <ErrorBox error={error} />

      {busy && (
        <div className="card bg-mint row" style={{ marginBottom: '1rem' }}>
          <PipCharacter size={52} mood="think" bounce />
          <div>
            <strong>Pip is reading your gaps and searching the training library…</strong>
            <div className="small">This can take up to half a minute when AI is switched on.</div>
          </div>
        </div>
      )}

      {loaded && !plan && !busy && (
        <div className="card empty">
          <PipCharacter size={80} />
          <h2 className="mt">No plan yet</h2>
          <p>We&apos;ll look at your gap analysis and suggest courses, books, events and internal programmes for each gap, with a reason for each one.</p>
          <p className="small muted">Tip: rate yourself on the <Link href="/progression">Gap analysis</Link> page first for sharper suggestions.</p>
          <button className="btn blue mt" onClick={generate}>
            🎯 Build my plan
          </button>
        </div>
      )}

      {plan && (
        <>
          <section className="card bg-lilac">
            <div className="row between">
              <h2>
                {plan.currentGradeName} → {plan.targetGradeName}
              </h2>
              <span className="chip">
                {plan.generatedBy === 'claude' ? `✨ AI plan (${plan.model})` : '⚙️ Matched by skill tags'} · {ukDate(plan.generatedAt)}
              </span>
            </div>
            <p style={{ marginTop: 0 }}>{plan.summary}</p>
            {plan.quickWins.length > 0 && (
              <>
                <h3>Quick wins this week</h3>
                <ul style={{ margin: 0 }}>
                  {plan.quickWins.map((q, i) => (
                    <li key={i}>{q}</li>
                  ))}
                </ul>
              </>
            )}
            {ai && !ai.enabled && plan.generatedBy === 'rules' && (
              <p className="xs muted mt" style={{ marginBottom: 0 }}>
                AI recommendations are off. Set ANTHROPIC_API_KEY on the backend to have Claude tailor your plan.
              </p>
            )}
          </section>

          <div className="stack mt2">
            {plan.items.map((item, idx) => (
              <section key={item.gap.ref} className="card">
                <div className="row between" style={{ alignItems: 'flex-start' }}>
                  <div>
                    <div className="xs muted">
                      PRIORITY {idx + 1} · {itemTypeLabel[item.gap.type]?.toUpperCase()}
                    </div>
                    <h2 style={{ margin: '0.2rem 0' }}>{item.gap.name}</h2>
                    <div className="small">
                      <span className="chip">{item.gap.from}</span> → <span className="chip pink">{item.gap.to}</span>
                    </div>
                  </div>
                </div>
                {item.gap.targetDescriptor && (
                  <details className="mt">
                    <summary>What {item.gap.to} looks like</summary>
                    <Descriptor text={item.gap.targetDescriptor} />
                  </details>
                )}
                <div className="notice mt">
                  <strong>Try this on your project: </strong>
                  {item.action}
                </div>
                <div className="grid grid-3 mt">
                  {item.suggestions.map((s) => (
                    <ResourceCard key={s.resource.id} resource={s.resource} reason={s.reason} compact />
                  ))}
                  {item.suggestions.length === 0 && <p className="muted small">No catalogue match yet. Ask Pip or your Head of Practice for ideas.</p>}
                </div>
              </section>
            ))}
          </div>
        </>
      )}
    </>
  );
}
