package vn.locpham.jobhunter.controller.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.reponse.DashboardDTO;
import vn.locpham.jobhunter.repository.CompanyRepository;
import vn.locpham.jobhunter.repository.JobRepository;
import vn.locpham.jobhunter.repository.ResumeRepository;
import vn.locpham.jobhunter.repository.SkillRepository;
import vn.locpham.jobhunter.repository.UserRepository;
import vn.locpham.jobhunter.util.constant.StatusEnum;

@RestController
@RequestMapping("/api/v1/admin")
public class DashboardController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ResumeRepository resumeRepository;
    private final SkillRepository skillRepository;

    public DashboardController(UserRepository userRepository,
            JobRepository jobRepository,
            CompanyRepository companyRepository,
            ResumeRepository resumeRepository,
            SkillRepository skillRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.resumeRepository = resumeRepository;
        this.skillRepository = skillRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboardStats() {
        DashboardDTO dto = new DashboardDTO();

        // 1. Counts
        dto.setTotalUsers(userRepository.count());
        dto.setTotalJobs(jobRepository.count());
        dto.setTotalActiveJobs(jobRepository.countByActiveTrue());
        dto.setTotalCompanies(companyRepository.count());
        dto.setTotalResumes(resumeRepository.count());

        // 2. Resume by status
        Map<String, Long> resumeByStatus = new LinkedHashMap<>();
        for (StatusEnum status : StatusEnum.values()) {
            resumeByStatus.put(status.name(), resumeRepository.countByStatus(status));
        }
        dto.setResumeByStatus(resumeByStatus);

        // 3. Top skills (most demanded in jobs)
        List<Object[]> rawTopSkills = skillRepository.findTopSkills();
        List<DashboardDTO.SkillCount> topSkills = rawTopSkills.stream()
                .map(row -> new DashboardDTO.SkillCount(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
        dto.setTopSkills(topSkills);

        // 4. Recent jobs (5 latest)
        List<Job> latestJobs = jobRepository.findTop5ByOrderByCreatedAtDesc();
        List<DashboardDTO.RecentJob> recentJobs = latestJobs.stream()
                .map(job -> {
                    DashboardDTO.RecentJob rj = new DashboardDTO.RecentJob();
                    rj.setId(job.getId());
                    rj.setName(job.getName());
                    rj.setCompanyName(job.getCompany() != null ? job.getCompany().getName() : "N/A");
                    rj.setLocation(job.getLocation());
                    rj.setSalary(job.getSalary());
                    rj.setLevel(job.getLevel() != null ? job.getLevel().name() : "N/A");
                    rj.setActive(job.isActive());
                    rj.setCreatedAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : "");
                    return rj;
                })
                .collect(Collectors.toList());
        dto.setRecentJobs(recentJobs);

        return ResponseEntity.ok(dto);
    }
}
