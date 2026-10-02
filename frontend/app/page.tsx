'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { Avatar, Ring } from '@/components/ui';
import { get } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { itemTypeLabel, timeAgo } from '@/lib/format';
import type { Achievement, GapItem, GapReport, JournalEntry, Plan, Post } from '@/lib/types';

export default function Dashboard() {
  return (
    <AppShell>
      <DashboardInner />
    </AppShell>
  );
}

function DashboardInner() {
  const { user } = useAuth();
  const [report, setReport] = useState<GapReport | null>(null);
  const [plan, setPlan] = useState<Plan | null>(null);
  const [journal, setJournal] = useState<JournalEntry[]>([]);
  const [badges, setBadges] = useState<Achievement[]>([]);
  const [feed, setFeed] = useState<Post[]>([]);

  useEffect(() => {
    get<GapReport>('/progression/gap').then(setReport).catch(() => {});
    get<Plan>('/recommendations/latest').then((p) => setPlan(p || null)).catch(() => {});
    get<JournalEntry[]>('/journal').then(setJournal).catch(() => {});
    get<{ achievements: Achievement[] }>('/achievements').then((a) => setBadges(a.achievements)).catch(() => {});
    get<Post[]>('/social/feed?scope=everyone&size=4').then(setFeed).catch(() => {});
  }, []);

  if (!user) return null;
  const lvl = user.level;
  const pct = lvl.nextLevelXp > lvl.currentLevelXp ? ((lvl.xp - lvl.currentLevelXp) / (lvl.nextLevelXp - lvl.currentLevelXp)) * 100 : 100;
  const gaps: GapItem[] = report
    ? [...report.skills, ...report.behaviours, ...report.impacts].filter((g) => g.status === 'GAP').sort((a, b) => b.gap - a.gap)
    : [];
  const earned = badges.filter((b) => b.earned);

  return (
    <>
      <section className="hero bg-dark">
        <div className="blob" style={{ width: 260, height: 260, background: 'var(--pastel-lilac)', right: -80, top: -90, opacity: 0.25 }} />
        <div className="row between" style={{ position: 'relative' }}>
          <div>
            <h1>Hello, {user.displayName.split(' ')[0]}</h1>
            <p className="subtitle">
              {user.roleName} · {user.currentGradeName} → {user.targetGradeName}
            </p>
          </div>
          <div className="row">
            <span className="chip" style={{ fontSize: '0.95rem' }}>
              🔥 {user.streakDays} day streak
            </span>
            <span className="chip" style={{ fontSize: '0.95rem' }}>
              ⭐ {lvl.xp.toLocaleString('en-GB')} XP
            </span>
          </div>
        </div>
        <div style={{ position: 'relative', maxWidth: 560 }} className="mt">
          <div className="row between small">
            <strong>
              Level {lvl.level}: {lvl.title}
            </strong>
            <span>{Math.max(0, lvl.nextLevelXp - lvl.xp)} XP to level {lvl.level + 1}</span>
          </div>
          <div className="bar" style={{ marginTop: 6 }}>
            <span style={{ width: `${pct}%` }} />
          </div>
        </div>
      </section>

      <div className="grid grid-4 mt">
        <div className="card bg-mint" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Ring percent={report?.summary.readinessPercent ?? 0} size={96} />
          <div>
            <div className="h" style={{ color: 'var(--black)', fontSize: '1rem' }}>Ready for {report?.targetGradeName || 'promotion'}</div>
            <div className="xs">
              {report ? `${report.summary.met} of ${report.summary.required} expectations met` : '…'}
            </div>
          </div>
        </div>
        <Link href="/progression" className="card bg-pink stat" style={{ textDecoration: 'none', color: 'inherit' }}>
          <span className="num">{report?.summary.gaps ?? '–'}</span>
          <span className="lbl">gaps to close</span>
          <span className="xs muted">
            {report ? `${report.summary.skillGaps} skills · ${report.summary.behaviourGaps} behaviours · ${report.summary.impactGaps} impact` : ''}
          </span>
        </Link>
        <Link href="/journal" className="card bg-blue stat" style={{ textDecoration: 'none', color: 'inherit' }}>
          <span className="num">{journal.length}</span>
          <span className="lbl">pieces of evidence</span>
          <span className="xs muted">{journal[0] ? `Last added ${timeAgo(journal[0].createdAt)}` : 'Add your first entry'}</span>
        </Link>
        <Link href="/achievements" className="card bg-lilac stat" style={{ textDecoration: 'none', color: 'inherit' }}>
          <span className="num">
            {earned.length}
            <span style={{ fontSize: '1rem' }}>/{badges.length}</span>
          </span>
          <span className="lbl">badges earned</span>
          <span style={{ fontSize: '1.3rem' }}>{earned.slice(0, 6).map((b) => b.icon).join(' ')}</span>
        </Link>
      </div>

      {report && report.summary.assessed === 0 && (
        <div className="card mt row between" style={{ border: '2px dashed var(--blue)' }}>
          <div>
            <h3>Start with a self-assessment</h3>
            <p className="small" style={{ margin: 0 }}>
              We&apos;ve assumed you&apos;re at the expected level for your current grade. Rate yourself honestly to get a much sharper plan.
            </p>
          </div>
          <Link href="/progression" className="btn blue">
            Rate myself
          </Link>
        </div>
      )}

      <div className="grid grid-2 mt2">
        <section className="card">
          <div className="row between">
            <h2>Your biggest gaps</h2>
            <Link href="/progression" className="small">
              See all
            </Link>
          </div>
          {gaps.length === 0 ? (
            <p className="muted">No gaps against your target. Nice work! Keep adding evidence.</p>
          ) : (
            <div className="list">
              {gaps.slice(0, 5).map((g) => (
                <div key={g.ref} className="row between" style={{ borderBottom: '1px solid var(--grey-100)', paddingBottom: '0.6rem' }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontWeight: 700 }}>{g.name}</div>
                    <div className="xs muted">
                      {itemTypeLabel[g.type]} · {(g.type === 'SKILL' ? g.selfLevel : report?.gradeScale.find((x) => x.code === g.selfLevel)?.name) || 'Not yet started'} → {g.targetExpected}
                    </div>
                  </div>
                  <span className="chip pink">
                    {g.gap} step{g.gap > 1 ? 's' : ''}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="card">
          <div className="row between">
            <h2>Your training plan</h2>
            <Link href="/plan" className="small">
              Open plan
            </Link>
          </div>
          {plan ? (
            <>
              <p className="small">{plan.summary}</p>
              <div className="list">
                {plan.items.slice(0, 3).map((i) => (
                  <div key={i.gap.ref} className="small">
                    <strong>{i.gap.name}:</strong> {i.suggestions[0]?.resource.title || i.action}
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="empty">
              <p>Let AI match training to your gaps.</p>
              <Link href="/plan" className="btn blue">
                🎯 Build my plan
              </Link>
            </div>
          )}
        </section>
      </div>

      <section className="card mt">
        <div className="row between">
          <h2>What colleagues are up to</h2>
          <Link href="/community" className="small">
            Go to community
          </Link>
        </div>
        <div className="grid grid-2">
          {feed.map((p) => (
            <div key={p.id} className="post">
              <Avatar name={p.author.displayName} colour={p.author.avatarColor} size="sm" />
              <div className="body small">
                <strong>{p.author.displayName}</strong> <span className="muted xs">{timeAgo(p.createdAt)}</span>
                <div style={{ display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                  {p.kind !== 'GENERAL' && p.author.displayName.split(' ')[0] + ' '}
                  {p.content}
                </div>
              </div>
            </div>
          ))}
          {feed.length === 0 && <p className="muted">Nothing yet. Be the first to post!</p>}
        </div>
      </section>
    </>
  );
}
