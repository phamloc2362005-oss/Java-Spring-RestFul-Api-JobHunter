package vn.locpham.jobhunter.domain.reponse;

import java.util.List;
import java.util.Map;

public class DashboardDTO {
    private long totalUsers;
    private long totalJobs;
    private long totalActiveJobs;
    private long totalCompanies;
    private long totalResumes;
    private Map<String, Long> resumeByStatus;
    private List<SkillCount> topSkills;
    private List<RecentJob> recentJobs;

    // --- Inner DTOs ---

    public static class SkillCount {
        private String name;
        private long count;

        public SkillCount() {
        }

        public SkillCount(String name, long count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class RecentJob {
        private long id;
        private String name;
        private String companyName;
        private String location;
        private double salary;
        private String level;
        private boolean active;
        private String createdAt;

        public RecentJob() {
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

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    // --- Getters & Setters ---

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getTotalActiveJobs() {
        return totalActiveJobs;
    }

    public void setTotalActiveJobs(long totalActiveJobs) {
        this.totalActiveJobs = totalActiveJobs;
    }

    public long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public long getTotalResumes() {
        return totalResumes;
    }

    public void setTotalResumes(long totalResumes) {
        this.totalResumes = totalResumes;
    }

    public Map<String, Long> getResumeByStatus() {
        return resumeByStatus;
    }

    public void setResumeByStatus(Map<String, Long> resumeByStatus) {
        this.resumeByStatus = resumeByStatus;
    }

    public List<SkillCount> getTopSkills() {
        return topSkills;
    }

    public void setTopSkills(List<SkillCount> topSkills) {
        this.topSkills = topSkills;
    }

    public List<RecentJob> getRecentJobs() {
        return recentJobs;
    }

    public void setRecentJobs(List<RecentJob> recentJobs) {
        this.recentJobs = recentJobs;
    }
}
