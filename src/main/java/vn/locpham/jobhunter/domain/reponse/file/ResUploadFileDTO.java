package vn.locpham.jobhunter.domain.reponse.file;

import java.time.Instant;

public class ResUploadFileDTO {
    private String fileName;
    private Instant uploadedAt;

    public ResUploadFileDTO(String name, Instant uploadedAt) {
        this.fileName = name;
        this.uploadedAt = uploadedAt;
    }

    public ResUploadFileDTO() {
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

}
