package com.tpximpact.trainingtool.social;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("select count(l) from PostLike l, Post p where l.postId = p.id and p.author.id = :authorId and l.userId <> :authorId")
    long countLikesReceived(@Param("authorId") Long authorId);
}
