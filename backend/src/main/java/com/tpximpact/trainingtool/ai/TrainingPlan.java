package com.tpximpact.trainingtool.ai;

import jakarta.persistence.*;

import java.time.Instant;

/** The most recent AI (or rule-based) training plan generated for a user, stored as JSON. */
@Entity
@Table(name = "training_plan")
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String json;

    private Instant createdAt = Instant.now();

    public TrainingPlan() {}

    public TrainingPlan(Long userId, String json) {
        this.userId = userId;
        this.json = json;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getJson() { return json; }
    public Instant getCreatedAt() { return createdAt; }
}
