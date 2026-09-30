package com.tpximpact.trainingtool.gamification;

/** Things people do that earn experience points (XP). */
public enum Activity {
    SIGNED_UP(10),
    PROFILE_COMPLETED(20),
    ASSESSMENT_SAVED(5),
    JOURNAL_ADDED(15),
    LEARNING_ADDED(5),
    LEARNING_COMPLETED(30),
    REVIEW_SHARED(10),
    POST_CREATED(5),
    COMMENTED(2),
    FOLLOWED(2),
    PLAN_GENERATED(10),
    JOURNAL_EXPORTED(10),
    PIP_CHAT(1),
    DAILY_VISIT(3),
    ACHIEVEMENT_BONUS(0);

    private final int xp;

    Activity(int xp) {
        this.xp = xp;
    }

    public int xp() {
        return xp;
    }
}
