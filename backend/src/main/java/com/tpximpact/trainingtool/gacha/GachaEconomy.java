package com.tpximpact.trainingtool.gacha;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The numbers behind the gacha. Everything is pretend: gems are free, and the shop "prices" are
 * satire. Nobody is ever charged. The pay-to-win part is that pulls award XP, XP ranks you on the
 * leaderboard, and spending (pretend) money raises your VIP tier, which boosts XP from pulls.
 */
public final class GachaEconomy {
    private GachaEconomy() {}

    public static final int PULLS_PER_BATCH = 10;
    public static final int STARTER_GEMS = 1_000;
    public static final int DAILY_GEMS = 300;
    /** Pulls without a Legendary before one is guaranteed. */
    public static final int PITY_THRESHOLD = 50;
    /** Gems you get for each XP you trade in. */
    public static final int GEMS_PER_XP = 5;
    public static final int MIN_XP_EXCHANGE = 10;
    /** Stops runaway numbers from someone clicking "buy" in a loop. */
    public static final long MAX_GEMS = 10_000_000L;

    public enum Banner {
        STANDARD("Standard 10-pull", 1_000, 1, false,
                "Ten ideas at standard odds. At least one Rare or better."),
        BOOSTED("Boosted 10-pull", 1_500, 3, false,
                "Triple Epic and Legendary odds. For people who want it more."),
        WHALE("Whale 10-pull", 5_000, 3, true,
                "Boosted odds plus a guaranteed Legendary. Money can buy happiness.");

        private final String label;
        private final int cost;
        private final int rareBoost;
        private final boolean guaranteedLegendary;
        private final String description;

        Banner(String label, int cost, int rareBoost, boolean guaranteedLegendary, String description) {
            this.label = label;
            this.cost = cost;
            this.rareBoost = rareBoost;
            this.guaranteedLegendary = guaranteedLegendary;
            this.description = description;
        }

        public String label() { return label; }
        public int cost() { return cost; }
        public boolean guaranteedLegendary() { return guaranteedLegendary; }
        public String description() { return description; }

        /** Weight for a rarity on this banner. Epic and Legendary get multiplied by the boost. */
        public int weight(Rarity r) {
            return r.atLeast(Rarity.EPIC) ? r.weight() * rareBoost : r.weight();
        }

        /** Percentage chance of each rarity per pull (ignoring pity and guarantees). */
        public Map<Rarity, Double> odds() {
            int total = 0;
            for (Rarity r : Rarity.values()) total += weight(r);
            Map<Rarity, Double> out = new EnumMap<>(Rarity.class);
            for (Rarity r : Rarity.values()) out.put(r, Math.round(weight(r) * 1000.0 / total) / 10.0);
            return out;
        }

        public static Optional<Banner> parse(String code) {
            if (code == null) return Optional.of(STANDARD);
            for (Banner b : values()) if (b.name().equalsIgnoreCase(code.trim())) return Optional.of(b);
            return Optional.empty();
        }
    }

    public record GemPack(String code, String name, long pricePence, long gems, String tagline) {}

    public static final List<GemPack> SHOP = List.of(
            new GemPack("pocket-change", "Pocket change", 99, 300, "Less than a coffee. Probably."),
            new GemPack("consultants-coffer", "Consultant's coffer", 999, 3_300, "Billable to the client? (No.)"),
            new GemPack("partner-track", "Partner track bundle", 4_999, 18_000, "Skip the promotion panel."),
            new GemPack("whale-of-a-time", "Whale of a time", 9_999, 40_000, "Best value! Unlocks VIP 4 instantly.")
    );

    public static Optional<GemPack> pack(String code) {
        return SHOP.stream().filter(p -> p.code().equals(code)).findFirst();
    }

    public record VipTier(int level, String title, long minSpendPence) {}

    public static final List<VipTier> VIP = List.of(
            new VipTier(0, "Free-to-play", 0),
            new VipTier(1, "Minnow", 99),
            new VipTier(2, "Dolphin", 999),
            new VipTier(3, "Shark", 4_999),
            new VipTier(4, "Whale", 9_999),
            new VipTier(5, "Leviathan", 49_999));

    /** Extra XP from pulls per VIP level, as a percentage. VIP 5 = +50%. */
    public static final int XP_BONUS_PER_VIP = 10;

    public static VipTier vipFor(long spendPence) {
        VipTier tier = VIP.get(0);
        for (VipTier t : VIP) if (spendPence >= t.minSpendPence()) tier = t;
        return tier;
    }

    public static Optional<VipTier> nextVip(VipTier current) {
        return current.level() + 1 < VIP.size() ? Optional.of(VIP.get(current.level() + 1)) : Optional.empty();
    }
}
