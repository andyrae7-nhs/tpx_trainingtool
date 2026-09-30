'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { Avatar, ErrorBox, Loading, Modal, Stars } from '@/components/ui';
import { del, get, post, put } from '@/lib/api';
import { timeAgo, typeLabel, ukDate } from '@/lib/format';
import type { LearningItem, LearningStatus, LearningType, Review } from '@/lib/types';

const STATUSES: { key: LearningStatus; label: string; bg: string }[] = [
  { key: 'PLANNED', label: 'Planned', bg: 'bg-blue' },
  { key: 'IN_PROGRESS', label: 'In progress', bg: 'bg-lilac' },
  { key: 'COMPLETED', label: 'Completed', bg: 'bg-mint' },
];
const TYPES: LearningType[] = ['COURSE', 'BOOK', 'EVENT', 'PROGRAMME', 'OTHER'];
const TYPE_ICON: Record<string, string> = { COURSE: '💻', BOOK: '📖', EVENT: '🎟️', PROGRAMME: '🧭', OTHER: '✨' };

export default function LearningPage() {
  return (
    <AppShell>
      <Learning />
    </AppShell>
  );
}

function Learning() {
  const [items, setItems] = useState<LearningItem[] | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [tab, setTab] = useState<'mine' | 'colleagues'>('mine');
  const [adding, setAdding] = useState(false);
  const [reviewing, setReviewing] = useState<LearningItem | null>(null);
  const [typeFilter, setTypeFilter] = useState<LearningType | ''>('');

  const load = () => get<LearningItem[]>('/learning').then(setItems);
  useEffect(() => {
    load();
    get<Review[]>('/learning/reviews').then(setReviews).catch(() => {});
  }, []);

  const setStatus = async (i: LearningItem, status: LearningStatus) => {
    await put(`/learning/${i.id}`, { status });
    await load();
    if (status === 'COMPLETED' && !i.review) setReviewing({ ...i, status });
  };

  const counts = { total: items?.length || 0, done: items?.filter((i) => i.status === 'COMPLETED').length || 0 };

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Learning log</h1>
          <p className="subtitle">Track the courses, books, events and programmes you&apos;ve done, and tell colleagues what you thought.</p>
        </div>
        <div className="row">
          <Link href="/library" className="btn secondary">
            Browse library
          </Link>
          <button className="btn blue" onClick={() => setAdding(true)}>
            + Log learning
          </button>
        </div>
      </div>

      <div className="tabs">
        <button className={tab === 'mine' ? 'active' : ''} onClick={() => setTab('mine')}>
          My learning ({counts.done}/{counts.total} done)
        </button>
        <button className={tab === 'colleagues' ? 'active' : ''} onClick={() => setTab('colleagues')}>
          Colleagues&apos; reviews ({reviews.length})
        </button>
      </div>

      {tab === 'mine' && (
        <>
          <div className="row" style={{ gap: '0.35rem', marginBottom: '1rem' }}>
            <button className={`chip${!typeFilter ? ' selected' : ''}`} onClick={() => setTypeFilter('')}>
              All
            </button>
            {TYPES.map((t) => (
              <button key={t} className={`chip${typeFilter === t ? ' selected' : ''}`} onClick={() => setTypeFilter(t)}>
                {TYPE_ICON[t]} {typeLabel[t]}s
              </button>
            ))}
          </div>
          {!items ? (
            <Loading />
          ) : items.length === 0 ? (
            <div className="card empty">
              <h2>Nothing logged yet</h2>
              <p>Add something you&apos;ve done recently, or pick something from your training plan.</p>
              <button className="btn blue" onClick={() => setAdding(true)}>
                Log my first learning
              </button>
            </div>
          ) : (
            <div className="grid grid-3" style={{ alignItems: 'start' }}>
              {STATUSES.map((s) => {
                const list = items.filter((i) => i.status === s.key && (!typeFilter || i.type === typeFilter));
                return (
                  <section key={s.key} className={`card ${s.bg}`} style={{ minHeight: 200 }}>
                    <h3>
                      {s.label} <span className="chip">{list.length}</span>
                    </h3>
                    <div className="stack">
                      {list.map((i) => (
                        <div key={i.id} className="card" style={{ padding: '0.9rem' }}>
                          <div className="xs muted">
                            {TYPE_ICON[i.type]} {typeLabel[i.type]}
                            {i.provider ? ` · ${i.provider}` : ''}
                          </div>
                          <div style={{ fontWeight: 700, margin: '0.2rem 0' }}>
                            {i.url ? (
                              <a href={i.url} target="_blank" rel="noopener noreferrer">
                                {i.title} ↗
                              </a>
                            ) : (
                              i.title
                            )}
                          </div>
                          {i.status === 'COMPLETED' && (
                            <div className="small">
                              {i.rating ? <Stars value={i.rating} /> : null} {i.completedOn && <span className="xs muted">Finished {ukDate(i.completedOn)}</span>}
                              {i.review && <p style={{ margin: '0.3rem 0' }}>&ldquo;{i.review}&rdquo;</p>}
                              {i.shared && i.review && <span className="chip mint">Shared with colleagues</span>}
                            </div>
                          )}
                          <div className="row mt" style={{ gap: '0.3rem' }}>
                            {s.key === 'PLANNED' && (
                              <button className="btn small secondary" onClick={() => setStatus(i, 'IN_PROGRESS')}>
                                Start
                              </button>
                            )}
                            {s.key !== 'COMPLETED' && (
                              <button className="btn small" onClick={() => setStatus(i, 'COMPLETED')}>
                                ✓ Done
                              </button>
                            )}
                            {s.key === 'COMPLETED' && (
                              <button className="btn small secondary" onClick={() => setReviewing(i)}>
                                {i.review ? 'Edit review' : '★ Review'}
                              </button>
                            )}
                            <button
                              className="btn ghost small"
                              aria-label={`Remove ${i.title}`}
                              onClick={async () => {
                                if (window.confirm('Remove this from your log?')) {
                                  await del(`/learning/${i.id}`);
                                  load();
                                }
                              }}
                            >
                              ✕
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </section>
                );
              })}
            </div>
          )}
        </>
      )}

      {tab === 'colleagues' && (
        <div className="grid grid-2">
          {reviews.map((r) => (
            <div key={r.id} className="card">
              <div className="row">
                <Avatar name={r.author.displayName} colour={r.author.avatarColor} />
                <div>
                  <Link href={`/people/${r.author.id}`} style={{ fontWeight: 700 }}>
                    {r.author.displayName}
                  </Link>
                  <div className="xs muted">
                    {r.author.roleName} · {timeAgo(r.createdAt)}
                  </div>
                </div>
              </div>
              <div className="mt">
                <span className="chip">{typeLabel[r.type] || r.type}</span>{' '}
                <strong>{r.url ? <a href={r.url} target="_blank" rel="noopener noreferrer">{r.title} ↗</a> : r.title}</strong>
              </div>
              <Stars value={r.rating} />
              <p className="small">{r.review}</p>
            </div>
          ))}
          {reviews.length === 0 && <div className="empty">No reviews shared yet.</div>}
        </div>
      )}

      {adding && <AddForm onClose={() => setAdding(false)} onSaved={() => { setAdding(false); load(); }} />}
      {reviewing && <ReviewForm item={reviewing} onClose={() => setReviewing(null)} onSaved={() => { setReviewing(null); load(); get<Review[]>('/learning/reviews').then(setReviews); }} />}
    </>
  );
}

function AddForm({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [type, setType] = useState<LearningType>('COURSE');
  const [title, setTitle] = useState('');
  const [provider, setProvider] = useState('');
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState<LearningStatus>('PLANNED');
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await post('/learning', { type, title, provider, url: url || undefined, status });
      onSaved();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Modal title="Log learning" onClose={onClose}>
      <form onSubmit={submit}>
        <ErrorBox error={error} />
        <div className="field">
          <label>Type</label>
          <div className="row" style={{ gap: '0.35rem' }}>
            {TYPES.map((t) => (
              <button type="button" key={t} className={`chip${type === t ? ' selected' : ''}`} onClick={() => setType(t)}>
                {TYPE_ICON[t]} {typeLabel[t]}
              </button>
            ))}
          </div>
        </div>
        <div className="field">
          <label htmlFor="lt">Title</label>
          <input id="lt" type="text" required value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="grid grid-2">
          <div className="field">
            <label htmlFor="lp">Provider, author or organiser</label>
            <input id="lp" type="text" value={provider} onChange={(e) => setProvider(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="lu">Link (optional)</label>
            <input id="lu" type="url" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://" />
          </div>
        </div>
        <div className="field">
          <label htmlFor="ls">Status</label>
          <select id="ls" value={status} onChange={(e) => setStatus(e.target.value as LearningStatus)}>
            {STATUSES.map((s) => (
              <option key={s.key} value={s.key}>
                {s.label}
              </option>
            ))}
          </select>
        </div>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn blue">Save</button>
        </div>
      </form>
    </Modal>
  );
}

function ReviewForm({ item, onClose, onSaved }: { item: LearningItem; onClose: () => void; onSaved: () => void }) {
  const [rating, setRating] = useState(item.rating || 0);
  const [review, setReview] = useState(item.review || '');
  const [shared, setShared] = useState(item.review ? item.shared : true);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await put(`/learning/${item.id}`, { rating: rating || undefined, review, shared });
      onSaved();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Modal title={`Review: ${item.title}`} onClose={onClose}>
      <form onSubmit={submit}>
        <ErrorBox error={error} />
        <div className="field">
          <label>How would you rate it?</label>
          <Stars value={rating} onChange={setRating} />
        </div>
        <div className="field">
          <label htmlFor="rv">Your thoughts</label>
          <textarea id="rv" value={review} onChange={(e) => setReview(e.target.value)} maxLength={3000} placeholder="What did you get out of it? Who would you recommend it to?" />
        </div>
        <label className="check field">
          <input type="checkbox" checked={shared} onChange={(e) => setShared(e.target.checked)} />
          Share my review with colleagues (posts to the community feed)
        </label>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn blue">Save review</button>
        </div>
      </form>
    </Modal>
  );
}
