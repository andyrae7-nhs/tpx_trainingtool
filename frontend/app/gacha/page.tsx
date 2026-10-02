'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { ErrorBox, Loading } from '@/components/ui';
import { get, post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { timeAgo, ukDate } from '@/lib/format';
import type {
  CollectionItem,
  GachaBanner,
  GachaCollection,
  GachaHistoryBatch,
  GachaOverview,
  GachaWallet,
  IdeaKind,
  PullResult,
  Rarity,
} from '@/lib/types';

const RARITIES: Rarity[] = ['LEGENDARY', 'EPIC', 'RARE', 'COMMON'];
const RARITY_LABEL: Record<Rarity, string> = { COMMON: 'Common', RARE: 'Rare', EPIC: 'Epic', LEGENDARY: 'Legendary' };
const KIND_LABEL: Record<IdeaKind, string> = { ACTION: 'Development action', PROJECT: 'Project idea', RESOURCE: 'Training resource' };
const KIND_FILTER: Record<IdeaKind | 'ALL', string> = { ALL: 'All kinds', ACTION: 'Actions', PROJECT: 'Projects', RESOURCE: 'Resources' };
const BANNER_STYLE: Record<string, string> = { STANDARD: 'bg-blue', BOOSTED: 'bg-lilac', WHALE: 'whale' };

const gbp = (pence: number) => (pence / 100).toLocaleString('en-GB', { style: 'currency', currency: 'GBP' });
const n = (v: number) => v.toLocaleString('en-GB');

export default function GachaPage() {
  return (
    <AppShell>
      <Gacha />
    </AppShell>
  );
}

type Tab = 'shop' | 'collection' | 'history' | 'odds';

function Gacha() {
  const { refresh } = useAuth();
  const [data, setData] = useState<GachaOverview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [result, setResult] = useState<PullResult | null>(null);
  const [tab, setTab] = useState<Tab>('shop');
  const [toast, setToast] = useState<string | null>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    get<GachaOverview>('/gacha').then(setData).catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3500);
    return () => clearTimeout(t);
  }, [toast]);

  const setWallet = (wallet: GachaWallet) => setData((d) => (d ? { ...d, wallet } : d));

  const run = async <T,>(key: string, fn: () => Promise<T>, after?: (r: T) => void) => {
    setBusy(key);
    setError(null);
    try {
      const r = await fn();
      after?.(r);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setBusy(null);
    }
  };

  const pull = (banner: GachaBanner) =>
    run(`pull:${banner.code}`, () => post<PullResult>('/gacha/pull', { banner: banner.code }), (r) => {
      setResult(r);
      setWallet(r.wallet);
      setReload((x) => x + 1);
      refresh();
    });

  const claimDaily = () =>
    run('daily', () => post<GachaWallet>('/gacha/daily'), (w) => {
      setWallet(w);
      setToast(`+${n(w.dailyGems)} 💎 claimed. See you tomorrow!`);
    });

  if (!data) return error ? <ErrorBox error={error} /> : <Loading />;
  const w = data.wallet;
  const pityLeft = w.pityThreshold - w.pityCount;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Idea gacha</h1>
          <p className="subtitle">
            Pull {data.pullsPerBatch} random ideas from a pool of {n(data.poolSize)} development actions, project ideas and training resources.
          </p>
        </div>
        <div className="row wrap">
          <span className="chip gem-chip" title="Gems">
            💎 {n(w.gems)}
          </span>
          <span className="chip" title="Your XP">
            ⭐ {n(w.xp)} XP
          </span>
          <span className={`chip vip vip-${w.vipLevel}`} title={`+${w.xpBonusPercent}% XP from pulls`}>
            VIP {w.vipLevel} · {w.vipTitle}
          </span>
          <button className="btn small" onClick={claimDaily} disabled={!w.dailyAvailable || busy !== null}>
            {w.dailyAvailable ? `🎁 Claim ${w.dailyGems} daily gems` : '✅ Daily claimed'}
          </button>
        </div>
      </div>

      <p className="notice small">
        🎭 <strong>Everything here is pretend.</strong> Gems are free and the shop takes no money. The joke is real, though: every pull awards XP, and XP puts you on the
        leaderboard. VIP tiers boost that XP. You've been warned.
      </p>

      <ErrorBox error={error} />

      <section className="grid grid-3 mt">
        {data.banners.map((b) => {
          const short = b.cost - w.gems;
          return (
            <div key={b.code} className={`card banner ${BANNER_STYLE[b.code] || ''}`}>
              <div className="row between">
                <h2 style={{ margin: 0 }}>{b.name}</h2>
                {b.guaranteedLegendary && <span className="chip dark">★★★★ guaranteed</span>}
              </div>
              <p className="small">{b.description}</p>
              <div className="odds-strip" aria-label="Odds">
                {RARITIES.map((r) => (
                  <span key={r} className={`odds r-${r.toLowerCase()}`} title={`${RARITY_LABEL[r]} ${b.odds[r]}%`}>
                    {RARITY_LABEL[r]} {b.odds[r]}%
                  </span>
                ))}
              </div>
              <button className="btn mt" style={{ width: '100%' }} onClick={() => pull(b)} disabled={short > 0 || busy !== null}>
                {busy === `pull:${b.code}` ? 'Pulling…' : `Pull ×${data.pullsPerBatch} · ${n(b.cost)} 💎`}
              </button>
              {short > 0 && (
                <div className="xs mt" style={{ textAlign: 'center' }}>
                  You need {n(short)} more 💎.{' '}
                  <button className="linkish" onClick={() => setTab('shop')}>
                    Top up?
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </section>

      <section className="card mt">
        <div className="row between">
          <strong>Legendary pity</strong>
          <span className="small muted">
            {pityLeft <= 1 ? 'Your next pull is a guaranteed Legendary!' : `Legendary guaranteed within ${pityLeft} pulls`}
          </span>
        </div>
        <div className="bar mt pity">
          <span style={{ width: `${(w.pityCount / w.pityThreshold) * 100}%` }} />
        </div>
        <div className="xs muted mt">
          {n(w.totalPulls)} pulls so far · {gbp(w.fakeSpendPence)} pretend money spent
        </div>
      </section>

      <div className="tabs mt2" role="tablist">
        {(
          [
            ['shop', '🛒 Gem shop'],
            ['collection', '🗂️ Collection'],
            ['history', '🕘 History'],
            ['odds', '📜 Odds and rules'],
          ] as [Tab, string][]
        ).map(([k, l]) => (
          <button key={k} role="tab" aria-selected={tab === k} className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>
            {l}
          </button>
        ))}
      </div>

      {tab === 'shop' && <Shop data={data} busy={busy} run={run} setWallet={setWallet} setToast={setToast} refresh={refresh} />}
      {tab === 'collection' && <Collection reload={reload} />}
      {tab === 'history' && <History reload={reload} />}
      {tab === 'odds' && <Odds data={data} />}

      {result && <Reveal result={result} onClose={() => setResult(null)} onAgain={() => {
        const b = data.banners.find((x) => x.code === result.banner);
        setResult(null);
        if (b) pull(b);
      }} canAgain={(data.banners.find((x) => x.code === result.banner)?.cost ?? Infinity) <= data.wallet.gems} />}

      {toast && (
        <div className="gacha-toast" role="status">
          {toast}
        </div>
      )}
    </>
  );
}

// ---------------------------------------------------------------- reveal

function Reveal({ result, onClose, onAgain, canAgain }: { result: PullResult; onClose: () => void; onAgain: () => void; canAgain: boolean }) {
  const [shown, setShown] = useState(0);
  const total = result.cards.length;
  const done = shown >= total;
  const best = useMemo(() => RARITIES.find((r) => result.cards.some((c) => c.idea.rarity === r)) || 'COMMON', [result]);

  useEffect(() => {
    if (done) return;
    const reduce = typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const t = setTimeout(() => setShown((s) => s + 1), reduce ? 0 : shown === 0 ? 450 : 220);
    return () => clearTimeout(t);
  }, [shown, done]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className={`gacha-reveal best-${best.toLowerCase()}`} role="dialog" aria-modal="true" aria-label="Your pull">
      {done && best === 'LEGENDARY' && <Confetti />}
      <div className="reveal-inner">
        <div className="row between">
          <h2 style={{ margin: 0, color: 'var(--white)' }}>{done ? (best === 'LEGENDARY' ? '✨ LEGENDARY PULL ✨' : 'Your ideas') : 'Pulling…'}</h2>
          {!done && (
            <button className="btn secondary small" onClick={() => setShown(total)}>
              Reveal all
            </button>
          )}
        </div>
        <div className="gcards">
          {result.cards.map((c, i) => (
            <div key={i} className={`gcard r-${c.idea.rarity.toLowerCase()}${i < shown ? ' flipped' : ''}`}>
              <div className="gcard-inner">
                <div className="gcard-back" aria-hidden>
                  <span>TPX</span>
                </div>
                <div className="gcard-face" aria-hidden={i >= shown}>
                  <div className="row between">
                    <span className="stars-r">{'★'.repeat(c.idea.stars)}</span>
                    {c.isNew ? <span className="new-tag">NEW</span> : <span className="dupe-tag">+{c.gemRefund}💎</span>}
                  </div>
                  <div className="gicon">{c.idea.icon}</div>
                  <div className="gtitle">{c.idea.title}</div>
                  <div className="gkind">{KIND_LABEL[c.idea.kind]}</div>
                  <div className="gdesc">{c.idea.description}</div>
                  <div className="gxp">
                    +{c.xp} XP{c.pity ? ' · guaranteed' : ''}
                  </div>
                  {c.idea.url && (
                    <a className="glink" href={c.idea.url} target="_blank" rel="noreferrer">
                      Open ↗
                    </a>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
        {done && (
          <div className="reveal-summary">
            <div className="row wrap">
              <span className="chip">⭐ +{n(result.xpGained)} XP</span>
              {result.gemsRefunded > 0 && <span className="chip">💎 +{n(result.gemsRefunded)} from duplicates</span>}
              <span className="chip">{result.cards.filter((c) => c.isNew).length} new</span>
              {result.badgesUnlocked.map((b) => (
                <span key={b} className="chip mint">
                  🏅 {b}
                </span>
              ))}
            </div>
            <div className="row">
              <button className="btn secondary" onClick={onClose}>
                Done
              </button>
              <button className="btn blue" onClick={onAgain} disabled={!canAgain} title={canAgain ? '' : 'Not enough gems'}>
                Pull again
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function Confetti() {
  const bits = useMemo(
    () =>
      Array.from({ length: 60 }, (_, i) => ({
        left: Math.random() * 100,
        delay: Math.random() * 0.8,
        duration: 2 + Math.random() * 2,
        colour: ['#c8e9ff', '#ffcfca', '#cafce5', '#e7d2ff', '#f5c542'][i % 5],
      })),
    [],
  );
  return (
    <>
      {bits.map((b, i) => (
        <span key={i} className="confetti" style={{ left: `${b.left}%`, background: b.colour, animationDelay: `${b.delay}s`, animationDuration: `${b.duration}s` }} />
      ))}
    </>
  );
}

// ---------------------------------------------------------------- shop

type Runner = <T>(key: string, fn: () => Promise<T>, after?: (r: T) => void) => Promise<void>;

function Shop({
  data,
  busy,
  run,
  setWallet,
  setToast,
  refresh,
}: {
  data: GachaOverview;
  busy: string | null;
  run: Runner;
  setWallet: (w: GachaWallet) => void;
  setToast: (t: string) => void;
  refresh: () => Promise<void>;
}) {
  const w = data.wallet;
  const [xp, setXp] = useState(Math.min(100, w.xp));

  const buy = (code: string, name: string) =>
    run(`buy:${code}`, () => post<GachaWallet>(`/gacha/shop/${code}`), (nw) => {
      const levelled = nw.vipLevel > w.vipLevel;
      setWallet(nw);
      setToast(levelled ? `👑 Welcome to VIP ${nw.vipLevel}: ${nw.vipTitle}! +${nw.xpBonusPercent}% XP on every pull.` : `${name} added. Thank you for your (pretend) money!`);
    });

  const exchange = () =>
    run('exchange', () => post<GachaWallet>('/gacha/exchange', { xp }), (nw) => {
      setWallet(nw);
      setToast(`Traded ${n(xp)} XP for ${n(xp * nw.gemsPerXp)} 💎`);
      refresh();
    });

  return (
    <>
      <div className="grid grid-4">
        {data.shop.map((p, i) => (
          <div key={p.code} className={`card pack pack-${i}`}>
            {i === data.shop.length - 1 && <span className="chip dark best-value">BEST VALUE</span>}
            <div className="pack-gems">💎 {n(p.gems)}</div>
            <h3>{p.name}</h3>
            <p className="small">{p.tagline}</p>
            <button className="btn" style={{ width: '100%' }} onClick={() => buy(p.code, p.name)} disabled={busy !== null}>
              {busy === `buy:${p.code}` ? 'Processing…' : `${gbp(p.pricePence)} (pretend)`}
            </button>
          </div>
        ))}
      </div>

      <div className="grid grid-2 mt">
        <section className="card">
          <h3>Trade XP for gems</h3>
          <p className="small muted">
            1 XP = {w.gemsPerXp} 💎. You'll drop down the leaderboard, but think of the pulls.
          </p>
          <div className="row">
            <input
              type="number"
              min={data.minXpExchange}
              max={w.xp}
              step={10}
              value={xp}
              onChange={(e) => setXp(Math.max(0, Math.floor(Number(e.target.value) || 0)))}
              aria-label="XP to trade"
              className="xp-input"
            />
            <button className="btn secondary" onClick={exchange} disabled={busy !== null || xp < data.minXpExchange || xp > w.xp}>
              Trade for {n(xp * w.gemsPerXp)} 💎
            </button>
          </div>
          <div className="hint">You have {n(w.xp)} XP. Minimum trade is {data.minXpExchange} XP.</div>
        </section>

        <section className="card">
          <h3>VIP tiers</h3>
          <p className="small muted">
            Spend (pretend) money, get more XP from every pull. Totally fair.
            {w.nextVipTitle && w.nextVipPence != null && (
              <>
                {' '}
                {gbp(w.nextVipPence - w.fakeSpendPence)} more to reach <strong>{w.nextVipTitle}</strong>.
              </>
            )}
          </p>
          <ol className="vip-ladder">
            {data.vipTiers.map((t) => (
              <li key={t.level} className={t.level === w.vipLevel ? 'current' : t.level < w.vipLevel ? 'passed' : ''}>
                <span>
                  VIP {t.level} · {t.title}
                </span>
                <span className="xs">
                  {t.minSpendPence ? gbp(t.minSpendPence) : 'Free'} · +{t.level * 10}% XP
                </span>
              </li>
            ))}
          </ol>
        </section>
      </div>
    </>
  );
}

// ---------------------------------------------------------------- collection

function Collection({ reload }: { reload: number }) {
  const [data, setData] = useState<GachaCollection | null>(null);
  const [rarity, setRarity] = useState<Rarity | 'ALL'>('ALL');
  const [kind, setKind] = useState<IdeaKind | 'ALL'>('ALL');
  const [ownedOnly, setOwnedOnly] = useState(false);
  const [open, setOpen] = useState<CollectionItem | null>(null);

  const load = useCallback(() => get<GachaCollection>('/gacha/collection').then(setData), []);
  useEffect(() => {
    load();
  }, [load, reload]);

  if (!data) return <Loading />;
  const items = data.items.filter((i) => (rarity === 'ALL' || i.rarity === rarity) && (kind === 'ALL' || i.kind === kind) && (!ownedOnly || i.owned));

  return (
    <>
      <div className="grid grid-4">
        {data.byRarity
          .slice()
          .reverse()
          .map((r) => (
            <div key={r.rarity} className={`card flat stat r-${r.rarity.toLowerCase()} rarity-stat`}>
              <span className="num">
                {r.owned}/{r.total}
              </span>
              <span className="lbl">{r.label}</span>
            </div>
          ))}
      </div>
      <div className="row wrap mt">
        <div className="seg" aria-label="Rarity">
          {(['ALL', ...RARITIES] as const).map((r) => (
            <button key={r} className={rarity === r ? 'active' : ''} onClick={() => setRarity(r)}>
              {r === 'ALL' ? 'All' : RARITY_LABEL[r]}
            </button>
          ))}
        </div>
        <div className="seg" aria-label="Kind">
          {(['ALL', 'ACTION', 'PROJECT', 'RESOURCE'] as const).map((k) => (
            <button key={k} className={kind === k ? 'active' : ''} onClick={() => setKind(k)}>
              {KIND_FILTER[k]}
            </button>
          ))}
        </div>
        <label className="row small" style={{ margin: 0, fontWeight: 600 }}>
          <input type="checkbox" checked={ownedOnly} onChange={(e) => setOwnedOnly(e.target.checked)} /> Owned only
        </label>
        <span className="small muted">
          {data.owned} of {data.poolSize} collected
        </span>
      </div>
      {items.length === 0 ? (
        <div className="empty">Nothing here yet. Go pull something!</div>
      ) : (
        <div className="mini-cards mt">
          {items.map((i) =>
            i.owned ? (
              <button key={i.id} className={`mini-card r-${i.rarity.toLowerCase()}`} onClick={() => setOpen(i)}>
                <span className="gicon">{i.icon}</span>
                <span className="mtitle">{i.title}</span>
                <span className="xs">
                  {'★'.repeat(i.stars)} {i.copies > 1 ? `×${i.copies}` : ''}
                </span>
              </button>
            ) : (
              <div key={i.id} className={`mini-card locked r-${i.rarity.toLowerCase()}`} title={`Locked ${RARITY_LABEL[i.rarity]} ${KIND_LABEL[i.kind].toLowerCase()}`}>
                <span className="gicon">?</span>
                <span className="mtitle">???</span>
                <span className="xs">{'★'.repeat(i.stars)}</span>
              </div>
            ),
          )}
        </div>
      )}
      {open && (
        <div className="modal-backdrop" onClick={() => setOpen(null)}>
          <div className={`modal idea-modal r-${open.rarity.toLowerCase()}`} role="dialog" aria-modal="true" aria-label={open.title} onClick={(e) => e.stopPropagation()}>
            <div className="row between">
              <span className="chip">
                {'★'.repeat(open.stars)} {RARITY_LABEL[open.rarity]} · {KIND_LABEL[open.kind]}
              </span>
              <button className="btn ghost" onClick={() => setOpen(null)} aria-label="Close">
                ✕
              </button>
            </div>
            <div className="gicon big">{open.icon}</div>
            <h2>{open.title}</h2>
            <p>{open.description}</p>
            <p className="xs muted">
              First pulled {ukDate(open.firstPulledAt)} · owned ×{open.copies}
            </p>
            {open.url && (
              <a className="btn blue" href={open.url} target="_blank" rel="noreferrer">
                Open resource ↗
              </a>
            )}
          </div>
        </div>
      )}
    </>
  );
}

// ---------------------------------------------------------------- history

function History({ reload }: { reload: number }) {
  const [rows, setRows] = useState<GachaHistoryBatch[] | null>(null);
  useEffect(() => {
    get<GachaHistoryBatch[]>('/gacha/history').then(setRows);
  }, [reload]);
  if (!rows) return <Loading />;
  if (!rows.length) return <div className="empty">No pulls yet. Your first 10-pull is on us.</div>;
  return (
    <div className="stack">
      {rows.map((b) => (
        <div key={b.batchId} className="card flat">
          <div className="row between">
            <strong>{b.banner.charAt(0) + b.banner.slice(1).toLowerCase()} 10-pull</strong>
            <span className="small muted">
              {timeAgo(b.createdAt)} · +{b.xpGained} XP
            </span>
          </div>
          <div className="row wrap mt">
            {b.cards.map((c, i) => (
              <span key={i} className={`chip hist r-${c.rarity.toLowerCase()}`} title={RARITY_LABEL[c.rarity]}>
                {c.icon} {c.title}
                {c.duplicate ? ' (dupe)' : ''}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

// ---------------------------------------------------------------- odds

function Odds({ data }: { data: GachaOverview }) {
  const w = data.wallet;
  return (
    <div className="grid grid-2">
      <section className="card">
        <h3>Odds per pull</h3>
        <table className="odds-table">
          <thead>
            <tr>
              <th>Rarity</th>
              {data.banners.map((b) => (
                <th key={b.code}>{b.code.charAt(0) + b.code.slice(1).toLowerCase()}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {RARITIES.map((r) => (
              <tr key={r}>
                <td>
                  <span className={`chip r-${r.toLowerCase()}`}>{RARITY_LABEL[r]}</span>
                </td>
                {data.banners.map((b) => (
                  <td key={b.code}>{b.odds[r]}%</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      <section className="card">
        <h3>The rules</h3>
        <ul className="small">
          <li>Every 10-pull includes at least one Rare or better.</li>
          <li>
            Pity: if you go {w.pityThreshold - 1} pulls without a Legendary, the next one is guaranteed.
          </li>
          <li>Whale pulls always include a Legendary.</li>
          <li>XP per pull: Common 3, Rare 8, Epic 25, Legendary 100, plus your VIP bonus (currently +{w.xpBonusPercent}%).</li>
          <li>Duplicates refund gems: Common 10, Rare 25, Epic 60, Legendary 200.</li>
          <li>
            Free gems: {data.wallet.dailyGems} every day, and new players start with enough for one 10-pull.
          </li>
          <li>Training resources come from the training library. Their rarity follows their level.</li>
          <li>No real money is ever taken. Please don't try to expense your gems.</li>
        </ul>
      </section>
    </div>
  );
}
