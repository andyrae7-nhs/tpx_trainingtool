package com.tpximpact.trainingtool.progression;

import jakarta.persistence.*;

import java.time.Instant;

/** A person's own view of where they are for one framework item. */
@Entity
@Table(name = "self_assessment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "itemType", "itemId"}))
public class SelfAssessment {

    public enum ItemType { SKILL, BEHAVIOUR, IMPACT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    @Column(nullable = false)
    private String itemId;

    /** Skill level name (Learner..Leader) for skills; grade code (G6..G12) for behaviours and impact. */
    @Column(nullable = false)
    private String level;

    @Column(length = 1000)
    private String note;

    private Instant updatedAt = Instant.now();

    public SelfAssessment() {}

    public SelfAssessment(Long userId, ItemType itemType, String itemId) {
        this.userId = userId;
        this.itemType = itemType;
        this.itemId = itemId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ItemType getItemType() { return itemType; }
    public String getItemId() { return itemId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
