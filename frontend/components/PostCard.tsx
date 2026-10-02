'use client';

import Link from 'next/link';
import { useState } from 'react';
import { del, post as apiPost } from '@/lib/api';
import { timeAgo } from '@/lib/format';
import type { Post } from '@/lib/types';
import { Avatar } from './ui';

const KIND_STYLE: Record<string, string> = { ACHIEVEMENT: 'bg-lilac', LEARNING: 'bg-mint', MILESTONE: 'bg-pink', GENERAL: '' };

export function PostCard({ post, onChange, onDelete }: { post: Post; onChange: (p: Post) => void; onDelete: () => void }) {
  const [comment, setComment] = useState('');
  const [showComments, setShowComments] = useState(post.comments.length > 0 && post.comments.length <= 2);
  const auto = post.kind !== 'GENERAL';

  const like = async () => onChange(await apiPost<Post>(`/social/posts/${post.id}/like`));
  const addComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!comment.trim()) return;
    onChange(await apiPost<Post>(`/social/posts/${post.id}/comments`, { content: comment }));
    setComment('');
    setShowComments(true);
  };

  return (
    <article className={`card post kind-${post.kind}`}>
      <Avatar name={post.author.displayName} colour={post.author.avatarColor} />
      <div className="body">
        <div className="row between">
          <div>
            <Link href={`/people/${post.author.id}`} style={{ fontWeight: 700, color: 'var(--black)' }}>
              {post.author.displayName}
            </Link>{' '}
            <span className="xs muted">
              {post.author.roleName} · {timeAgo(post.createdAt)}
            </span>
          </div>
          {post.mine && (
            <button className="btn ghost small" onClick={async () => { if (window.confirm('Delete this post?')) { await del(`/social/posts/${post.id}`); onDelete(); } }}>
              Delete
            </button>
          )}
        </div>
        {auto ? (
          <div className={`content small ${KIND_STYLE[post.kind]}`} style={{ padding: '0.55rem 0.75rem', borderRadius: 8 }}>
            {post.kind === 'ACHIEVEMENT' ? '🏅 ' : '📚 '}
            {post.author.displayName.split(' ')[0]} {post.content}
          </div>
        ) : (
          <div className="content">{post.content}</div>
        )}
        <div className="row small" style={{ gap: '0.4rem' }}>
          <button className={`btn small ${post.likedByMe ? '' : 'secondary'}`} onClick={like} aria-pressed={post.likedByMe}>
            {post.likedByMe ? '💙' : '🤍'} {post.likeCount > 0 ? post.likeCount : ''} {auto ? 'Cheer' : 'Like'}
          </button>
          <button className="btn small ghost" onClick={() => setShowComments(!showComments)}>
            💬 {post.comments.length} comment{post.comments.length === 1 ? '' : 's'}
          </button>
        </div>
        {showComments && (
          <div className="stack mt" style={{ gap: '0.4rem' }}>
            {post.comments.map((c) => (
              <div key={c.id} className="comment small">
                <Avatar name={c.author.displayName} colour={c.author.avatarColor} size="sm" />
                <div>
                  <strong>{c.author.displayName}</strong> <span className="xs muted">{timeAgo(c.createdAt)}</span>
                  <div>{c.content}</div>
                </div>
              </div>
            ))}
            <form onSubmit={addComment} className="row" style={{ flexWrap: 'nowrap' }}>
              <input type="text" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Write a comment…" aria-label="Write a comment" maxLength={1000} />
              <button className="btn small" disabled={!comment.trim()}>
                Reply
              </button>
            </form>
          </div>
        )}
      </div>
    </article>
  );
}
