package com.tpximpact.trainingtool.gamification;

import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.security.CurrentUser;
import com.tpximpact.trainingtool.social.FollowRepository;
import com.tpximpact.trainingtool.social.Follow;
import com.tpximpact.trainingtool.user.User;
import com.tpximpact.trainingtool.user.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api")
public class GamificationController {

    private final GamificationService gamification;
    private final CurrentUser currentUser;
    private final UserRepository users;
    private final UserAchievementRepository achievements;
    private final ActivityLogRepository activity;
    private final FollowRepository follows;
    private final FrameworkService framework;

    public GamificationController(GamificationService gamification, CurrentUser currentUser, UserRepository users,
                                  UserAchievementRepository achievements, ActivityLogRepository activity,
                                  FollowRepository follows, FrameworkService framework) {
        this.gamification = gamification;
        this.currentUser = currentUser;
        this.users = users;
        this.achievements = achievements;
        this.activity = activity;
        this.follows = follows;
        this.framework = framework;
    }

    @GetMapping("/achievements")
    public Map<String, Object> mine() {
        User me = currentUser.get();
        return Map.of(
                "level", Levels.info(me.getXp()),
                "streakDays", me.getStreakDays(),
                "achievements", gamification.achievementsFor(me.getId()));
    }

    @GetMapping("/achievements/users/{userId}")
    public List<GamificationService.AchievementView> forUser(@PathVariable Long userId) {
        return gamification.achievementsFor(userId).stream().filter(GamificationService.AchievementView::earned).toList();
    }

    /** Achievements unlocked since the last call. The front end uses this to celebrate. */
    @PostMapping("/achievements/unseen")
    public List<GamificationService.AchievementView> unseen() {
        return gamification.takeUnseen(currentUser.id());
    }

    public record LeaderboardRow(int rank, Long userId, String displayName, String avatarColor, String roleName,
                                 String capability, int xp, long periodXp, int level, String levelTitle,
                                 long badges, int streakDays, boolean isMe) {}

    /**
     * @param scope  "all" (default), "friends" (people you follow plus you) or "capability" (your capability)
     * @param period "all" (default), "month" or "week"
     */
    @GetMapping("/leaderboard")
    public List<LeaderboardRow> leaderboard(@RequestParam(defaultValue = "all") String scope,
                                            @RequestParam(defaultValue = "all") String period) {
        User me = currentUser.get();
        List<User> pool = new ArrayList<>(users.findAll());
        if ("friends".equals(scope)) {
            Set<Long> ids = new HashSet<>();
            ids.add(me.getId());
            follows.findByFollowerId(me.getId()).stream().map(Follow::getFolloweeId).forEach(ids::add);
            pool.removeIf(u -> !ids.contains(u.getId()));
        } else if ("capability".equals(scope)) {
            String cap = framework.findRole(me.getRoleId()).map(r -> r.capability()).orElse(null);
            pool.removeIf(u -> cap == null || !cap.equals(framework.findRole(u.getRoleId()).map(r -> r.capability()).orElse(null)));
        }
        Instant since = switch (period) {
            case "week" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> null;
        };
        Map<Long, Long> periodXp = new HashMap<>();
        for (User u : pool) {
            periodXp.put(u.getId(), since == null ? u.getXp() : activity.sumXpSince(u.getId(), since));
        }
        pool.sort(Comparator.comparingLong((User u) -> periodXp.get(u.getId())).reversed()
                .thenComparing(User::getDisplayName));
        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 0;
        for (User u : pool) {
            rank++;
            if (rank > 100) break;
            var role = framework.findRole(u.getRoleId());
            int level = Levels.levelFor(u.getXp());
            rows.add(new LeaderboardRow(rank, u.getId(), u.getDisplayName(), u.getAvatarColor(),
                    role.map(r -> r.name()).orElse(null), role.map(r -> r.capability()).orElse(null),
                    u.getXp(), periodXp.get(u.getId()), level, Levels.titleFor(level),
                    achievements.countByUserId(u.getId()), u.getStreakDays(), u.getId().equals(me.getId())));
        }
        return rows;
    }

    @GetMapping("/activity")
    public List<ActivityLog> recentActivity() {
        return activity.findTop20ByUserIdOrderByCreatedAtDesc(currentUser.id());
    }
}
