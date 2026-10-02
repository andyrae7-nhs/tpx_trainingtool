package com.tpximpact.trainingtool.gamification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserIdOrderByEarnedAtDesc(Long userId);

    boolean existsByUserIdAndCode(Long userId, String code);

    long countByUserId(Long userId);

    List<UserAchievement> findByUserIdAndSeenFalse(Long userId);
}
