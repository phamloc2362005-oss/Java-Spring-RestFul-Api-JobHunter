package vn.locpham.jobhunter.domain.reponse.job;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import vn.locpham.jobhunter.util.constant.LevelEnum;

public class ResUpdateJobDTO {
    private long id;
    private String name;
    private String location;
    private double salary;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private LevelEnum level;
    private Date startDate;
    private Date endDate;
    private List<String> skills;
    private List<String> expertises;
    private boolean active;
    private Instant updatedAt;
    private String updatedBy;

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public void setLevel(LevelEnum level) {
        this.level = level;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getExpertises() {
        return expertises;
    }

    public void setExpertises(List<String> expertises) {
        this.expertises = expertises;
    }

}
