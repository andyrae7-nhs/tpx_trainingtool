/** Pip: TPX Grow's original sprout mascot. */
export function PipCharacter({ size = 76, mood = 'happy', bounce = false }: { size?: number; mood?: 'happy' | 'wow' | 'think'; bounce?: boolean }) {
  return (
    <svg className={`pip-svg${bounce ? ' bounce' : ''}`} width={size} height={size * 1.15} viewBox="0 0 80 92" role="img" aria-label="Pip the sprout">
      {/* leaves */}
      <path d="M40 22 C 30 6, 12 6, 8 14 C 18 22, 30 24, 40 22 Z" fill="#cafce5" stroke="#141414" strokeWidth="2.5" strokeLinejoin="round" />
      <path d="M40 22 C 50 2, 70 2, 74 10 C 64 20, 50 24, 40 22 Z" fill="#cafce5" stroke="#141414" strokeWidth="2.5" strokeLinejoin="round" />
      <path d="M40 22 L 40 30" stroke="#141414" strokeWidth="2.5" strokeLinecap="round" />
      {/* body */}
      <path d="M40 28 C 62 28, 70 46, 68 62 C 66 80, 54 88, 40 88 C 26 88, 14 80, 12 62 C 10 46, 18 28, 40 28 Z" fill="#c8e9ff" stroke="#141414" strokeWidth="2.5" />
      {/* cheeks */}
      <ellipse cx="24" cy="64" rx="5" ry="3.2" fill="#ffcfca" />
      <ellipse cx="56" cy="64" rx="5" ry="3.2" fill="#ffcfca" />
      {/* eyes */}
      {mood === 'think' ? (
        <>
          <path d="M26 54 q5 -4 10 0" stroke="#141414" strokeWidth="2.5" fill="none" strokeLinecap="round" />
          <path d="M44 54 q5 -4 10 0" stroke="#141414" strokeWidth="2.5" fill="none" strokeLinecap="round" />
        </>
      ) : (
        <>
          <circle cx="31" cy="54" r={mood === 'wow' ? 5.5 : 4.5} fill="#141414" />
          <circle cx="49" cy="54" r={mood === 'wow' ? 5.5 : 4.5} fill="#141414" />
          <circle cx="32.6" cy="52.4" r="1.5" fill="#fff" />
          <circle cx="50.6" cy="52.4" r="1.5" fill="#fff" />
        </>
      )}
      {/* mouth */}
      {mood === 'wow' ? (
        <ellipse cx="40" cy="70" rx="4" ry="5" fill="#141414" />
      ) : (
        <path d="M33 68 q7 7 14 0" stroke="#141414" strokeWidth="2.5" fill="none" strokeLinecap="round" />
      )}
    </svg>
  );
}
