package com.tpximpact.trainingtool.progression;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SelfAssessmentRepository extends JpaRepository<SelfAssessment, Long> {
    List<SelfAssessment> findByUserId(Long userId);

    Optional<SelfAssessment> findByUserIdAndItemTypeAndItemId(Long userId, SelfAssessment.ItemType type, String itemId);

    long countByUserId(Long userId);
}
