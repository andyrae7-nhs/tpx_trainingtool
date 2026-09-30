'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { usePathname } from 'next/navigation';
import { get, onActivity, post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { Achievement } from '@/lib/types';
import { PipCharacter } from './PipCharacter';

type Msg = { role: 'user' | 'assistant'; content: string };

const PAGE_TIPS: Record<string, string> = {
  '/journal': "It looks like you're writing evidence! Try: what was going on, what you did, and what changed because of it.",
  '/progression': 'Be honest with your self-ratings. The more accurate they are, the better training I can find for you.',
  '/plan': "Pick one or two suggestions to start with. Small steps add up, and you'll earn XP as you go!",
  '/learning': 'Finished something? Mark it complete and share a review to help your colleagues.',
  '/community': 'Cheer someone on today. A like or comment goes a long way.',
  '/leaderboard': 'XP comes from journalling, learning and helping others. No shortcuts, just steady growth!',
  '/library': 'Every resource has a description so you know what you are getting before you click.',
};

function safeSession(key: string, value?: string): string | null {
  try {
    if (value !== undefined) window.sessionStorage.setItem(key, value);
    return window.sessionStorage.getItem(key);
  } catch {
    return null;
  }
}

export function Pip() {
  const pathname = usePathname();
  const { user, refresh } = useAuth();
  const [open, setOpen] = useState(false);
  const [hidden, setHidden] = useState(false);
  const [tip, setTip] = useState<string | null>(null);
  const [bounce, setBounce] = useState(false);
  const [celebrate, setCelebrate] = useState<Achievement[]>([]);
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>(['What should I focus on?', 'How do I write good evidence?', 'Suggest a book']);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const msgsRef = useRef<HTMLDivElement>(null);
  const timer = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    setHidden(safeSession('pip.hidden') === '1');
  }, []);

  // Contextual pop-up tip, once per page per session.
  useEffect(() => {
    if (!user || open) return;
    const key = 'pip.tip.' + pathname;
    if (safeSession(key)) return;
    const t = setTimeout(async () => {
      let text = PAGE_TIPS[pathname];
      if (!text) {
        try {
          text = (await get<{ tip: string }>('/pip/tip')).tip;
        } catch {
          return;
        }
      }
      setTip(text);
      setBounce(true);
      safeSession(key, '1');
      setTimeout(() => setBounce(false), 3000);
    }, 3500);
    return () => clearTimeout(t);
  }, [pathname, user, open]);

  // After any change, check for newly unlocked achievements and celebrate.
  const checkBadges = useCallback(() => {
    clearTimeout(timer.current);
    timer.current = setTimeout(async () => {
      try {
        const fresh = await post<Achievement[]>('/achievements/unseen');
        if (fresh && fresh.length) {
          setCelebrate(fresh);
          setHidden(false);
          setBounce(true);
          refresh();
        }
      } catch {
        /* ignore */
      }
    }, 700);
  }, [refresh]);

  useEffect(() => {
    if (!user) return;
    checkBadges();
    const off = onActivity(() => {
      // Don't re-trigger from our own unseen call
      checkBadges();
    });
    return () => {
      off();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  useEffect(() => {
    msgsRef.current?.scrollTo({ top: msgsRef.current.scrollHeight });
  }, [msgs, open]);

  const send = async (text: string) => {
    if (!text.trim() || busy) return;
    const history = msgs;
    setMsgs([...history, { role: 'user', content: text }]);
    setInput('');
    setBusy(true);
    try {
      const res = await post<{ reply: string; suggestions: string[] }>('/pip/chat', { message: text, history });
      setMsgs((m) => [...m, { role: 'assistant', content: res.reply }]);
      if (res.suggestions?.length) setSuggestions(res.suggestions);
    } catch (e) {
      setMsgs((m) => [...m, { role: 'assistant', content: "Oops, I couldn't reach the server. Try again in a moment." }]);
    } finally {
      setBusy(false);
    }
  };

  if (!user) return null;

  if (hidden && !celebrate.length) {
    return (
      <div className="pip-dock">
        <button className="chip dark" onClick={() => { setHidden(false); safeSession('pip.hidden', '0'); }}>
          🌱 Bring Pip back
        </button>
      </div>
    );
  }

  return (
    <>
      {celebrate.length > 0 && <Celebration items={celebrate} onClose={() => { setCelebrate([]); setBounce(false); }} />}
      <div className="pip-dock">
        {open ? (
          <div className="pip-panel" role="dialog" aria-label="Chat with Pip">
            <header>
              <PipCharacter size={34} />
              <div style={{ flex: 1 }}>
                <h3>Pip</h3>
                <div className="xs">Your growth buddy</div>
              </div>
              <button className="btn ghost small" onClick={() => setOpen(false)} aria-label="Close chat">
                ✕
              </button>
            </header>
            <div className="pip-msgs" ref={msgsRef}>
              {msgs.length === 0 && (
                <div className="msg assistant">
                  Hi {user.displayName.split(' ')[0]}! I'm Pip 🌱 Ask me about your progression gaps, writing evidence or finding training.
                </div>
              )}
              {msgs.map((m, i) => (
                <div key={i} className={`msg ${m.role}`}>
                  {m.content}
                </div>
              ))}
              {busy && <div className="msg assistant">Pip is thinking…</div>}
            </div>
            <div className="pip-suggest">
              {suggestions.slice(0, 4).map((s) => (
                <button key={s} className="chip outline" onClick={() => send(s)} disabled={busy}>
                  {s}
                </button>
              ))}
            </div>
            <form
              className="pip-input"
              onSubmit={(e) => {
                e.preventDefault();
                send(input);
              }}
            >
              <input type="text" value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask Pip anything…" aria-label="Message Pip" />
              <button className="btn blue small" disabled={busy || !input.trim()}>
                Send
              </button>
            </form>
          </div>
        ) : (
          tip && (
            <div className="pip-bubble" role="status">
              {tip}
              <div className="row">
                <button className="btn small" onClick={() => { setTip(null); setOpen(true); }}>
                  Chat with Pip
                </button>
                <button className="btn ghost small" onClick={() => setTip(null)}>
                  Not now
                </button>
                <button
                  className="btn ghost small"
                  onClick={() => {
                    setTip(null);
                    setHidden(true);
                    safeSession('pip.hidden', '1');
                  }}
                >
                  Hide Pip
                </button>
              </div>
            </div>
          )
        )}
        <button className="pip-btn" onClick={() => { setOpen(!open); setTip(null); }} aria-label={open ? 'Close Pip' : 'Open Pip, your assistant'}>
          <PipCharacter size={70} bounce={bounce} mood={busy ? 'think' : celebrate.length ? 'wow' : 'happy'} />
        </button>
      </div>
    </>
  );
}

function Celebration({ items, onClose }: { items: Achievement[]; onClose: () => void }) {
  const colours = ['#c8e9ff', '#ffcfca', '#cafce5', '#e7d2ff', '#0252bb'];
  const pieces = Array.from({ length: 40 }, (_, i) => ({
    left: Math.random() * 100,
    delay: Math.random() * 0.8,
    dur: 2 + Math.random() * 1.5,
    colour: colours[i % colours.length],
  }));
  return (
    <div className="celebrate" onClick={onClose}>
      {pieces.map((p, i) => (
        <span key={i} className="confetti" style={{ left: `${p.left}%`, background: p.colour, animationDuration: `${p.dur}s`, animationDelay: `${p.delay}s` }} />
      ))}
      <div className="card bg-mint" onClick={(e) => e.stopPropagation()} role="alertdialog" aria-label="Achievement unlocked">
        <PipCharacter size={70} mood="wow" bounce />
        <h2 className="mt">Badge unlocked!</h2>
        {items.map((a) => (
          <div key={a.code} style={{ margin: '0.75rem 0' }}>
            <div style={{ fontSize: '2.4rem' }}>{a.icon}</div>
            <div className="h" style={{ fontSize: '1.2rem', color: 'var(--black)' }}>
              {a.title}
            </div>
            <div className="small">{a.description}</div>
            {a.bonusXp > 0 && <div className="chip dark mt">+{a.bonusXp} XP</div>}
          </div>
        ))}
        <button className="btn mt" onClick={onClose}>
          Brilliant!
        </button>
      </div>
    </div>
  );
}
