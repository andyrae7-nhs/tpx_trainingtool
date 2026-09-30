package com.tpximpact.trainingtool.journal;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A piece of evidence for the end-of-year progression assessment. */
@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    /** What happened and what you did. */
    @Column(length = 5000)
    private String body;

    /** The result or impact of what you did. */
    @Column(length = 3000)
    private String impact;

    private LocalDate entryDate = LocalDate.now();

    /** References into the framework: "SKILL:&lt;id&gt;", "BEHAVIOUR:&lt;id&gt;" or "IMPACT:&lt;id&gt;". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "journal_entry_ref", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "ref")
    private List<String> refs = new ArrayList<>();

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public List<String> getRefs() { return refs; }
    public void setRefs(List<String> refs) { this.refs = refs == null ? new ArrayList<>() : new ArrayList<>(refs); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
