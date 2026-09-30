'use client';

import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { PostCard } from '@/components/PostCard';
import { Avatar, Loading, Stars } from '@/components/ui';
import { del, get, post } from '@/lib/api';
import { typeLabel } from '@/lib/format';
import type { Achievement, Post, Review, User } from '@/lib/types';

export default function PersonPage({ params }: { params: { id: string } }) {
  return (
    <AppShell>
      <Person id={params.id} />
    </AppShell>
  );
}

function Person({ id }: { id: string }) {
  const [person, setPerson] = useState<User | null>(null);
  const [badges, setBadges] = useState<Achievement[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);

  useEffect(() => {
    get<User>(`/users/${id}`).then(setPerson);
    get<Achievement[]>(`/achievements/users/${id}`).then(setBadges);
    get<Post[]>(`/social/feed?scope=user&userId=${id}`).then(setPosts);
    get<Review[]>('/learning/reviews').then((r) => setReviews(r.filter((x) => String(x.author.id) === id)));
  }, [id]);

  if (!person) return <Loading />;
  const toggle = async () => setPerson(person.isFollowing ? await del<User>(`/social/follow/${id}`) : await post<User>(`/social/follow/${id}`));

  return (
    <>
      <section className="card bg-blue row" style={{ gap: '1.25rem' }}>
        <Avatar name={person.displayName} colour={person.avatarColor || '#fff'} size="lg" />
        <div style={{ flex: 1, minWidth: 200 }}>
          <h1 style={{ margin: 0 }}>{person.displayName}</h1>
          <p className="subtitle" style={{ margin: '0.2rem 0' }}>
            {person.roleName || 'Role not set'}
            {person.currentGradeName ? ` · ${person.currentGradeName}` : ''}
          </p>
          {person.bio && <p className="small" style={{ margin: 0 }}>{person.bio}</p>}
          <div className="row small mt">
            <span className="chip">
              Lvl {person.level.level} · {person.level.title}
            </span>
            <span className="chip">⭐ {person.level.xp.toLocaleString('en-GB')} XP</span>
            <span className="chip">{person.followers} followers</span>
            <span className="chip">{person.following} following</span>
          </div>
        </div>
        {!person.isMe && (
          <button className={`btn ${person.isFollowing ? 'secondary' : ''}`} onClick={toggle}>
            {person.isFollowing ? 'Following ✓' : '+ Follow'}
          </button>
        )}
      </section>

      <div className="grid mt2" style={{ gridTemplateColumns: 'minmax(0,2fr) minmax(0,1fr)', alignItems: 'start' }}>
        <div className="stack">
          <h2>Recent activity</h2>
          {posts.map((p) => (
            <PostCard key={p.id} post={p} onChange={(np) => setPosts((l) => l.map((x) => (x.id === np.id ? np : x)))} onDelete={() => setPosts((l) => l.filter((x) => x.id !== p.id))} />
          ))}
          {posts.length === 0 && <p className="muted">No posts yet.</p>}
        </div>
        <aside className="stack">
          <section className="card">
            <h3>Badges</h3>
            <div style={{ fontSize: '1.7rem', lineHeight: 1.6 }}>
              {badges.map((b) => (
                <span key={b.code} title={`${b.title}: ${b.description}`}>
                  {b.icon}{' '}
                </span>
              ))}
              {badges.length === 0 && <span className="small muted">None yet</span>}
            </div>
          </section>
          <section className="card">
            <h3>Reviews</h3>
            {reviews.map((r) => (
              <div key={r.id} className="small" style={{ marginBottom: '0.75rem' }}>
                <strong>{r.title}</strong> <span className="chip">{typeLabel[r.type]}</span>
                <div>
                  <Stars value={r.rating} />
                </div>
                {r.review}
              </div>
            ))}
            {reviews.length === 0 && <span className="small muted">No reviews shared yet</span>}
          </section>
        </aside>
      </div>
    </>
  );
}
