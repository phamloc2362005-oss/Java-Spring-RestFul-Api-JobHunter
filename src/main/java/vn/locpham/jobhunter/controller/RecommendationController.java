package vn.locpham.jobhunter.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.locpham.jobhunter.domain.reponse.RestResponse;
import vn.locpham.jobhunter.service.RecommendationService;
import vn.locpham.jobhunter.domain.JobWithScore;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserService userService;

    public RecommendationController(RecommendationService recommendationService, UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    /**
     * API gợi việc làm cho user đang đăng nhập
     * GET /api/v1/recommendations/jobs?limit=10
     * 
     * Lấy userId từ token JWT thay vì truyền thủ công
     */
    @GetMapping("/jobs")
    public ResponseEntity<RestResponse<List<JobWithScore>>> recommendJobs(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        // Lấy email user từ SecurityUtils và tìm user trong DB
        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) {
            RestResponse<List<JobWithScore>> errorResponse = new RestResponse<>();
            errorResponse.setStatusCode(401);
            errorResponse.setError("Unauthorized - Please login");
            return ResponseEntity.status(401).body(errorResponse);
        }

        User user = this.userService.handleGetUserByUsername(email);
        if (user == null) {
            RestResponse<List<JobWithScore>> errorResponse = new RestResponse<>();
            errorResponse.setStatusCode(401);
            errorResponse.setError("Unauthorized - Please login");
            return ResponseEntity.status(401).body(errorResponse);
        }

        RestResponse<List<JobWithScore>> result = this.recommendationService.recommendJobsForUser(user.getId(), limit);
        return ResponseEntity.ok(result);
    }

    // Using email-based lookup instead of extracting userId from token
}