package com.tpximpact.trainingtool.gacha;

/**
 * How rare an idea is. Weights are out of 1,000 per pull on the standard banner.
 * Rarer ideas give more XP, which is what makes spending pay off on the leaderboard.
 */
public enum Rarity {
    COMMON("Common", 1, 600, 3, 10),
    RARE("Rare", 2, 280, 8, 25),
    EPIC("Epic", 3, 100, 25, 60),
    LEGENDARY("Legendary", 4, 20, 100, 200);

    private final String label;
    private final int stars;
    private final int weight;
    private final int xp;
    private final int duplicateGems;

    Rarity(String label, int stars, int weight, int xp, int duplicateGems) {
        this.label = label;
        this.stars = stars;
        this.weight = weight;
        this.xp = xp;
        this.duplicateGems = duplicateGems;
    }

    public String label() { return label; }
    public int stars() { return stars; }
    /** Base weight out of 1,000 on the standard banner. */
    public int weight() { return weight; }
    /** XP awarded every time this rarity is pulled (before any VIP bonus). */
    public int xp() { return xp; }
    /** Gems refunded when you pull an idea you already own. */
    public int duplicateGems() { return duplicateGems; }

    public boolean atLeast(Rarity other) {
        return ordinal() >= other.ordinal();
    }
}
