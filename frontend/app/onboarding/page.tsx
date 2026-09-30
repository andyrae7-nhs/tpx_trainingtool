'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { AuthLayout } from '@/components/AuthLayout';
import { RolePicker, RoleChoice } from '@/components/RolePicker';
import { ErrorBox } from '@/components/ui';
import { put } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { User } from '@/lib/types';

export default function Onboarding() {
  const { user, loading, setUser } = useAuth();
  const router = useRouter();
  const [choice, setChoice] = useState<RoleChoice>({ roleId: '', currentGrade: '', targetGrade: '' });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace('/login');
    if (user) setChoice({ roleId: user.roleId || '', currentGrade: user.currentGrade || '', targetGrade: user.targetGrade || '' });
  }, [user, loading, router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const u = await put<User>('/me', choice);
      setUser(u);
      router.replace('/progression?welcome=1');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout>
      <h1>Personalise your journey</h1>
      <p className="subtitle">Tell us your role and where you&apos;re heading. We&apos;ll tailor your progression assessment to you.</p>
      <form onSubmit={submit} style={{ maxWidth: 520 }}>
        <ErrorBox error={error} />
        <div className="mt">
          <RolePicker value={choice} onChange={setChoice} />
        </div>
        <button className="btn blue mt" disabled={busy || !choice.roleId || !choice.currentGrade || !choice.targetGrade}>
          {busy ? 'Saving…' : 'Show me my gaps →'}
        </button>
      </form>
    </AuthLayout>
  );
}
