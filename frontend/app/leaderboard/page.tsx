'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { Avatar, Loading } from '@/components/ui';
import { get } from '@/lib/api';
import type { LeaderboardRow } from '@/lib/types';

export default function LeaderboardPage() {
  return (
    <AppShell>
      <Leaderboard />
    </AppShell>
  );
}

const MEDALS = ['🥇', '🥈', '🥉'];

function Leaderboard() {
  const [scope, setScope] = useState('all');
  const [period, setPeriod] = useState('month');
  const [rows, setRows] = useState<LeaderboardRow[] | null>(null);

  useEffect(() => {
    setRows(null);
    get<LeaderboardRow[]>(`/leaderboard?scope=${scope}&period=${period}`).then(setRows);
  }, [scope, period]);

  const top = rows?.slice(0, 3) || [];
  const podiumOrder = [top[1], top[0], top[2]];
  const heights = ['bg-blue', 'bg-mint', 'bg-pink'];
  const me = rows?.find((r) => r.isMe);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Leaderboard</h1>
          <p className="subtitle">XP comes from adding evidence, finishing learning, sharing reviews and supporting others.</p>
        </div>
        <div className="row">
          <div className="seg" aria-label="Who">
            {[
              ['all', 'Everyone'],
              ['capability', 'My capability'],
              ['friends', 'People I follow'],
            ].map(([k, l]) => (
              <button key={k} className={scope === k ? 'active' : ''} onClick={() => setScope(k)}>
                {l}
              </button>
            ))}
          </div>
          <div className="seg" aria-label="When">
            {[
              ['week', 'This week'],
              ['month', 'This month'],
              ['all', 'All time'],
            ].map(([k, l]) => (
              <button key={k} className={period === k ? 'active' : ''} onClick={() => setPeriod(k)}>
                {l}
              </button>
            ))}
          </div>
        </div>
      </div>

      {!rows ? (
        <Loading />
      ) : (
        <>
          {me && (
            <div className="notice" style={{ marginBottom: '1rem' }}>
              You&apos;re <strong>#{me.rank}</strong> with <strong>{me.periodXp.toLocaleString('en-GB')} XP</strong> {period === 'all' ? 'overall' : period === 'week' ? 'this week' : 'this month'}.
              {me.rank > 1 && rows[me.rank - 2] && <> {rows[me.rank - 2].periodXp - me.periodXp + 1} XP to overtake {rows[me.rank - 2].displayName.split(' ')[0]}!</>}
            </div>
          )}
          {top.length >= 3 && (
            <div className="podium">
              {podiumOrder.map((r, i) =>
                r ? (
                  <Link key={r.userId} href={`/people/${r.userId}`} className={`card ${heights[i]}`} style={{ textDecoration: 'none', color: 'inherit', paddingTop: i === 1 ? '2rem' : '1.25rem', paddingBottom: i === 1 ? '2rem' : '1.25rem' }}>
                    <div style={{ fontSize: '2rem' }}>{MEDALS[r.rank - 1]}</div>
                    <Avatar name={r.displayName} colour="#fff" size="lg" />
                    <div className="h mt" style={{ color: 'var(--black)' }}>{r.displayName}</div>
                    <div className="xs">{r.roleName}</div>
                    <div className="h" style={{ fontSize: '1.6rem', color: 'var(--black)', marginTop: '0.4rem' }}>
                      {r.periodXp.toLocaleString('en-GB')} XP
                    </div>
                  </Link>
                ) : (
                  <div key={i} />
                ),
              )}
            </div>
          )}
          <div className="card" style={{ overflowX: 'auto' }}>
            <table className="board">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Person</th>
                  <th className="hide-sm">Level</th>
                  <th className="hide-sm">Badges</th>
                  <th className="hide-sm">Streak</th>
                  <th style={{ textAlign: 'right' }}>XP</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.userId} className={r.isMe ? 'me' : ''}>
                    <td className="rank">{r.rank <= 3 ? MEDALS[r.rank - 1] : r.rank}</td>
                    <td>
                      <div className="row" style={{ flexWrap: 'nowrap' }}>
                        <Avatar name={r.displayName} colour={r.avatarColor} size="sm" />
                        <div>
                          <Link href={`/people/${r.userId}`} style={{ fontWeight: 700, color: 'var(--black)' }}>
                            {r.displayName} {r.isMe && '(you)'}
                          </Link>
                          <div className="xs muted">{r.roleName || 'Role not set'}</div>
                        </div>
                      </div>
                    </td>
                    <td className="hide-sm">
                      {r.level} · {r.levelTitle}
                    </td>
                    <td className="hide-sm">🏅 {r.badges}</td>
                    <td className="hide-sm">{r.streakDays > 0 ? `🔥 ${r.streakDays}` : '–'}</td>
                    <td style={{ textAlign: 'right', fontWeight: 700 }}>{r.periodXp.toLocaleString('en-GB')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {rows.length === 0 && <div className="empty">No one here yet. Follow some colleagues!</div>}
          </div>
        </>
      )}
    </>
  );
}
