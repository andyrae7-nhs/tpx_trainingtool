package com.tpximpact.trainingtool.social;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc(Pageable page);

    List<Post> findByAuthorIdInOrderByCreatedAtDesc(Collection<Long> authorIds, Pageable page);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable page);

    long countByAuthorId(Long authorId);
}
