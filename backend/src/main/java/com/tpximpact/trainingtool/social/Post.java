package com.tpximpact.trainingtool.social;

import com.tpximpact.trainingtool.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "post")
public class Post {

    public enum Kind { GENERAL, ACHIEVEMENT, LEARNING, MILESTONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    private Kind kind = Kind.GENERAL;

    private Instant createdAt = Instant.now();

    public Post() {}

    public Post(User author, String content, Kind kind) {
        this.author = author;
        this.content = content;
        this.kind = kind;
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Kind getKind() { return kind; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
