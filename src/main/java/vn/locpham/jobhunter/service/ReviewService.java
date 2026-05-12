package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Review;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResReviewDTO;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.request.ReqCreateReviewDTO;
import vn.locpham.jobhunter.repository.CompanyRepository;
import vn.locpham.jobhunter.repository.ReviewRepository;
import vn.locpham.jobhunter.repository.UserRepository;
import vn.locpham.jobhunter.util.SecurityUtils;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository,
            CompanyRepository companyRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    public ResReviewDTO createReview(ReqCreateReviewDTO req) throws Exception {
        Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
        if (!currentUserLogin.isPresent()) {
            throw new Exception("You must be logged in to post a review");
        }

        String email = currentUserLogin.get();
        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("User not found");
        }

        Optional<Company> optCompany = this.companyRepository.findById(req.getCompanyId());
        if (!optCompany.isPresent()) {
            throw new Exception("Company not found");
        }

        Review review = new Review();
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setTitle(req.getTitle());
        review.setPros(req.getPros());
        review.setCons(req.getCons());
        review.setUser(user);
        review.setCompany(optCompany.get());

        Review savedReview = this.reviewRepository.save(review);
        return convertToResReviewDTO(savedReview);
    }

    public ResultPaginationDTO fetchReviewsByCompany(long companyId, Pageable pageable) {
        Optional<Company> optCompany = this.companyRepository.findById(companyId);
        if (!optCompany.isPresent()) {
            return null;
        }

        Page<Review> pageReview = this.reviewRepository.findByCompany(optCompany.get(), pageable);
        
        List<ResReviewDTO> listReview = pageReview.getContent()
                .stream().map(item -> convertToResReviewDTO(item))
                .collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageReview.getTotalPages());
        mt.setTotal(pageReview.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listReview);
        
        return rs;
    }

    private ResReviewDTO convertToResReviewDTO(Review review) {
        ResReviewDTO res = new ResReviewDTO();
        res.setId(review.getId());
        res.setRating(review.getRating());
        res.setContent(review.getContent());
        res.setTitle(review.getTitle());
        res.setPros(review.getPros());
        res.setCons(review.getCons());
        res.setCreatedAt(review.getCreatedAt());

        if (review.getUser() != null) {
            ResReviewDTO.UserReview userReview = new ResReviewDTO.UserReview();
            userReview.setId(review.getUser().getId());
            userReview.setName(review.getUser().getName());
            res.setUser(userReview);
        }

        return res;
    }
}
