package com.tpximpact.trainingtool.social;

import jakarta.persistence.*;

@Entity
@Table(name = "post_like", uniqueConstraints = @UniqueConstraint(columnNames = {"postId", "userId"}))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    public PostLike() {}

    public PostLike(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getUserId() { return userId; }
}
