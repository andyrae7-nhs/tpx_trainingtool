'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useAuth } from '@/lib/auth';
import { Avatar } from './ui';
import { Pip } from './Pip';

const NAV = [
  { section: 'Grow' },
  { href: '/', label: 'Dashboard', ico: '🏠' },
  { href: '/progression', label: 'Gap analysis', ico: '📊' },
  { href: '/plan', label: 'My training plan', ico: '🎯' },
  { href: '/library', label: 'Training library', ico: '📚' },
  { section: 'Record' },
  { href: '/journal', label: 'Evidence journal', ico: '📝' },
  { href: '/learning', label: 'Learning log', ico: '🎓' },
  { href: '/certifications', label: 'Certifications', ico: '📜' },
  { section: 'Together' },
  { href: '/community', label: 'Community', ico: '💬' },
  { href: '/leaderboard', label: 'Leaderboard', ico: '🏆' },
  { href: '/achievements', label: 'Badges', ico: '🏅' },
  { section: 'Play' },
  { href: '/gacha', label: 'Idea gacha', ico: '🎰' },
] as const;

export function Brand() {
  return (
    <Link href="/" className="brand" aria-label="TPX Grow home">
      <span className="mark">TPX</span>
      <span className="slash">/</span>
      <span className="mark">GROW</span>
    </Link>
  );
}

export function AppShell({ children, allowUnonboarded = false }: { children: React.ReactNode; allowUnonboarded?: boolean }) {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) router.replace('/login');
    else if (!user.onboarded && !allowUnonboarded) router.replace('/onboarding');
  }, [user, loading, allowUnonboarded, router]);

  useEffect(() => setMenuOpen(false), [pathname]);

  if (loading || !user) {
    return (
      <div style={{ padding: '3rem', textAlign: 'center' }} className="muted">
        Loading…
      </div>
    );
  }

  return (
    <div className="shell">
      <div className="topbar">
        <Brand />
        <button className="btn secondary small" onClick={() => setMenuOpen(!menuOpen)} aria-expanded={menuOpen} aria-controls="sidebar">
          ☰ Menu
        </button>
      </div>
      <aside className={`sidebar${menuOpen ? ' open' : ''}`} id="sidebar">
        <div>
          <Brand />
          <div className="brand" style={{ marginTop: '-0.2rem' }}>
            <span className="tag">by TPXimpact</span>
          </div>
        </div>
        <nav className="nav" aria-label="Main">
          {NAV.map((n, i) =>
            'section' in n ? (
              <div className="nav-section" key={i}>
                {n.section}
              </div>
            ) : (
              <Link key={n.href} href={n.href} className={pathname === n.href ? 'active' : ''} aria-current={pathname === n.href ? 'page' : undefined}>
                <span className="ico" aria-hidden>
                  {n.ico}
                </span>
                {n.label}
              </Link>
            ),
          )}
        </nav>
        <div className="sidebar-user">
          <Link href="/profile" style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', textDecoration: 'none', color: 'inherit', flex: 1, minWidth: 0 }}>
            <Avatar name={user.displayName} colour={user.avatarColor} />
            <div style={{ minWidth: 0 }}>
              <div style={{ fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{user.displayName}</div>
              <div className="xs muted">
                Lvl {user.level.level} · {user.level.title}
              </div>
            </div>
          </Link>
          <button className="btn ghost small" onClick={logout} title="Sign out" aria-label="Sign out">
            ⎋
          </button>
        </div>
      </aside>
      <main className="main">{children}</main>
      <Pip />
    </div>
  );
}
