package com.tpximpact.trainingtool.gamification;

import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.gamification.AchievementCatalog.Achievement;
import com.tpximpact.trainingtool.gamification.AchievementCatalog.UserStats;
import com.tpximpact.trainingtool.journal.JournalEntry;
import com.tpximpact.trainingtool.journal.JournalEntryRepository;
import com.tpximpact.trainingtool.learning.LearningItem;
import com.tpximpact.trainingtool.learning.LearningItemRepository;
import com.tpximpact.trainingtool.progression.SelfAssessmentRepository;
import com.tpximpact.trainingtool.social.FollowRepository;
import com.tpximpact.trainingtool.social.Post;
import com.tpximpact.trainingtool.social.PostLikeRepository;
import com.tpximpact.trainingtool.social.PostRepository;
import com.tpximpact.trainingtool.user.User;
import com.tpximpact.trainingtool.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** Awards XP, keeps streaks and unlocks achievements. */
@Service
public class GamificationService {

    private final UserRepository users;
    private final ActivityLogRepository activity;
    private final UserAchievementRepository achievements;
    private final JournalEntryRepository journal;
    private final LearningItemRepository learning;
    private final SelfAssessmentRepository assessments;
    private final FollowRepository follows;
    private final PostRepository posts;
    private final PostLikeRepository likes;
    private final FrameworkService framework;

    public GamificationService(UserRepository users, ActivityLogRepository activity,
                               UserAchievementRepository achievements, JournalEntryRepository journal,
                               LearningItemRepository learning, SelfAssessmentRepository assessments,
                               FollowRepository follows, PostRepository posts, PostLikeRepository likes,
                               FrameworkService framework) {
        this.users = users;
        this.activity = activity;
        this.achievements = achievements;
        this.journal = journal;
        this.learning = learning;
        this.assessments = assessments;
        this.follows = follows;
        this.posts = posts;
        this.likes = likes;
        this.framework = framework;
    }

    /** Record an activity: award XP, then check for newly unlocked achievements. */
    @Transactional
    public List<Achievement> record(User user, Activity act) {
        if (act.xp() > 0) {
            user.setXp(user.getXp() + act.xp());
        }
        activity.save(new ActivityLog(user.getId(), act, act.xp()));
        users.save(user);
        return evaluate(user);
    }

    /** Update the daily streak. Call once per authenticated session start. */
    @Transactional
    public void touch(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastActiveDate();
        if (today.equals(last)) return;
        if (last != null && last.plusDays(1).equals(today)) {
            user.setStreakDays(user.getStreakDays() + 1);
        } else {
            user.setStreakDays(1);
        }
        user.setLastActiveDate(today);
        record(user, Activity.DAILY_VISIT);
    }

    @Transactional
    public List<Achievement> evaluate(User user) {
        UserStats stats = stats(user);
        List<Achievement> unlocked = new ArrayList<>();
        // Loop because bonus XP from one badge can unlock an XP badge.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Achievement a : AchievementCatalog.ALL) {
                if (achievements.existsByUserIdAndCode(user.getId(), a.code())) continue;
                if (!a.rule().test(stats)) continue;
                achievements.save(new UserAchievement(user.getId(), a.code()));
                if (a.bonusXp() > 0) {
                    user.setXp(user.getXp() + a.bonusXp());
                    activity.save(new ActivityLog(user.getId(), Activity.ACHIEVEMENT_BONUS, a.bonusXp()));
                }
                if (!"WELCOME".equals(a.code())) {
                    posts.save(new Post(user, "unlocked the " + a.icon() + " " + a.title() + " badge: " + a.description().toLowerCase(Locale.UK),
                            Post.Kind.ACHIEVEMENT));
                }
                unlocked.add(a);
                changed = true;
            }
            if (changed) {
                users.save(user);
                stats = stats(user);
            }
        }
        return unlocked;
    }

    public UserStats stats(User user) {
        Long id = user.getId();
        List<JournalEntry> entries = journal.findByUserIdOrderByEntryDateDescIdDesc(id);
        long distinctRefs = entries.stream().flatMap(e -> e.getRefs().stream()).distinct().count();
        return new UserStats(
                user.isOnboarded(),
                assessableItemCount(user),
                assessments.countByUserId(id),
                activity.countByUserIdAndActivity(id, Activity.PLAN_GENERATED),
                entries.size(),
                distinctRefs,
                activity.countByUserIdAndActivity(id, Activity.JOURNAL_EXPORTED),
                learning.countByUserId(id),
                learning.countByUserIdAndStatus(id, LearningItem.Status.COMPLETED),
                learning.countByUserIdAndStatusAndType(id, LearningItem.Status.COMPLETED, LearningItem.Type.BOOK),
                learning.countByUserIdAndStatusAndType(id, LearningItem.Status.COMPLETED, LearningItem.Type.EVENT),
                learning.countByUserIdAndSharedTrueAndReviewIsNotNull(id),
                follows.countByFollowerId(id),
                posts.countByAuthorId(id),
                likes.countLikesReceived(id),
                activity.countByUserIdAndActivity(id, Activity.PIP_CHAT),
                user.getStreakDays(),
                user.getXp());
    }

    /** Number of skills, behaviours and impact items a person can rate themselves against. */
    public int assessableItemCount(User user) {
        if (!user.isOnboarded()) return 0;
        return framework.findRole(user.getRoleId()).map(role -> {
            long skills = role.skills().stream()
                    .filter(rs -> framework.expectedSkillLevel(rs, user.getTargetGrade()) != null
                            || framework.expectedSkillLevel(rs, user.getCurrentGrade()) != null)
                    .count();
            return (int) skills + framework.behaviours().size() + framework.impacts().size();
        }).orElse(0);
    }

    public record AchievementView(String code, String title, String description, String icon, String category,
                                  int bonusXp, boolean earned, String earnedAt) {}

    public List<AchievementView> achievementsFor(Long userId) {
        Map<String, UserAchievement> earned = new HashMap<>();
        achievements.findByUserIdOrderByEarnedAtDesc(userId).forEach(a -> earned.put(a.getCode(), a));
        return AchievementCatalog.ALL.stream().map(a -> {
            UserAchievement ua = earned.get(a.code());
            return new AchievementView(a.code(), a.title(), a.description(), a.icon(), a.category().name(),
                    a.bonusXp(), ua != null, ua == null ? null : ua.getEarnedAt().toString());
        }).toList();
    }

    @Transactional
    public List<AchievementView> takeUnseen(Long userId) {
        List<UserAchievement> unseen = achievements.findByUserIdAndSeenFalse(userId);
        List<AchievementView> out = new ArrayList<>();
        for (UserAchievement ua : unseen) {
            ua.setSeen(true);
            AchievementCatalog.find(ua.getCode()).ifPresent(a -> out.add(new AchievementView(a.code(), a.title(),
                    a.description(), a.icon(), a.category().name(), a.bonusXp(), true, ua.getEarnedAt().toString())));
        }
        achievements.saveAll(unseen);
        return out;
    }
}
