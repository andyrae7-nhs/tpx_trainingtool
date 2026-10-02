'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { PostCard } from '@/components/PostCard';
import { Avatar, Loading } from '@/components/ui';
import { del, get, post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { Post, User } from '@/lib/types';

export default function CommunityPage() {
  return (
    <AppShell>
      <Community />
    </AppShell>
  );
}

function Community() {
  const { user } = useAuth();
  const [scope, setScope] = useState<'following' | 'everyone'>('everyone');
  const [posts, setPosts] = useState<Post[] | null>(null);
  const [people, setPeople] = useState<User[]>([]);
  const [q, setQ] = useState('');
  const [draft, setDraft] = useState('');

  const loadFeed = () => get<Post[]>(`/social/feed?scope=${scope}`).then(setPosts);
  useEffect(() => {
    setPosts(null);
    loadFeed();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scope]);
  useEffect(() => {
    const t = setTimeout(() => get<User[]>(`/users?q=${encodeURIComponent(q)}`).then(setPeople), 250);
    return () => clearTimeout(t);
  }, [q]);

  const publish = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!draft.trim()) return;
    const p = await post<Post>('/social/posts', { content: draft });
    setPosts((list) => [p, ...(list || [])]);
    setDraft('');
  };

  const toggleFollow = async (u: User) => {
    const updated = u.isFollowing ? await del<User>(`/social/follow/${u.id}`) : await post<User>(`/social/follow/${u.id}`);
    setPeople((list) => list.map((x) => (x.id === u.id ? updated : x)));
    if (scope === 'following') loadFeed();
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Community</h1>
          <p className="subtitle">Share progress, celebrate wins and see what colleagues are learning.</p>
        </div>
      </div>
      <div className="grid" style={{ gridTemplateColumns: 'minmax(0, 2fr) minmax(0, 1fr)', alignItems: 'start' }}>
        <div className="stack">
          <form className="card" onSubmit={publish}>
            <div className="row" style={{ alignItems: 'flex-start', flexWrap: 'nowrap' }}>
              <Avatar name={user?.displayName} colour={user?.avatarColor} />
              <textarea
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder="What are you learning or working on? Ask for tips, share a win…"
                maxLength={2000}
                style={{ minHeight: 70 }}
                aria-label="Write a post"
              />
            </div>
            <div className="row" style={{ justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <button className="btn blue" disabled={!draft.trim()}>
                Post
              </button>
            </div>
          </form>
          <div className="seg">
            <button className={scope === 'everyone' ? 'active' : ''} onClick={() => setScope('everyone')}>
              Everyone
            </button>
            <button className={scope === 'following' ? 'active' : ''} onClick={() => setScope('following')}>
              People I follow
            </button>
          </div>
          {!posts ? (
            <Loading />
          ) : posts.length === 0 ? (
            <div className="card empty">
              {scope === 'following' ? 'Follow some colleagues to see their updates here.' : 'No posts yet. Say hello!'}
            </div>
          ) : (
            posts.map((p) => (
              <PostCard
                key={p.id}
                post={p}
                onChange={(np) => setPosts((list) => (list || []).map((x) => (x.id === np.id ? np : x)))}
                onDelete={() => setPosts((list) => (list || []).filter((x) => x.id !== p.id))}
              />
            ))
          )}
        </div>
        <aside className="card" style={{ position: 'sticky', top: '1rem' }}>
          <h2>Colleagues</h2>
          <input type="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Find someone…" aria-label="Find a colleague" />
          <div className="stack mt">
            {people
              .filter((p) => !p.isMe)
              .map((p) => (
                <div key={p.id} className="row" style={{ flexWrap: 'nowrap' }}>
                  <Avatar name={p.displayName} colour={p.avatarColor} size="sm" />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Link href={`/people/${p.id}`} style={{ fontWeight: 700, color: 'var(--black)' }}>
                      {p.displayName}
                    </Link>
                    <div className="xs muted" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {p.roleName || 'New starter'} · Lvl {p.level.level}
                    </div>
                  </div>
                  <button className={`btn small ${p.isFollowing ? 'secondary' : ''}`} onClick={() => toggleFollow(p)}>
                    {p.isFollowing ? 'Following' : 'Follow'}
                  </button>
                </div>
              ))}
          </div>
        </aside>
      </div>
      <style>{`@media (max-width: 900px) { .grid[style*="2fr"] { grid-template-columns: 1fr !important; } aside.card { position: static !important; } }`}</style>
    </>
  );
}
