import { PipCharacter } from './PipCharacter';

export function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="auth-wrap">
      <section className="auth-art">
        <div className="blob" style={{ width: 340, height: 340, background: 'var(--pastel-lilac)', right: -120, top: -100, opacity: 0.35 }} />
        <div className="blob" style={{ width: 220, height: 220, background: 'var(--pastel-mint)', left: -60, bottom: -60, opacity: 0.35 }} />
        <div className="brand" style={{ color: 'white' }}>
          <span className="mark">TPX</span>
          <span className="slash">/</span>
          <span className="mark">GROW</span>
        </div>
        <div style={{ position: 'relative' }}>
          <h1>Grow your craft.<br />Own your progression.</h1>
          <p className="subtitle">See where you are, where you want to be, and exactly how to get there.</p>
          <ul style={{ paddingLeft: '1.1rem', lineHeight: 1.9 }}>
            <li>Compare yourself with the Progression Framework for your role</li>
            <li>Get training matched to your gaps</li>
            <li>Keep an evidence journal you can export at assessment time</li>
            <li>Earn badges, climb the leaderboard and cheer on colleagues</li>
          </ul>
        </div>
        <div className="row" style={{ position: 'relative' }}>
          <PipCharacter size={64} />
          <div className="pip-bubble" style={{ color: 'var(--black)', boxShadow: 'none' }}>
            Hi, I&apos;m Pip! I&apos;ll help you along the way.
          </div>
        </div>
      </section>
      <section className="auth-form">{children}</section>
    </div>
  );
}
