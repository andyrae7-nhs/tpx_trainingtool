export type LevelInfo = { level: number; title: string; xp: number; currentLevelXp: number; nextLevelXp: number };

export type User = {
  id: number;
  email?: string;
  displayName: string;
  bio?: string;
  avatarColor?: string;
  roleId?: string;
  roleName?: string;
  capability?: string;
  practice?: string;
  currentGrade?: string;
  currentGradeName?: string;
  targetGrade?: string;
  targetGradeName?: string;
  onboarded: boolean;
  level: LevelInfo;
  streakDays: number;
  followers: number;
  following: number;
  isFollowing?: boolean;
  isMe?: boolean;
};

export type UserSummary = {
  id: number;
  displayName: string;
  avatarColor?: string;
  roleName?: string;
  capability?: string;
  currentGradeName?: string;
  level: number;
};

export type RoleSummary = { id: string; name: string; capability: string; practice: string; skillCount: number };
export type Grade = { code: string; name: string; number: number };

export type ItemType = 'SKILL' | 'BEHAVIOUR' | 'IMPACT';
export type GapStatus = 'MET' | 'GAP' | 'NOT_REQUIRED';

export type GapItem = {
  ref: string;
  type: ItemType;
  id: string;
  name: string;
  definition?: string;
  currentExpected?: string;
  targetExpected?: string;
  selfLevel?: string;
  selfAssessed: boolean;
  gap: number;
  status: GapStatus;
  selfDescriptor?: string;
  targetDescriptor?: string;
  note?: string;
  evidenceCount: number;
  certifications: CertificationBadge[];
};

export type GapReport = {
  roleId: string;
  roleName: string;
  capability: string;
  practice: string;
  currentGrade: string;
  currentGradeName: string;
  targetGrade: string;
  targetGradeName: string;
  skillScale: string[];
  gradeScale: Grade[];
  skills: GapItem[];
  behaviours: GapItem[];
  impacts: GapItem[];
  summary: {
    required: number;
    met: number;
    gaps: number;
    readinessPercent: number;
    assessed: number;
    totalItems: number;
    skillGaps: number;
    behaviourGaps: number;
    impactGaps: number;
  };
};

export type Resource = {
  id: string;
  title: string;
  type: string;
  provider?: string;
  url?: string;
  description?: string;
  tags: string[];
  level?: string;
  duration?: string;
  cost?: string;
  format?: string;
};

export type Plan = {
  generatedBy: 'claude' | 'rules';
  model?: string;
  generatedAt: string;
  roleName: string;
  currentGradeName: string;
  targetGradeName: string;
  summary: string;
  items: {
    gap: { ref: string; type: ItemType; name: string; from: string; to: string; gap: number; targetDescriptor?: string };
    suggestions: { resource: Resource; reason: string }[];
    action: string;
  }[];
  quickWins: string[];
};

export type JournalEntry = {
  id: number;
  title: string;
  body?: string;
  impact?: string;
  entryDate: string;
  refs: { ref: string; name: string }[];
  createdAt: string;
  updatedAt: string;
};

export type LearningType = 'COURSE' | 'BOOK' | 'EVENT' | 'PROGRAMME' | 'OTHER';
export type LearningStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED';

export type LearningItem = {
  id: number;
  type: LearningType;
  title: string;
  provider?: string;
  url?: string;
  catalogueId?: string;
  status: LearningStatus;
  completedOn?: string;
  rating?: number;
  review?: string;
  shared: boolean;
  refs: string[];
  createdAt: string;
};

export type Review = {
  id: number;
  title: string;
  type: string;
  provider?: string;
  url?: string;
  catalogueId?: string;
  rating?: number;
  review: string;
  completedOn?: string;
  createdAt: string;
  author: UserSummary;
};

export type Achievement = {
  code: string;
  title: string;
  description: string;
  icon: string;
  category: string;
  bonusXp: number;
  earned: boolean;
  earnedAt?: string;
};

export type LeaderboardRow = {
  rank: number;
  userId: number;
  displayName: string;
  avatarColor?: string;
  roleName?: string;
  capability?: string;
  xp: number;
  periodXp: number;
  level: number;
  levelTitle: string;
  badges: number;
  streakDays: number;
  isMe: boolean;
};

