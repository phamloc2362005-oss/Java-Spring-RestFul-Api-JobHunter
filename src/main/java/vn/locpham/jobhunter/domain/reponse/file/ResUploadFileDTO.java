package vn.locpham.jobhunter.domain.reponse.file;

import java.time.Instant;

public class ResUploadFileDTO {
    private String name;
    private Instant uploadedAt;

    public ResUploadFileDTO(String name, Instant uploadedAt) {
        this.name = name;
        this.uploadedAt = uploadedAt;
    }

    public ResUploadFileDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

}
