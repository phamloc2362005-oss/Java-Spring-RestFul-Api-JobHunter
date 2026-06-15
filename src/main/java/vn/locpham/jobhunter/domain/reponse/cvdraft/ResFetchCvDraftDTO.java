package vn.locpham.jobhunter.domain.reponse.cvdraft;

import java.time.Instant;

public class ResFetchCvDraftDTO {
    private Long id;
    private String title;
    private String cvJsonData;
    private String templateId;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCvJsonData() { return cvJsonData; }
    public void setCvJsonData(String cvJsonData) { this.cvJsonData = cvJsonData; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