export type Comment = { id: number; content: string; createdAt: string; author: UserSummary; mine: boolean };

export type Post = {
  id: number;
  content: string;
  kind: 'GENERAL' | 'ACHIEVEMENT' | 'LEARNING' | 'MILESTONE';
  createdAt: string;
  author: UserSummary;
  likeCount: number;
  likedByMe: boolean;
  comments: Comment[];
  mine: boolean;
};

export type GradedItem = { id: string; name: string; type: string; definition?: string; levels: Record<string, string> };
export type RoleDetail = {
  id: string;
  name: string;
  capability: string;
  practice: string;
  skills: { skill: { id: string; name: string; definition?: string; levels: Record<string, string> }; expected: Record<string, string | null> }[];
};

// ---------- Idea gacha ----------
export type Rarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
export type IdeaKind = 'ACTION' | 'PROJECT' | 'RESOURCE';

export type GachaWallet = {
  gems: number;
  xp: number;
  pityCount: number;
  pityThreshold: number;
  dailyAvailable: boolean;
  dailyGems: number;
  vipLevel: number;
  vipTitle: string;
  xpBonusPercent: number;
  fakeSpendPence: number;
  nextVipTitle?: string;
  nextVipPence?: number;
  totalPulls: number;
  gemsPerXp: number;
};

export type GachaBanner = {
  code: 'STANDARD' | 'BOOSTED' | 'WHALE';
  name: string;
  cost: number;
  description: string;
  guaranteedLegendary: boolean;
  odds: Record<Rarity, number>;
};

export type GemPack = { code: string; name: string; pricePence: number; gems: number; tagline: string };
export type VipTier = { level: number; title: string; minSpendPence: number };

export type GachaOverview = {
  wallet: GachaWallet;
  banners: GachaBanner[];
  shop: GemPack[];
  vipTiers: VipTier[];
  poolSize: number;
  pullsPerBatch: number;
  minXpExchange: number;
};

export type GachaIdea = {
  id: string;
  kind: IdeaKind;
  rarity: Rarity;
  rarityLabel: string;
  stars: number;
  title: string;
  description: string;
  icon: string;
  url?: string;
  resourceId?: string;
};

export type PulledCard = { idea: GachaIdea; isNew: boolean; xp: number; gemRefund: number; pity: boolean };

export type PullResult = {
  batchId: string;
  banner: string;
  cards: PulledCard[];
  gemsSpent: number;
  gemsRefunded: number;
  xpGained: number;
  badgesUnlocked: string[];
  wallet: GachaWallet;
};

export type CollectionItem = {
  id: string;
  kind: IdeaKind;
  rarity: Rarity;
  stars: number;
  owned: boolean;
  title?: string;
  description?: string;
  icon?: string;
  url?: string;
  copies: number;
  firstPulledAt?: string;
};

export type GachaCollection = {
  poolSize: number;
  owned: number;
  byRarity: { rarity: Rarity; label: string; owned: number; total: number }[];
  items: CollectionItem[];
};

export type GachaHistoryBatch = {
  batchId: string;
  banner: string;
  createdAt: string;
  xpGained: number;
  cards: { ideaId: string; title: string; icon: string; rarity: Rarity; duplicate: boolean }[];
};
// ---------- Certifications ----------
export type CertificationStatus = 'EARNED' | 'IN_PROGRESS';
export type CertificationExpiry = 'NO_EXPIRY' | 'ACTIVE' | 'EXPIRING_SOON' | 'EXPIRED';

/** Short form shown next to framework items. */
export type CertificationBadge = {
  id: number;
  name: string;
  issuer?: string;
  status: CertificationStatus;
  expiry: CertificationExpiry;
  expiresOn?: string;
};

export type Certification = {
  id: number;
  name: string;
  issuer?: string;
  status: CertificationStatus;
  credentialId?: string;
  credentialUrl?: string;
  issuedOn?: string;
  expiresOn?: string;
  expiry: CertificationExpiry;
  notes?: string;
  refs: { ref: string; name: string }[];
  createdAt: string;
  updatedAt: string;
};

export type CertificationSuggestion = { ref: string; name: string; type: string; score: number };
export type KnownCertification = { name: string; issuer: string };
