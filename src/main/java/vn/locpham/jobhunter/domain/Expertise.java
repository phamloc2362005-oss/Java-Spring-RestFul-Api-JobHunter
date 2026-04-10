package vn.locpham.jobhunter.domain;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import vn.locpham.jobhunter.util.SecurityUtils;

@Entity
@Table(name = "expertises")
public class Expertise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "name  không được để trống")
    private String name;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "expertises")
    @JsonIgnore
    private List<Job> jobs;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ExpertiseCategory expertiseCategory;

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
        this.createdBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : "";
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedAt = Instant.now();
        this.updatedBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : "";
    }

    public ExpertiseCategory getExpertiseCategory() {
        return expertiseCategory;
    }

    public void setExpertiseCategory(ExpertiseCategory expertiseCategory) {
        this.expertiseCategory = expertiseCategory;
    }
}
