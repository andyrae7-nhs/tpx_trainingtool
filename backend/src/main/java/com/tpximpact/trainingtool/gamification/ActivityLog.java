package com.tpximpact.trainingtool.gamification;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Activity activity;

    private int xp;

    private Instant createdAt = Instant.now();

    public ActivityLog() {}

    public ActivityLog(Long userId, Activity activity, int xp) {
        this.userId = userId;
        this.activity = activity;
        this.xp = xp;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Activity getActivity() { return activity; }
    public int getXp() { return xp; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
