export function ukDate(iso?: string) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function timeAgo(iso: string) {
  const s = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (s < 60) return 'just now';
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  if (d < 30) return `${d}d ago`;
  return ukDate(iso);
}

export function initials(name?: string) {
  if (!name) return '?';
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('');
}

export const typeLabel: Record<string, string> = {
  COURSE: 'Course',
  BOOK: 'Book',
  EVENT: 'Event',
  PROGRAMME: 'Programme',
  ARTICLE: 'Guide',
  OTHER: 'Other',
};

export const itemTypeLabel: Record<string, string> = { SKILL: 'Skill', BEHAVIOUR: 'Behaviour', IMPACT: 'Impact' };

export function bullets(text?: string | null): string[] {
  if (!text) return [];
  return text
    .split(/\n+|•/)
    .map((l) => l.replace(/^\s*[-•]\s*/, '').trim())
    .filter((l) => l.length > 1);
}
