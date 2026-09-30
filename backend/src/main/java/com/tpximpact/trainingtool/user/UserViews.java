package com.tpximpact.trainingtool.user;

import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.gamification.Levels;
import com.tpximpact.trainingtool.social.FollowRepository;
import org.springframework.stereotype.Component;

/** Builds the JSON views of a user. */
@Component
public class UserViews {

    private final FrameworkService framework;
    private final FollowRepository follows;

    public UserViews(FrameworkService framework, FollowRepository follows) {
        this.framework = framework;
        this.follows = follows;
    }

    public record UserView(Long id, String email, String displayName, String bio, String avatarColor,
                           String roleId, String roleName, String capability, String practice,
                           String currentGrade, String currentGradeName, String targetGrade, String targetGradeName,
                           boolean onboarded, Levels.LevelInfo level, int streakDays,
                           long followers, long following, Boolean isFollowing, Boolean isMe) {}

    public record UserSummary(Long id, String displayName, String avatarColor, String roleName, String capability,
                              String currentGradeName, int level) {}

    public UserView full(User u, Long viewerId) {
        var role = framework.findRole(u.getRoleId());
        boolean me = u.getId().equals(viewerId);
        Boolean isFollowing = viewerId == null || me ? null
                : follows.findByFollowerIdAndFolloweeId(viewerId, u.getId()).isPresent();
        return new UserView(u.getId(), me ? u.getEmail() : null, u.getDisplayName(), u.getBio(), u.getAvatarColor(),
                u.getRoleId(), role.map(r -> r.name()).orElse(null), role.map(r -> r.capability()).orElse(null),
                role.map(r -> r.practice()).orElse(null),
                u.getCurrentGrade(), gradeName(u.getCurrentGrade()), u.getTargetGrade(), gradeName(u.getTargetGrade()),
                u.isOnboarded(), Levels.info(u.getXp()), u.getStreakDays(),
                follows.countByFolloweeId(u.getId()), follows.countByFollowerId(u.getId()), isFollowing, me);
    }

    public UserSummary summary(User u) {
        var role = framework.findRole(u.getRoleId());
        return new UserSummary(u.getId(), u.getDisplayName(), u.getAvatarColor(),
                role.map(r -> r.name()).orElse(null), role.map(r -> r.capability()).orElse(null),
                gradeName(u.getCurrentGrade()), Levels.levelFor(u.getXp()));
    }

    private String gradeName(String code) {
        if (code == null || !framework.isGrade(code)) return null;
        return framework.grade(code).name();
    }
}
