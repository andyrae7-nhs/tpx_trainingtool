package com.tpximpact.trainingtool.gacha;

import jakarta.persistence.*;

import java.time.LocalDate;

/** A person's gems, pity counter and (pretend) spending. One row per user, created on first visit. */
@Entity
@Table(name = "gacha_wallet")
public class GachaWallet {

    @Id
    private Long userId;

    private long gems;

    /** Pulls since the last Legendary. Hitting the pity threshold guarantees one. */
    private int pullsSinceLegendary;

    private long totalPulls;

    /** Pretend money "spent" in the gem shop, in pence. No real payment is ever taken. */
    private long fakeSpendPence;

    private LocalDate lastDailyClaim;

    @Version
    private Long version;

    public GachaWallet() {}

    public GachaWallet(Long userId, long gems) {
        this.userId = userId;
        this.gems = gems;
    }

    public Long getUserId() { return userId; }
    public long getGems() { return gems; }
    public void setGems(long gems) { this.gems = gems; }
    public int getPullsSinceLegendary() { return pullsSinceLegendary; }
    public void setPullsSinceLegendary(int pullsSinceLegendary) { this.pullsSinceLegendary = pullsSinceLegendary; }
    public long getTotalPulls() { return totalPulls; }
    public void setTotalPulls(long totalPulls) { this.totalPulls = totalPulls; }
    public long getFakeSpendPence() { return fakeSpendPence; }
    public void setFakeSpendPence(long fakeSpendPence) { this.fakeSpendPence = fakeSpendPence; }
    public LocalDate getLastDailyClaim() { return lastDailyClaim; }
    public void setLastDailyClaim(LocalDate lastDailyClaim) { this.lastDailyClaim = lastDailyClaim; }
}
