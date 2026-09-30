'use client';

import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { Loading } from '@/components/ui';
import { get } from '@/lib/api';
import { ukDate } from '@/lib/format';
import type { Achievement, LevelInfo } from '@/lib/types';

const CATEGORY: Record<string, { label: string; bg: string }> = {
  GETTING_STARTED: { label: 'Getting started', bg: 'bg-blue' },
  EVIDENCE: { label: 'Evidence', bg: 'bg-pink' },
  LEARNING: { label: 'Learning', bg: 'bg-mint' },
  SOCIAL: { label: 'Together', bg: 'bg-lilac' },
  MASTERY: { label: 'Mastery', bg: 'bg-blue' },
};

export default function AchievementsPage() {
  return (
    <AppShell>
      <Achievements />
    </AppShell>
  );
}

function Achievements() {
  const [data, setData] = useState<{ level: LevelInfo; streakDays: number; achievements: Achievement[] } | null>(null);
  useEffect(() => {
    get<typeof data>('/achievements').then(setData);
  }, []);
  if (!data) return <Loading />;
  const earned = data.achievements.filter((a) => a.earned).length;
  const lvl = data.level;
  const pct = ((lvl.xp - lvl.currentLevelXp) / Math.max(1, lvl.nextLevelXp - lvl.currentLevelXp)) * 100;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Badges</h1>
          <p className="subtitle">
            {earned} of {data.achievements.length} unlocked. Every badge also earns bonus XP.
          </p>
        </div>
      </div>
      <section className="card bg-dark hero">
        <div className="row between">
          <div>
            <div className="small">LEVEL {lvl.level}</div>
            <h2 style={{ fontSize: '2rem' }}>{lvl.title}</h2>
          </div>
          <div className="row">
            <span className="chip">⭐ {lvl.xp.toLocaleString('en-GB')} XP</span>
            <span className="chip">🔥 {data.streakDays} day streak</span>
          </div>
        </div>
        <div className="bar mt">
          <span style={{ width: `${pct}%` }} />
        </div>
        <div className="xs mt">
          {lvl.nextLevelXp - lvl.xp} XP to level {lvl.level + 1}
        </div>
      </section>

      {Object.entries(CATEGORY).map(([key, cat]) => {
        const list = data.achievements.filter((a) => a.category === key);
        if (!list.length) return null;
        return (
          <section key={key} className="mt2">
            <h2>{cat.label}</h2>
            <div className="grid grid-4">
              {list.map((a) => (
                <div key={a.code} className={`badge ${a.earned ? cat.bg : 'locked'}`}>
                  <div className="icon" aria-hidden>
                    {a.icon}
                  </div>
                  <div className="title">{a.title}</div>
                  <div className="small">{a.description}</div>
                  <div className="xs mt">{a.earned ? `Unlocked ${ukDate(a.earnedAt)}` : a.bonusXp ? `🔒 +${a.bonusXp} XP` : '🔒 Locked'}</div>
                </div>
              ))}
            </div>
          </section>
        );
      })}
    </>
  );
}
