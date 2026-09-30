package com.tpximpact.trainingtool.learning;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A course, book, event or programme a person has done or plans to do. */
@Entity
@Table(name = "learning_item")
public class LearningItem {

    public enum Type { COURSE, BOOK, EVENT, PROGRAMME, OTHER }

    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.COURSE;

    @Column(nullable = false)
    private String title;

    private String provider;

    @Column(length = 1000)
    private String url;

    /** Id of the catalogue resource this came from, if any. */
    private String catalogueId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PLANNED;

    private LocalDate completedOn;

    /** 1 to 5 stars. */
    private Integer rating;

    @Column(length = 3000)
    private String review;

    /** Whether the review is visible to colleagues on the social feed and catalogue. */
    private boolean shared;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learning_item_ref", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "ref")
    private List<String> refs = new ArrayList<>();

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCatalogueId() { return catalogueId; }
    public void setCatalogueId(String catalogueId) { this.catalogueId = catalogueId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
    public List<String> getRefs() { return refs; }
    public void setRefs(List<String> refs) { this.refs = refs == null ? new ArrayList<>() : new ArrayList<>(refs); }
    public Instant getCreatedAt() { return createdAt; }
}
