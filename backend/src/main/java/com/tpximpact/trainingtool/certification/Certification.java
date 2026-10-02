package com.tpximpact.trainingtool.certification;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A professional certification someone holds or is working towards, e.g. AWS Solutions Architect. */
@Entity
@Table(name = "certification", indexes = @Index(name = "idx_certification_user", columnList = "userId"))
public class Certification {

    /** Stored as text (not an enum column) so new statuses need no database change. */
    public static final String EARNED = "EARNED";
    public static final String IN_PROGRESS = "IN_PROGRESS";

    /** How many days before expiry a certification counts as "expiring soon". */
    public static final int EXPIRING_SOON_DAYS = 90;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 120)
    private String issuer;

    @Column(length = 20, nullable = false)
    private String status = EARNED;

    @Column(length = 120)
    private String credentialId;

    @Column(length = 500)
    private String credentialUrl;

    private LocalDate issuedOn;

    private LocalDate expiresOn;

    @Column(length = 1000)
    private String notes;

    /** Framework items this certification supports: "SKILL:&lt;id&gt;", "BEHAVIOUR:&lt;id&gt;" or "IMPACT:&lt;id&gt;". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "certification_ref", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "ref")
    private List<String> refs = new ArrayList<>();

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public enum Expiry { NO_EXPIRY, ACTIVE, EXPIRING_SOON, EXPIRED }

    public Expiry expiry(LocalDate today) {
        if (expiresOn == null) return Expiry.NO_EXPIRY;
        if (expiresOn.isBefore(today)) return Expiry.EXPIRED;
        if (!expiresOn.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) return Expiry.EXPIRING_SOON;
        return Expiry.ACTIVE;
    }

    /** Earned and not expired: fair to cite as evidence. */
    public boolean isCurrent(LocalDate today) {
        return EARNED.equals(status) && expiry(today) != Expiry.EXPIRED;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
    public String getCredentialUrl() { return credentialUrl; }
    public void setCredentialUrl(String credentialUrl) { this.credentialUrl = credentialUrl; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public void setIssuedOn(LocalDate issuedOn) { this.issuedOn = issuedOn; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<String> getRefs() { return refs; }
    public void setRefs(List<String> refs) { this.refs = refs == null ? new ArrayList<>() : new ArrayList<>(refs); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
