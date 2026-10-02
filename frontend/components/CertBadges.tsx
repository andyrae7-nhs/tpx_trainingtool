'use client';

import Link from 'next/link';
import { ukDate } from '@/lib/format';
import type { CertificationBadge } from '@/lib/types';

/** Certification chips shown next to a framework item. Each links to the certification. */
export function CertBadges({ certs, compact = false }: { certs: CertificationBadge[]; compact?: boolean }) {
  if (!certs.length) return null;
  return (
    <>
      {certs.map((c) => {
        const inProgress = c.status === 'IN_PROGRESS';
        const note =
          c.expiry === 'EXPIRED'
            ? ' · expired'
            : c.expiry === 'EXPIRING_SOON' && c.expiresOn
              ? ` · expires ${ukDate(c.expiresOn)}`
              : inProgress
                ? ' · working towards'
                : '';
        const title = [c.name, c.issuer, inProgress ? 'Working towards' : 'Earned', c.expiresOn ? `Valid until ${ukDate(c.expiresOn)}` : '']
          .filter(Boolean)
          .join(' · ');
        return (
          <Link
            key={c.id}
            href={`/certifications#cert-${c.id}`}
            className={`chip cert-chip ${c.expiry === 'EXPIRED' ? 'expired' : inProgress ? 'in-progress' : c.expiry === 'EXPIRING_SOON' ? 'expiring' : ''}`}
            title={title}
          >
            📜 {c.name}
            {!compact && note}
          </Link>
        );
      })}
    </>
  );
}
