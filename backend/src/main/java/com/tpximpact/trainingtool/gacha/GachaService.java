package com.tpximpact.trainingtool.gacha;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.gacha.GachaEconomy.Banner;
import com.tpximpact.trainingtool.gacha.GachaEconomy.GemPack;
import com.tpximpact.trainingtool.gacha.GachaEconomy.VipTier;
import com.tpximpact.trainingtool.gamification.AchievementCatalog.Achievement;
import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.random.RandomGenerator;

/** Gems, pulls, the gem shop and your idea collection. */
@Service
public class GachaService {

    private final GachaWalletRepository wallets;
    private final GachaPullRepository pulls;
    private final IdeaPool pool;
    private final GamificationService gamification;
    private RandomGenerator random = new SecureRandom();

    public GachaService(GachaWalletRepository wallets, GachaPullRepository pulls, IdeaPool pool,
                        GamificationService gamification) {
        this.wallets = wallets;
        this.pulls = pulls;
        this.pool = pool;
        this.gamification = gamification;
    }

    /** For tests that need predictable pulls. */
    void setRandom(RandomGenerator random) {
        this.random = random;
    }

    // ---------------------------------------------------------------- views

    public record WalletView(long gems, int xp, int pityCount, int pityThreshold, boolean dailyAvailable,
                             int dailyGems, int vipLevel, String vipTitle, int xpBonusPercent, long fakeSpendPence,
                             String nextVipTitle, Long nextVipPence, long totalPulls, int gemsPerXp) {}

    public record BannerView(String code, String name, int cost, String description, boolean guaranteedLegendary,
                             Map<Rarity, Double> odds) {}

    public record IdeaView(String id, String kind, String rarity, String rarityLabel, int stars, String title,
                           String description, String icon, String url, String resourceId) {
        static IdeaView of(Idea i) {
            return new IdeaView(i.id(), i.kind().name(), i.rarity().name(), i.rarity().label(), i.rarity().stars(),
                    i.title(), i.description(), i.icon(), i.url(), i.resourceId());
        }
    }

    public record Overview(WalletView wallet, List<BannerView> banners, List<GemPack> shop, List<VipTier> vipTiers,
                           int poolSize, int pullsPerBatch, int minXpExchange) {}

    public record PulledCard(IdeaView idea, boolean isNew, int xp, int gemRefund, boolean pity) {}

    public record PullResult(String batchId, String banner, List<PulledCard> cards, int gemsSpent, int gemsRefunded,
                             int xpGained, List<String> badgesUnlocked, WalletView wallet) {}

    public record CollectionItem(String id, String kind, String rarity, int stars, boolean owned, String title,
                                 String description, String icon, String url, long copies, Instant firstPulledAt) {}

    public record RarityProgress(String rarity, String label, int owned, int total) {}

    public record CollectionView(int poolSize, int owned, List<RarityProgress> byRarity, List<CollectionItem> items) {}

    public record HistoryCard(String ideaId, String title, String icon, String rarity, boolean duplicate) {}

    public record HistoryBatch(String batchId, String banner, Instant createdAt, int xpGained, List<HistoryCard> cards) {}

    // ---------------------------------------------------------------- reads

    @Transactional
    public Overview overview(User user) {
        GachaWallet w = walletFor(user.getId());
        List<BannerView> banners = Arrays.stream(Banner.values())
                .map(b -> new BannerView(b.name(), b.label(), b.cost(), b.description(), b.guaranteedLegendary(), b.odds()))
                .toList();
        return new Overview(view(w, user), banners, GachaEconomy.SHOP, GachaEconomy.VIP, pool.size(),
                GachaEconomy.PULLS_PER_BATCH, GachaEconomy.MIN_XP_EXCHANGE);
    }

