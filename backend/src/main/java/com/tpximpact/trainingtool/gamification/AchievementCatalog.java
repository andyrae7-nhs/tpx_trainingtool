package com.tpximpact.trainingtool.gamification;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Every badge in TPX Grow and the rule for earning it. */
public final class AchievementCatalog {
    private AchievementCatalog() {}

    public enum Category { GETTING_STARTED, EVIDENCE, LEARNING, SOCIAL, MASTERY }

    public record Achievement(String code, String title, String description, String icon, Category category,
                              int bonusXp, Predicate<UserStats> rule) {}

    public static final List<Achievement> ALL = List.of(
            new Achievement("WELCOME", "Hello, world", "Join TPX Grow", "👋", Category.GETTING_STARTED, 10,
                    s -> true),
            new Achievement("ROLE_SET", "Know where you're going", "Choose your role, current grade and target grade", "🧭",
                    Category.GETTING_STARTED, 20, UserStats::onboarded),
            new Achievement("SELF_ASSESSED", "Mirror, mirror", "Rate yourself against every skill, behaviour and impact for your role", "🪞",
                    Category.GETTING_STARTED, 50, s -> s.itemsInFramework() > 0 && s.assessedItems() >= s.itemsInFramework()),
            new Achievement("GAP_HUNTER", "Gap hunter", "Generate your first personalised training plan", "🎯",
                    Category.GETTING_STARTED, 20, s -> s.plansGenerated() >= 1),

            new Achievement("FIRST_EVIDENCE", "Receipts", "Add your first journal entry", "🧾", Category.EVIDENCE, 15,
                    s -> s.journalEntries() >= 1),
            new Achievement("EVIDENCE_10", "Paper trail", "Add 10 journal entries", "📚", Category.EVIDENCE, 50,
                    s -> s.journalEntries() >= 10),
            new Achievement("FULL_HOUSE", "Full house", "Tag evidence against 10 different framework items", "🏠", Category.EVIDENCE, 75,
                    s -> s.distinctEvidenceRefs() >= 10),
            new Achievement("EXPORTER", "Assessment ready", "Export your journal for your progression assessment", "📤",
                    Category.EVIDENCE, 25, s -> s.exports() >= 1),

            new Achievement("FIRST_LEARNING", "Curious cat", "Log your first course, book, event or programme", "🐈",
                    Category.LEARNING, 10, s -> s.learningItems() >= 1),
            new Achievement("FINISHER", "Finisher", "Complete 5 pieces of learning", "🏁", Category.LEARNING, 50,
                    s -> s.learningCompleted() >= 5),
            new Achievement("BOOKWORM", "Bookworm", "Finish 3 books", "🐛", Category.LEARNING, 40,
                    s -> s.booksCompleted() >= 3),
            new Achievement("OUT_AND_ABOUT", "Out and about", "Attend 3 events", "🎟️", Category.LEARNING, 40,
                    s -> s.eventsAttended() >= 3),
            new Achievement("CRITIC", "Critic's choice", "Share 5 reviews with colleagues", "⭐", Category.LEARNING, 40,
                    s -> s.reviewsShared() >= 5),

            new Achievement("BETTER_TOGETHER", "Better together", "Follow 3 colleagues", "🤝", Category.SOCIAL, 15,
                    s -> s.following() >= 3),
            new Achievement("FIRST_POST", "Hot off the press", "Write your first post", "📰", Category.SOCIAL, 10,
                    s -> s.posts() >= 1),
            new Achievement("CROWD_PLEASER", "Crowd pleaser", "Get 10 likes from colleagues", "🎉", Category.SOCIAL, 50,
                    s -> s.likesReceived() >= 10),
            new Achievement("PIP_PAL", "Pip's pal", "Chat with Pip 5 times", "💬", Category.SOCIAL, 10,
                    s -> s.pipChats() >= 5),

            new Achievement("ON_A_ROLL", "On a roll", "Visit 7 days in a row", "🔥", Category.MASTERY, 50,
                    s -> s.streakDays() >= 7),
            new Achievement("LEVEL_5", "In full bloom", "Reach level 5", "🌸", Category.MASTERY, 0,
                    s -> Levels.levelFor(s.xp()) >= 5),
            new Achievement("XP_2000", "Evergreen", "Earn 2,000 XP", "🌲", Category.MASTERY, 0,
                    s -> s.xp() >= 2000)
    );

    public static Optional<Achievement> find(String code) {
        return ALL.stream().filter(a -> a.code().equals(code)).findFirst();
    }

    public record UserStats(boolean onboarded, int itemsInFramework, long assessedItems, long plansGenerated,
                            long journalEntries, long distinctEvidenceRefs, long exports, long learningItems,
                            long learningCompleted, long booksCompleted, long eventsAttended, long reviewsShared,
                            long following, long posts, long likesReceived, long pipChats, int streakDays, int xp) {}
}
