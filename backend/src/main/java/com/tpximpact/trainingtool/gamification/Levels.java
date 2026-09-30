package com.tpximpact.trainingtool.gamification;

import java.util.List;

/** XP levels. Level n needs 50 * n * (n - 1) XP: 0, 100, 300, 600, 1000, 1500... */
public final class Levels {
    private Levels() {}

    private static final List<String> TITLES = List.of(
            "Seedling", "Sprout", "Sapling", "Grower", "In bloom", "Branching out", "Deep roots", "Evergreen", "Mighty oak", "Forest");

    public static int levelFor(int xp) {
        int level = 1;
        while (xpForLevel(level + 1) <= xp) level++;
        return level;
    }

    public static int xpForLevel(int level) {
        return 50 * level * (level - 1);
    }

    public static String titleFor(int level) {
        return TITLES.get(Math.min(level, TITLES.size()) - 1);
    }

    public record LevelInfo(int level, String title, int xp, int currentLevelXp, int nextLevelXp) {}

    public static LevelInfo info(int xp) {
        int level = levelFor(xp);
        return new LevelInfo(level, titleFor(level), xp, xpForLevel(level), xpForLevel(level + 1));
    }
}
