package vn.locpham.jobhunter.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "review_votes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "review_id"})
)
public class ReviewVote {

    public enum VoteType {
        LIKE, DISLIKE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType type;

    // ── Getters & Setters ──────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }

    public VoteType getType() { return type; }
    public void setType(VoteType type) { this.type = type; }
}
