'use client';

import { useEffect } from 'react';
import { initials } from '@/lib/format';

export function Avatar({ name, colour, size }: { name?: string; colour?: string; size?: 'sm' | 'lg' }) {
  return (
    <span className={`avatar${size ? ' ' + size : ''}`} style={{ background: colour || 'var(--pastel-blue)' }} aria-hidden>
      {initials(name)}
    </span>
  );
}

export function Stars({ value, onChange }: { value?: number; onChange?: (n: number) => void }) {
  if (!onChange) {
    const v = value || 0;
    return (
      <span className="stars" aria-label={`${v} out of 5 stars`}>
        {'★'.repeat(v)}
        <span style={{ color: 'var(--grey-300)' }}>{'★'.repeat(5 - v)}</span>
      </span>
    );
  }
  return (
    <span className="stars" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button type="button" key={n} className={n <= (value || 0) ? 'on' : ''} onClick={() => onChange(n)} aria-label={`${n} star${n > 1 ? 's' : ''}`} aria-checked={value === n} role="radio">
          ★
        </button>
      ))}
    </span>
  );
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <div className="row between" style={{ marginBottom: '0.75rem' }}>
          <h2 style={{ margin: 0 }}>{title}</h2>
          <button className="btn ghost" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Ring({ percent, size = 120, label }: { percent: number; size?: number; label?: string }) {
  const r = (size - 14) / 2;
  const c = 2 * Math.PI * r;
  const p = Math.max(0, Math.min(100, percent));
  return (
    <div className="ring" style={{ width: size, height: size }}>
      <svg width={size} height={size} aria-hidden>
        <circle cx={size / 2} cy={size / 2} r={r} stroke="rgba(20,20,20,0.1)" strokeWidth="12" fill="none" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          stroke="#141414"
          strokeWidth="12"
          fill="none"
          strokeLinecap="round"
          strokeDasharray={`${(p / 100) * c} ${c}`}
          opacity={p > 0 ? 1 : 0}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      <div className="label">
        <div className="num">{Math.round(p)}%</div>
        {label && <div className="xs">{label}</div>}
      </div>
    </div>
  );
}

export function Loading() {
  return (
    <div className="grid grid-3">
      <div className="skeleton" />
      <div className="skeleton" />
      <div className="skeleton" />
    </div>
  );
}

export function ErrorBox({ error }: { error?: string | null }) {
  if (!error) return null;
  return (
    <div className="error" role="alert">
      {error}
    </div>
  );
}

export const PASTELS = ['bg-blue', 'bg-pink', 'bg-mint', 'bg-lilac'];

export function Descriptor({ text }: { text?: string | null }) {
  if (!text) return <p className="muted small">Not defined for this grade.</p>;
  const lines = text
    .split(/\n+/)
    .map((l) => l.trim())
    .filter(Boolean);
  const isList = lines.some((l) => /^[-•]/.test(l));
  if (!isList) return <p className="desc">{lines.join('\n\n')}</p>;
  return (
    <div className="desc">
      <ul>
        {lines.map((l, i) => (
          <li key={i}>{l.replace(/^[-•]\s*/, '')}</li>
        ))}
      </ul>
    </div>
  );
}
