package vn.locpham.jobhunter.domain.reponse.company;

import java.time.Instant;
import vn.locpham.jobhunter.domain.reponse.ResReviewDTO;

public class ResCompanyDTO {
    private long id;
    private String name;
    private String address;
    private String description;
    private String logo;
    private Instant createdAt;
    private Instant updatedAt;

    // Review statistics
    private Double averageRating;
    private Double recommendPercentage;
    private Long totalReviews;
    private ResReviewDTO latestReview;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Double getRecommendPercentage() {
        return recommendPercentage;
    }

    public void setRecommendPercentage(Double recommendPercentage) {
        this.recommendPercentage = recommendPercentage;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public ResReviewDTO getLatestReview() {
        return latestReview;
    }

    public void setLatestReview(ResReviewDTO latestReview) {
        this.latestReview = latestReview;
    }
}
