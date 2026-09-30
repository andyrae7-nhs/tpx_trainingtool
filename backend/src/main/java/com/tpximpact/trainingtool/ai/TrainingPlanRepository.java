package com.tpximpact.trainingtool.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {
    Optional<TrainingPlan> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