    @Transactional(readOnly = true)
    public CollectionView collection(Long userId) {
        Map<String, GachaPullRepository.OwnedIdea> owned = new HashMap<>();
        pulls.ownedBy(userId).forEach(o -> owned.put(o.getIdeaId(), o));
        List<CollectionItem> items = new ArrayList<>();
        Map<Rarity, int[]> progress = new EnumMap<>(Rarity.class);
        for (Rarity r : Rarity.values()) progress.put(r, new int[2]);
        for (Idea idea : pool.all()) {
            var o = owned.get(idea.id());
            int[] p = progress.get(idea.rarity());
            p[1]++;
            if (o != null) {
                p[0]++;
                items.add(new CollectionItem(idea.id(), idea.kind().name(), idea.rarity().name(), idea.rarity().stars(),
                        true, idea.title(), idea.description(), idea.icon(), idea.url(), o.getCopies(), o.getFirstPulledAt()));
            } else {
                // Locked cards only reveal their rarity and kind. No peeking.
                items.add(new CollectionItem(idea.id(), idea.kind().name(), idea.rarity().name(), idea.rarity().stars(),
                        false, null, null, null, null, 0, null));
            }
        }
        items.sort(Comparator.comparing((CollectionItem i) -> Rarity.valueOf(i.rarity())).reversed()
                .thenComparing(i -> !i.owned())
                .thenComparing(i -> Objects.requireNonNullElse(i.title(), "")));
        List<RarityProgress> byRarity = Arrays.stream(Rarity.values()).map(r ->
                new RarityProgress(r.name(), r.label(), progress.get(r)[0], progress.get(r)[1])).toList();
        int ownedCount = (int) pool.all().stream().filter(i -> owned.containsKey(i.id())).count();
        return new CollectionView(pool.size(), ownedCount, byRarity, items);
    }

    @Transactional(readOnly = true)
    public List<HistoryBatch> history(Long userId) {
        Map<String, List<GachaPull>> batches = new LinkedHashMap<>();
        for (GachaPull p : pulls.findTop100ByUserIdOrderByIdDesc(userId)) {
            batches.computeIfAbsent(p.getBatchId(), k -> new ArrayList<>()).add(p);
        }
        List<HistoryBatch> out = new ArrayList<>();
        batches.forEach((id, list) -> {
            list.sort(Comparator.comparing(GachaPull::getId));
            List<HistoryCard> cards = list.stream().map(p -> {
                Optional<Idea> idea = pool.find(p.getIdeaId());
                return new HistoryCard(p.getIdeaId(), idea.map(Idea::title).orElse("Retired idea"),
                        idea.map(Idea::icon).orElse("❔"), p.getRarity(), p.isDuplicate());
            }).toList();
            int xp = list.stream().mapToInt(GachaPull::getXp).sum();
            out.add(new HistoryBatch(id, list.get(0).getBanner(), list.get(0).getCreatedAt(), xp, cards));
        });
        return out;
    }

    // ---------------------------------------------------------------- writes

    @Transactional
    public WalletView claimDaily(User user) {
        GachaWallet w = walletFor(user.getId());
        LocalDate today = LocalDate.now();
        if (today.equals(w.getLastDailyClaim())) {
            throw ApiException.badRequest("You've already claimed today's gems. Come back tomorrow (or visit the shop 👀)");
        }
        w.setLastDailyClaim(today);
        addGems(w, GachaEconomy.DAILY_GEMS);
        return view(wallets.save(w), user);
    }

    @Transactional
    public WalletView exchangeXp(User user, int xp) {
        if (xp < GachaEconomy.MIN_XP_EXCHANGE) {
            throw ApiException.badRequest("Trade at least " + GachaEconomy.MIN_XP_EXCHANGE + " XP");
        }
        if (xp > user.getXp()) {
            throw ApiException.badRequest("You only have " + user.getXp() + " XP");
        }
        GachaWallet w = walletFor(user.getId());
        addGems(w, (long) xp * GachaEconomy.GEMS_PER_XP);
        wallets.save(w);
        gamification.adjustXp(user, Activity.GACHA_EXCHANGE, -xp);
        return view(w, user);
    }

    @Transactional
    public WalletView buyPack(User user, String code) {
        GemPack pack = GachaEconomy.pack(code).orElseThrow(() -> ApiException.notFound("Gem pack"));
        GachaWallet w = walletFor(user.getId());
        addGems(w, pack.gems());
        w.setFakeSpendPence(w.getFakeSpendPence() + pack.pricePence());
        return view(wallets.save(w), user);
    }

