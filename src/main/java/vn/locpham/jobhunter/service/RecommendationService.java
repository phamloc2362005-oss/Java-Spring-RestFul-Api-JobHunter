package vn.locpham.jobhunter.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private final AiService aiService;

    // Cache structure: userId -> CachedResult
    private static class CachedRecommendation {
        List<JobWithScore> data;
        long timestamp;

        CachedRecommendation(List<JobWithScore> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long durationMs) {
            return System.currentTimeMillis() - timestamp > durationMs;
        }
    }

    private final Map<Long, CachedRecommendation> recommendationCache = new ConcurrentHashMap<>();
    private final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(10); // 10 minutes

    public RecommendationService(JobRepository jobRepository, UserRepository userRepository, AiService aiService) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
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

        // 0. Check Cache
        CachedRecommendation cached = recommendationCache.get(userId);
        if (cached != null && !cached.isExpired(CACHE_DURATION)) {
            System.out.println("DEBUG AI:  Trả về kết quả từ Cache cho User ID: " + userId);
            response.setStatusCode(200);
            response.setData(cached.data);
            return response;
        }

        List<Job> allJobs = this.jobRepository.findAll();
        if (allJobs == null || allJobs.isEmpty()) {
            response.setStatusCode(404);
            response.setError("No jobs available");
            return response;
        }

        // 1. Lọc thô bằng thuật toán cũ (Prefilter)
        List<JobWithScore> candidates = allJobs.stream()
                .filter(Job::isActive)
                .map(job -> calculateScore(job, user))
                .filter(jws -> jws.getScore() > 10)
                .sorted(Comparator.comparing(JobWithScore::getScore).reversed())
                .limit(20) // Lấy top 20 để AI phân tích
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            response.setStatusCode(200);
            response.setData(new java.util.ArrayList<>());
            return response;
        }

        // 2. Re-rank bằng AI
        List<Job> candidateJobs = candidates.stream().map(JobWithScore::getJob).collect(Collectors.toList());
        List<Map<String, Object>> aiRankings = this.aiService.rankJobsWithAi(user, candidateJobs);

        List<JobWithScore> finalJobs;
        if (aiRankings != null && !aiRankings.isEmpty()) {
            // Mapping kết quả AI vào JobWithScore
            finalJobs = candidates.stream().map(jws -> {
                Map<String, Object> aiRes = aiRankings.stream()
                        .filter(res -> {
                            Object jobIdObj = res.get("jobId");
                            if (jobIdObj instanceof Number) {
                                return ((Number) jobIdObj).longValue() == jws.getJob().getId();
                            }
                            return false;
                        })
                        .findFirst()
                        .orElse(null);

                if (aiRes != null) {
                    int aiScore = ((Number) aiRes.get("score")).intValue();
                    String aiSummary = (String) aiRes.get("summary");
                    return new JobWithScore(jws.getJob(), aiScore, aiSummary);
                }
                return jws;
            }).filter(jws -> jws.getScore() > 50) // Sau khi AI chấm xong, chỉ lấy Job > 50%
                    .sorted(Comparator.comparing(JobWithScore::getScore).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            // Fallback nếu AI lỗi: Lấy hết 20 candidates ban đầu để người dùng không bị
            // trống màn hình
            finalJobs = candidates.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // 3. Save to Cache
        if (finalJobs != null && !finalJobs.isEmpty()) {
            recommendationCache.put(userId, new CachedRecommendation(finalJobs));
        }

        response.setStatusCode(200);
        response.setData(finalJobs);
        return response;
    }

    /**
     * Clear cache for a specific user (call this when user updates profile)
     */
    public void clearCache(Long userId) {
        recommendationCache.remove(userId);
        System.out.println("DEBUG AI:  Đã xóa cache gợi ý cho User ID: " + userId);
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
     * Tính điểm cấp độ: công bằng với cả over-qualified và under-qualified
     * - Exact match (user level == job level): 25 điểm (phù hợp nhất)
     * - Adjacent match (|user level - job level| == 1): 15 điểm (chấp nhận được)
     * - Gap > 1 level (dù cao hay thấp): 0 điểm (không phù hợp)
     * - Null level: 15 điểm (không xác định, cho cơ hội)
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

        // Tính khoảng cách giữa level
        int levelGap = Math.abs(userLevelValue - jobLevelValue);

        if (levelGap == 0) {
            return 25; // Exact match (INTERN-INTERN, JUNIOR-JUNIOR, v.v.)
        } else if (levelGap == 1) {
            return 15; // Adjacent match (JUNIOR-MIDDLE, MIDDLE-SENIOR, v.v.)
        }

        return 0; // Gap > 1: quá cao hoặc quá thấp
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