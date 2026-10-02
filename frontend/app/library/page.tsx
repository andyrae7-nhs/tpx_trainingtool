'use client';

import { useEffect, useMemo, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { ResourceCard } from '@/components/ResourceCard';
import { Avatar, Loading, Stars } from '@/components/ui';
import { get } from '@/lib/api';
import { timeAgo, typeLabel } from '@/lib/format';
import type { Resource, Review } from '@/lib/types';

const TYPES = ['', 'COURSE', 'BOOK', 'EVENT', 'PROGRAMME', 'ARTICLE'];

export default function LibraryPage() {
  return (
    <AppShell>
      <Library />
    </AppShell>
  );
}

function Library() {
  const [all, setAll] = useState<Resource[] | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [q, setQ] = useState('');
  const [type, setType] = useState('');

  useEffect(() => {
    setQ(new URLSearchParams(window.location.search).get('q') || '');
    get<Resource[]>('/catalogue').then(setAll);
    get<Review[]>('/learning/reviews').then(setReviews).catch(() => {});
  }, []);

  const results = useMemo(() => {
    if (!all) return [];
    const words = q.toLowerCase().split(/\s+/).filter((w) => w.length > 2);
    return all.filter((r) => {
      if (type && r.type !== type) return false;
      if (!words.length) return true;
      const hay = `${r.title} ${r.description} ${r.provider} ${r.tags.join(' ')}`.toLowerCase();
      return words.some((w) => hay.includes(w.replace(/s$/, '')));
    });
  }, [all, q, type]);

  const reviewsByResource = useMemo(() => {
    const m = new Map<string, Review[]>();
    reviews.forEach((r) => r.catalogueId && m.set(r.catalogueId, [...(m.get(r.catalogueId) || []), r]));
    return m;
  }, [reviews]);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Training library</h1>
          <p className="subtitle">Courses, books, events and internal programmes, each with a short description so you know what to expect.</p>
        </div>
      </div>
      <div className="card flat row" style={{ marginBottom: '1rem' }}>
        <input type="search" placeholder="Search e.g. facilitation, cloud, feedback…" value={q} onChange={(e) => setQ(e.target.value)} style={{ flex: 1, minWidth: 220 }} aria-label="Search the library" />
        <div className="row" style={{ gap: '0.35rem' }}>
          {TYPES.map((t) => (
            <button key={t || 'all'} className={`chip${type === t ? ' selected' : ''}`} onClick={() => setType(t)}>
              {t ? typeLabel[t] : 'All'}
            </button>
          ))}
        </div>
      </div>

      {reviews.length > 0 && !q && !type && (
        <section className="mt" style={{ marginBottom: '1.5rem' }}>
          <h2>Recently reviewed by colleagues</h2>
          <div className="grid grid-3">
            {reviews.slice(0, 3).map((r) => (
              <div key={r.id} className="card bg-pink">
                <div className="row">
                  <Avatar name={r.author.displayName} colour={r.author.avatarColor} size="sm" />
                  <div className="small">
                    <strong>{r.author.displayName}</strong>
                    <div className="xs">{timeAgo(r.createdAt)}</div>
                  </div>
                </div>
                <div className="mt" style={{ fontWeight: 700 }}>{r.title}</div>
                <Stars value={r.rating} />
                <p className="small" style={{ marginBottom: 0 }}>&ldquo;{r.review}&rdquo;</p>
              </div>
            ))}
          </div>
        </section>
      )}

      {!all ? (
        <Loading />
      ) : (
        <>
          <p className="small muted">{results.length} resources</p>
          <div className="grid grid-3">
            {results.map((r) => (
              <div key={r.id} className="stack" style={{ gap: 0 }}>
                <ResourceCard resource={r} />
                {reviewsByResource.get(r.id)?.length ? (
                  <details className="small" style={{ padding: '0.5rem 0.25rem' }}>
                    <summary>
                      {reviewsByResource.get(r.id)!.length} colleague review{reviewsByResource.get(r.id)!.length > 1 ? 's' : ''}
                    </summary>
                    {reviewsByResource.get(r.id)!.map((rv) => (
                      <p key={rv.id}>
                        <Stars value={rv.rating} /> <strong>{rv.author.displayName}:</strong> {rv.review}
                      </p>
                    ))}
                  </details>
                ) : null}
              </div>
            ))}
          </div>
        </>
      )}
    </>
  );
}
