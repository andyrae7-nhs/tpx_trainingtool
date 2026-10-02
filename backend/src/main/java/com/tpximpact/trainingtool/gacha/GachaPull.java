package com.tpximpact.trainingtool.gacha;

import jakarta.persistence.*;

import java.time.Instant;

/** One card from one pull. A 10-pull saves 10 rows sharing a batchId. */
@Entity
@Table(name = "gacha_pull", indexes = {
        @Index(name = "idx_gacha_pull_user", columnList = "userId"),
        @Index(name = "idx_gacha_pull_user_idea", columnList = "userId,ideaId")})
public class GachaPull {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 36)
    private String batchId;

    /** Banner used: STANDARD, BOOSTED or WHALE. Stored as text so new banners need no migration. */
    @Column(nullable = false, length = 20)
    private String banner;

    @Column(nullable = false)
    private String ideaId;

    /** Stored as text (not an enum column) so it survives changes to the rarity list. */
    @Column(nullable = false, length = 20)
    private String rarity;

    private int xp;

    private boolean duplicate;

    private Instant createdAt = Instant.now();

    public GachaPull() {}

    public GachaPull(Long userId, String batchId, String banner, String ideaId, Rarity rarity, int xp, boolean duplicate) {
        this.userId = userId;
        this.batchId = batchId;
        this.banner = banner;
        this.ideaId = ideaId;
        this.rarity = rarity.name();
        this.xp = xp;
        this.duplicate = duplicate;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getBatchId() { return batchId; }
    public String getBanner() { return banner; }
    public String getIdeaId() { return ideaId; }
    public String getRarity() { return rarity; }
    public int getXp() { return xp; }
    public boolean isDuplicate() { return duplicate; }
    public Instant getCreatedAt() { return createdAt; }
}
