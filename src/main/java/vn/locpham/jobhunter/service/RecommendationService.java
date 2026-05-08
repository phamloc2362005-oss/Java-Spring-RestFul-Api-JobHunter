package vn.locpham.jobhunter.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.RestResponse;
import vn.locpham.jobhunter.domain.JobWithScore;
import vn.locpham.jobhunter.repository.JobRepository;
import vn.locpham.jobhunter.repository.UserRepository;
import vn.locpham.jobhunter.util.constant.LevelEnum;

@Service
public class RecommendationService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public RecommendationService(JobRepository jobRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gợi ý việc làm cho user dựa trên:
     * 1. Skill matching (so sánh skills của user vs job requirements)
     * 2. Level matching (kinh nghiệm)
     * 3. Location (địa chỉ)
     * 4. Expertise (chuyên ngành)
     * 5. Salary & active status
     */
    public RestResponse<List<JobWithScore>> recommendJobsForUser(Long userId, int limit) {
        RestResponse<List<JobWithScore>> response = new RestResponse<>();

        User user = this.userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.setStatusCode(404);
            response.setError("User not found");
            return response;
        }

        List<Job> allJobs = this.jobRepository.findAll();
        if (allJobs == null || allJobs.isEmpty()) {
            response.setStatusCode(404);
            response.setError("No jobs available");
            return response;
        }

        // Lọc chỉ lấy job active
        List<Job> activeJobs = allJobs.stream()
                .filter(Job::isActive)
                .collect(Collectors.toList());

        // Tính điểm cho từng job
        List<JobWithScore> scoredJobs = activeJobs.stream()
                .map(job -> calculateScore(job, user))
                .sorted(Comparator.comparing(JobWithScore::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        response.setStatusCode(200);
        response.setData(scoredJobs);
        return response;
    }

    /**
     * Tính điểm phù hợp của job với user dựa trên:
     * - Skill matching: 40 điểm (so sánh skills của user vs job required skills)
     * - Level matching: 25 điểm (user level >= job level)
     * - Location matching: 20 điểm (địa chỉ user vs job)
     * - Expertise matching: 10 điểm (chuyên ngành của user vs job)
     * - Salary: 5 điểm (job có salary cao)
     */
    private JobWithScore calculateScore(Job job, User user) {
        double score = 0;
        double maxScore = 100;

        // 1. Skill matching (40 điểm) - so sánh skills thực sự
        double skillScore = calculateSkillScore(job, user);
        score += skillScore;

        // 2. Level matching (25 điểm)
        double levelScore = calculateLevelScore(job, user);
        score += levelScore;

        // 3. Location matching (20 điểm)
        double locationScore = calculateLocationScore(job, user);
        score += locationScore;

        // 4. Expertise matching (10 điểm)
        double expertiseScore = calculateExpertiseScore(job, user);
        score += expertiseScore;

        // 5. Salary & active status (5 điểm)
        double bonusScore = calculateBonusScore(job);
        score += bonusScore;

        return new JobWithScore(job, (int) Math.min(score, maxScore));
    }

    /**
     * Tính điểm kỹ năng: so sánh skills của user với skills required của job
     * Formula: (số skills match / số skills required của job) * 40
     * Nếu job không yêu cầu skill thì cho 40 điểm (mọi người đều phù hợp)
     */
    private double calculateSkillScore(Job job, User user) {
        if (job.getSkills() == null || job.getSkills().isEmpty()) {
            return 40; // Job không yêu cầu skill cụ thể
        }

        if (user.getSkills() == null || user.getSkills().isEmpty()) {
            return 0; // User không có skill nào
        }

        // So sánh số skill khớp
        List<String> userSkillNames = user.getSkills()
                .stream()
                .map(Skill::getName)
                .collect(Collectors.toList());

        long matchedSkills = job.getSkills()
                .stream()
                .filter(jobSkill -> userSkillNames.contains(jobSkill.getName()))
                .count();

        double matchRatio = (double) matchedSkills / job.getSkills().size();
        return matchRatio * 40;
    }

    /**
     * Tính điểm cấp độ: user level >= job level được điểm full
     * - Exact match: 25 điểm
     * - 1 level cao hơn: 25 điểm (vẫn phù hợp)
     * - 1 level thấp hơn: 15 điểm (có thể phù hợp nhưng chưa đủ kinh nghiệm)
     * - 2+ level thấp hơn: 0 điểm
     * - Null level: 15 điểm (không xác định)
     */
    private double calculateLevelScore(Job job, User user) {
        if (job.getLevel() == null) {
            return 25; // Job không yêu cầu level cụ thể
        }

        if (user.getLevel() == null) {
            return 15; // User chưa xác định level
        }

        int userLevelValue = getLevelValue(user.getLevel());
        int jobLevelValue = getLevelValue(job.getLevel());

        if (userLevelValue >= jobLevelValue) {
            return 25; // Đủ kinh nghiệm
        } else if (userLevelValue == jobLevelValue - 1) {
            return 15; // Thiếu 1 level
        }

        return 0; // Thiếu quá nhiều kinh nghiệm
    }

    /**
     * Lấy giá trị số của level để so sánh
     */
    private int getLevelValue(LevelEnum level) {
        switch (level) {
            case INTERN:
                return 1;
            case JUNIOR:
                return 2;
            case MIDDLE:
                return 3;
            case SENIOR:
                return 4;
            default:
                return 0;
        }
    }

    /**
     * Tính điểm vị trí: so sánh địa chỉ user vs job location
     * - Exact match: 20 điểm
     * - Partial match (cùng city/province): 12 điểm
     * - No match: 5 điểm (vẫn xem xét cho công việc remote)
     */
    private double calculateLocationScore(Job job, User user) {
        if (job.getLocation() == null || job.getLocation().isEmpty()) {
            return 10; // Job remote hoặc location không rõ
        }

        if (user.getAddress() == null || user.getAddress().isEmpty()) {
            return 5;
        }

        String userLocation = user.getAddress().toLowerCase().trim();
        String jobLocation = job.getLocation().toLowerCase().trim();

        if (userLocation.equals(jobLocation)) {
            return 20; // Exact match
        }

        // Kiểm tra partial match (cùng tỉnh/thành phố)
        String[] userLocationParts = userLocation.split(",");
        String[] jobLocationParts = jobLocation.split(",");

        if (userLocationParts.length > 0 && jobLocationParts.length > 0) {
            String userCity = userLocationParts[userLocationParts.length - 1].trim();
            String jobCity = jobLocationParts[jobLocationParts.length - 1].trim();

            if (userCity.equalsIgnoreCase(jobCity)) {
                return 12; // Partial match (cùng city)
            }
        }

        return 5; // Không match nhưng vẫn consider (có thể remote)
    }

    /**
     * Tính điểm chuyên ngành: user expertise match với job expertise
     * - Match: 10 điểm
     * - No match: 3 điểm (vẫn xem xét)
     */
    private double calculateExpertiseScore(Job job, User user) {
        if (job.getExpertise() == null || user.getExpertise() == null) {
            return 3;
        }

        if (job.getExpertise().getId() == user.getExpertise().getId()) {
            return 10;
        }

        return 3;
    }

    /**
     * Tính điểm bonus: job active & có salary cao
     */
    private double calculateBonusScore(Job job) {
        double bonus = 0;

        if (job.isActive()) {
            bonus += 3;
        }

        if (job.getSalary() > 15_000_000) {
            bonus += 2; // Job lương cao
        }

        return bonus;
    }

    // JobWithScore moved to domain package
}