    @Transactional
    public PullResult pull(User user, String bannerCode) {
        Banner banner = Banner.parse(bannerCode)
                .orElseThrow(() -> ApiException.badRequest("Unknown banner. Use STANDARD, BOOSTED or WHALE"));
        GachaWallet w = walletFor(user.getId());
        if (w.getGems() < banner.cost()) {
            throw ApiException.badRequest("Not enough gems: you need " + banner.cost() + " 💎 and have " + w.getGems()
                    + ". Claim your daily gems, trade XP or visit the shop.");
        }
        w.setGems(w.getGems() - banner.cost());

        // Roll rarities first so the guarantees can be applied to the whole batch.
        Rarity[] rarities = new Rarity[GachaEconomy.PULLS_PER_BATCH];
        boolean[] pity = new boolean[rarities.length];
        int sinceLegendary = w.getPullsSinceLegendary();
        for (int i = 0; i < rarities.length; i++) {
            if (sinceLegendary + 1 >= GachaEconomy.PITY_THRESHOLD) {
                rarities[i] = Rarity.LEGENDARY;
                pity[i] = true;
            } else {
                rarities[i] = roll(banner);
            }
            sinceLegendary = rarities[i] == Rarity.LEGENDARY ? 0 : sinceLegendary + 1;
        }
        int last = rarities.length - 1;
        boolean anyLegendary = Arrays.stream(rarities).anyMatch(r -> r == Rarity.LEGENDARY);
        if (banner.guaranteedLegendary() && !anyLegendary) {
            rarities[last] = Rarity.LEGENDARY;
            pity[last] = true;
            sinceLegendary = 0;
        } else if (Arrays.stream(rarities).noneMatch(r -> r.atLeast(Rarity.RARE))) {
            rarities[last] = Rarity.RARE;
            pity[last] = true;
        }

        VipTier vip = GachaEconomy.vipFor(w.getFakeSpendPence());
        int bonusPercent = vip.level() * GachaEconomy.XP_BONUS_PER_VIP;
        Set<String> owned = new HashSet<>(pulls.ownedIdeaIds(user.getId()));
        String batchId = UUID.randomUUID().toString();
        List<PulledCard> cards = new ArrayList<>();
        List<GachaPull> rows = new ArrayList<>();
        int xpTotal = 0;
        int refundTotal = 0;
        for (int i = 0; i < rarities.length; i++) {
            List<Idea> candidates = pool.ofRarity(rarities[i]);
            Idea idea = candidates.get(random.nextInt(candidates.size()));
            boolean isNew = owned.add(idea.id());
            int xp = idea.rarity().xp() * (100 + bonusPercent) / 100;
            int refund = isNew ? 0 : idea.rarity().duplicateGems();
            xpTotal += xp;
            refundTotal += refund;
            cards.add(new PulledCard(IdeaView.of(idea), isNew, xp, refund, pity[i]));
            rows.add(new GachaPull(user.getId(), batchId, banner.name(), idea.id(), idea.rarity(), xp, !isNew));
        }
        pulls.saveAll(rows);

        addGems(w, refundTotal);
        w.setPullsSinceLegendary(sinceLegendary);
        w.setTotalPulls(w.getTotalPulls() + rarities.length);
        wallets.save(w);

        List<Achievement> unlocked = gamification.adjustXp(user, Activity.GACHA_REWARD, xpTotal);
        return new PullResult(batchId, banner.name(), cards, banner.cost(), refundTotal, xpTotal,
                unlocked.stream().map(a -> a.icon() + " " + a.title()).toList(), view(w, user));
    }

    // ---------------------------------------------------------------- helpers

    private Rarity roll(Banner banner) {
        int total = 0;
        for (Rarity r : Rarity.values()) total += banner.weight(r);
        int n = random.nextInt(total);
        for (Rarity r : Rarity.values()) {
            n -= banner.weight(r);
            if (n < 0) return r;
        }
        return Rarity.COMMON;
    }

    private GachaWallet walletFor(Long userId) {
        return wallets.findById(userId)
                .orElseGet(() -> wallets.save(new GachaWallet(userId, GachaEconomy.STARTER_GEMS)));
    }

    private static void addGems(GachaWallet w, long gems) {
        long next = w.getGems() + gems;
        if (next > GachaEconomy.MAX_GEMS) {
            throw ApiException.badRequest("Even whales have limits. Spend some gems first.");
        }
        w.setGems(next);
    }

    private WalletView view(GachaWallet w, User user) {
        VipTier vip = GachaEconomy.vipFor(w.getFakeSpendPence());
        Optional<VipTier> next = GachaEconomy.nextVip(vip);
        return new WalletView(w.getGems(), user.getXp(), w.getPullsSinceLegendary(), GachaEconomy.PITY_THRESHOLD,
                !LocalDate.now().equals(w.getLastDailyClaim()), GachaEconomy.DAILY_GEMS, vip.level(), vip.title(),
                vip.level() * GachaEconomy.XP_BONUS_PER_VIP, w.getFakeSpendPence(),
                next.map(VipTier::title).orElse(null), next.map(VipTier::minSpendPence).orElse(null),
                w.getTotalPulls(), GachaEconomy.GEMS_PER_XP);
    }
}
