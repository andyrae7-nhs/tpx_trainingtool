package com.tpximpact.trainingtool.gacha;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface GachaPullRepository extends JpaRepository<GachaPull, Long> {

    /** Newest first. 100 rows = the last 10 ten-pulls. */
    List<GachaPull> findTop100ByUserIdOrderByIdDesc(Long userId);

    interface OwnedIdea {
        String getIdeaId();
        long getCopies();
        Instant getFirstPulledAt();
    }

    @Query("select p.ideaId as ideaId, count(p) as copies, min(p.createdAt) as firstPulledAt "
            + "from GachaPull p where p.userId = :userId group by p.ideaId")
    List<OwnedIdea> ownedBy(@Param("userId") Long userId);

    @Query("select distinct p.ideaId from GachaPull p where p.userId = :userId")
    List<String> ownedIdeaIds(@Param("userId") Long userId);
}
