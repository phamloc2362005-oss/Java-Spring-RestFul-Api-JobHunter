package vn.locpham.jobhunter.domain;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.constant.GenderEnum;
import vn.locpham.jobhunter.util.constant.LevelEnum;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "tên không được để trống")
    private String name;
    @NotBlank(message = "email không được để trống")
    private String email;
    @NotBlank(message = "password không được để trống")
    private String password;
    private int age;
    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    private String address;

    @Enumerated(EnumType.STRING)
    private LevelEnum level;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String refreshToken;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Resume> resumes;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_skill", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> skills;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_favorite_job", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "job_id"))
    @JsonIgnore
    private List<Job> favoriteJobs;

    @ManyToOne
    @JoinColumn(name = "expertise_id")
    private Expertise expertise;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    public GenderEnum getGender() {
        return gender;
    }

    public void setGender(GenderEnum gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<Resume> getResumes() {
        return resumes;
    }

    public List<Job> getFavoriteJobs() {
        return favoriteJobs;
    }

    public void setFavoriteJobs(List<Job> favoriteJobs) {
        this.favoriteJobs = favoriteJobs;
    }

    public void setResumes(List<Resume> resumes) {
        this.resumes = resumes;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public void setLevel(LevelEnum level) {
        this.level = level;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    public Expertise getExpertise() {
        return expertise;
    }

    public void setExpertise(Expertise expertise) {
        this.expertise = expertise;
    }

}
