package vn.locpham.jobhunter.domain.reponse;

import vn.locpham.jobhunter.domain.Job;

/**
 * DTO để trả về job recommendation với score và matching details
 */
public class JobRecommendationDTO {
    private Long jobId;
    private String jobName;
    private String companyName;
    private String location;
    private double salary;
    private String level;
    private String expertise;
    private int matchScore;
    private String matchSummary;

    public JobRecommendationDTO(Job job, int score, String matchSummary) {
        this.jobId = job.getId();
        this.jobName = job.getName();
        this.companyName = job.getCompany() != null ? job.getCompany().getName() : "Unknown";
        this.location = job.getLocation();
        this.salary = job.getSalary();
        this.level = job.getLevel() != null ? job.getLevel().toString() : "N/A";
        this.expertise = job.getExpertise() != null ? job.getExpertise().getName() : "N/A";
        this.matchScore = score;
        this.matchSummary = matchSummary;
    }

    // Getters
    public Long getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getLocation() {
        return location;
    }

    public double getSalary() {
        return salary;
    }

    public String getLevel() {
        return level;
    }

    public String getExpertise() {
        return expertise;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public String getMatchSummary() {
        return matchSummary;
    }
}
