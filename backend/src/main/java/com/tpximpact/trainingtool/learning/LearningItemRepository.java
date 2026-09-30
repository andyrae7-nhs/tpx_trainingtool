package com.tpximpact.trainingtool.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningItemRepository extends JpaRepository<LearningItem, Long> {
    List<LearningItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, LearningItem.Status status);

    long countByUserIdAndStatusAndType(Long userId, LearningItem.Status status, LearningItem.Type type);

    long countByUserIdAndSharedTrueAndReviewIsNotNull(Long userId);

    List<LearningItem> findBySharedTrueAndCatalogueIdOrderByCreatedAtDesc(String catalogueId);

    List<LearningItem> findTop30BySharedTrueOrderByCreatedAtDesc();
}
