package com.tpximpact.trainingtool.gamification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    long countByUserIdAndActivity(Long userId, Activity activity);

    List<ActivityLog> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(a.xp), 0) from ActivityLog a where a.userId = :userId and a.createdAt >= :since")
    long sumXpSince(@Param("userId") Long userId, @Param("since") Instant since);
}
