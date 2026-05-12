package vn.locpham.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.locpham.jobhunter.domain.reponse.ResReviewDTO;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.request.ReqCreateReviewDTO;
import vn.locpham.jobhunter.service.ReviewService;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(@RequestBody ReqCreateReviewDTO req) {
        try {
            ResReviewDTO res = this.reviewService.createReview(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/reviews/by-company")
    public ResponseEntity<ResultPaginationDTO> getReviewsByCompany(
            @RequestParam("companyId") long companyId,
            Pageable pageable) {
        ResultPaginationDTO rs = this.reviewService.fetchReviewsByCompany(companyId, pageable);
        return ResponseEntity.ok(rs);
    }
}
