'use client';

import { useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { RoleChoice, RolePicker } from '@/components/RolePicker';
import { Avatar, ErrorBox } from '@/components/ui';
import { put } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { User } from '@/lib/types';

const COLOURS = ['#c8e9ff', '#ffcfca', '#cafce5', '#e7d2ff', '#ffffff'];

export default function ProfilePage() {
  return (
    <AppShell allowUnonboarded>
      <Profile />
    </AppShell>
  );
}

function Profile() {
  const { user, setUser } = useAuth();
  const [name, setName] = useState('');
  const [bio, setBio] = useState('');
  const [colour, setColour] = useState('');
  const [choice, setChoice] = useState<RoleChoice>({ roleId: '', currentGrade: '', targetGrade: '' });
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    setName(user.displayName);
    setBio(user.bio || '');
    setColour(user.avatarColor || COLOURS[0]);
    setChoice({ roleId: user.roleId || '', currentGrade: user.currentGrade || '', targetGrade: user.targetGrade || '' });
  }, [user]);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaved(false);
    try {
      const u = await put<User>('/me', { displayName: name, bio, avatarColor: colour, ...choice });
      setUser(u);
      setSaved(true);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  if (!user) return null;
  return (
    <>
      <div className="page-head">
        <div>
          <h1>Your profile</h1>
          <p className="subtitle">{user.email}</p>
        </div>
      </div>
      <form className="grid grid-2" onSubmit={save} style={{ alignItems: 'start' }}>
        <section className="card">
          <h2>About you</h2>
          <div className="row" style={{ marginBottom: '1rem' }}>
            <Avatar name={name} colour={colour} size="lg" />
            <div className="row" style={{ gap: '0.4rem' }} role="radiogroup" aria-label="Avatar colour">
              {COLOURS.map((c) => (
                <button
                  type="button"
                  key={c}
                  role="radio"
                  aria-checked={colour === c}
                  aria-label={`Colour ${c}`}
                  onClick={() => setColour(c)}
                  style={{ width: 30, height: 30, borderRadius: '50%', background: c, border: colour === c ? '3px solid var(--black)' : '2px solid var(--grey-300)', cursor: 'pointer' }}
                />
              ))}
            </div>
          </div>
          <div className="field">
            <label htmlFor="n">Name</label>
            <input id="n" type="text" value={name} onChange={(e) => setName(e.target.value)} required maxLength={80} />
          </div>
          <div className="field">
            <label htmlFor="bio">Short bio</label>
            <textarea id="bio" value={bio} onChange={(e) => setBio(e.target.value)} maxLength={1000} placeholder="What do you do, and what are you learning?" />
          </div>
        </section>
        <section className="card">
          <h2>Role and progression</h2>
          <RolePicker value={choice} onChange={setChoice} />
          <p className="hint">Changing role or grade updates your gap analysis straight away. Your self-ratings are kept.</p>
        </section>
        <div className="row">
          <button className="btn blue">Save profile</button>
          {saved && <span className="chip mint">✓ Saved</span>}
          <ErrorBox error={error} />
        </div>
      </form>
    </>
  );
}
