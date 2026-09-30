package com.tpximpact.trainingtool.gamification;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_achievement", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "code"}))
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String code;

    private Instant earnedAt = Instant.now();

    /** False until the front end has celebrated it with the user. */
    private boolean seen;

    public UserAchievement() {}

    public UserAchievement(Long userId, String code) {
        this.userId = userId;
        this.code = code;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getCode() { return code; }
    public Instant getEarnedAt() { return earnedAt; }
    public void setEarnedAt(Instant earnedAt) { this.earnedAt = earnedAt; }
    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
}
