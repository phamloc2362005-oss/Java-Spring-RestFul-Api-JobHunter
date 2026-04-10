package vn.locpham.jobhunter.domain.reponse.expertise;

import java.time.Instant;

public class ResCreateExpertiseDTO {
    private Long id;
    private String name;
    private String expertiseCategoryName;
    private String createdBy;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpertiseCategoryName() {
        return expertiseCategoryName;
    }

    public void setExpertiseCategoryName(String expertiseCategoryName) {
        this.expertiseCategoryName = expertiseCategoryName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
