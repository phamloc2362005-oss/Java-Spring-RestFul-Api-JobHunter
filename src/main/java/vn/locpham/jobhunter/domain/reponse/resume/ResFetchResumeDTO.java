package vn.locpham.jobhunter.domain.reponse.resume;

import java.time.Instant;

import vn.locpham.jobhunter.util.constant.StatusEnum;

public class ResFetchResumeDTO {
    private Long id;
    private String email;
    private String url;
    private StatusEnum status;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private String company;
    private UserResume user;
    private JobResume job;

    public ResFetchResumeDTO() {
    }

    public ResFetchResumeDTO(Long id, String email, String url, StatusEnum status, Instant createdAt, Instant updatedAt,
            String createdBy, String updatedBy, UserResume user, JobResume job) {
        this.id = id;
        this.email = email;
        this.url = url;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.user = user;
        this.job = job;
    }

    public static class UserResume {
        private long id;
        private String name;

        public UserResume() {
        }

        public UserResume(long id, String name) {
            this.id = id;
            this.name = name;
        }

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

    }

    public static class JobResume {
        private long id;
        private String name;

        public JobResume() {
        }

        public JobResume(long id, String name) {
            this.id = id;
            this.name = name;
        }

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
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
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

    public UserResume getUser() {
        return user;
    }

    public void setUser(UserResume user) {
        this.user = user;
    }

    public JobResume getJob() {
        return job;
    }

    public void setJob(JobResume job) {
        this.job = job;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

}
