package vn.locpham.jobhunter.domain.reponse.expertise;

import java.time.Instant;

public class ResUpdateExpertiseDTO {
    private Long id;
    private String name;
    private String expertiseCategoryName;
    private Instant updatedAt;
    private String updatedBy;

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

}
