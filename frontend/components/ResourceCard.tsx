'use client';

import { useState } from 'react';
import Link from 'next/link';
import { post } from '@/lib/api';
import { typeLabel } from '@/lib/format';
import type { Resource } from '@/lib/types';

const TYPE_CHIP: Record<string, string> = { COURSE: 'blue', BOOK: 'pink', EVENT: 'lilac', PROGRAMME: 'mint', ARTICLE: 'outline' };

export function ResourceCard({ resource, reason, compact = false }: { resource: Resource; reason?: string; compact?: boolean }) {
  const [added, setAdded] = useState(false);
  const [busy, setBusy] = useState(false);
  const add = async () => {
    setBusy(true);
    try {
      await post('/learning', { catalogueId: resource.id, status: 'PLANNED' });
      setAdded(true);
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="card flat resource">
      <div className="meta">
        <span className={`chip ${TYPE_CHIP[resource.type] || ''}`}>{typeLabel[resource.type] || resource.type}</span>
        {resource.level && <span className="chip outline">{resource.level}</span>}
        {resource.cost && <span className="chip outline">{resource.cost}</span>}
      </div>
      <div className="title">
        {resource.url ? (
          <a href={resource.url} target="_blank" rel="noopener noreferrer">
            {resource.title} <span aria-hidden>↗</span>
            <span className="sr-only" style={{ position: 'absolute', left: -9999 }}>
              (opens in new tab)
            </span>
          </a>
        ) : (
          resource.title
        )}
      </div>
      {resource.provider && <div className="xs muted">{resource.provider}</div>}
      {resource.description && <p className="small" style={{ margin: 0 }}>{resource.description}</p>}
      {reason && (
        <p className="small" style={{ margin: 0, background: 'var(--pastel-mint)', padding: '0.5rem 0.65rem', borderRadius: 8 }}>
          <strong>Why this helps: </strong>
          {reason}
        </p>
      )}
      {!compact && (resource.format || resource.duration) && (
        <div className="xs muted">{[resource.format, resource.duration].filter(Boolean).join(' · ')}</div>
      )}
      <div className="actions">
        {added ? (
          <Link href="/learning" className="chip mint">
            ✓ Added to your learning log
          </Link>
        ) : (
          <button className="btn small" onClick={add} disabled={busy}>
            + Add to my learning
          </button>
        )}
        {!resource.url && <span className="xs muted">Internal. Ask Talent &amp; Development to book</span>}
      </div>
    </div>
  );
}
