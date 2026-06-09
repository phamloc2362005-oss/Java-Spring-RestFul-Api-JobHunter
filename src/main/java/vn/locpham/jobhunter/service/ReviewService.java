package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Review;
import vn.locpham.jobhunter.domain.ReviewVote;
import vn.locpham.jobhunter.domain.ReviewVote.VoteType;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResReviewDTO;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.request.ReqCreateReviewDTO;
import vn.locpham.jobhunter.repository.CompanyRepository;
import vn.locpham.jobhunter.repository.ReviewRepository;
import vn.locpham.jobhunter.repository.ReviewVoteRepository;
import vn.locpham.jobhunter.repository.UserRepository;
import vn.locpham.jobhunter.util.SecurityUtils;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ReviewVoteRepository reviewVoteRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository,
            CompanyRepository companyRepository, ReviewVoteRepository reviewVoteRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.reviewVoteRepository = reviewVoteRepository;
    }

    // ── Create Review ────────────────────────────────────────────────────────────
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
        review.setRecommend(req.isRecommend());
        review.setUser(user);
        review.setCompany(optCompany.get());

        Review savedReview = this.reviewRepository.save(review);
        return convertToResReviewDTO(savedReview, null);
    }

    // ── Fetch Reviews by Company ─────────────────────────────────────────────────
    public ResultPaginationDTO fetchReviewsByCompany(long companyId, Pageable pageable) {
        Optional<Company> optCompany = this.companyRepository.findById(companyId);
        if (!optCompany.isPresent()) {
            return null;
        }

        // Lấy user hiện tại nếu đã đăng nhập (để trả userVote)
        User currentUser = getCurrentUser();

        Page<Review> pageReview = this.reviewRepository.findByCompany(optCompany.get(), pageable);

        List<ResReviewDTO> listReview = pageReview.getContent()
                .stream().map(review -> {
                    String userVote = null;
                    if (currentUser != null) {
                        Optional<ReviewVote> vote = reviewVoteRepository.findByUserAndReview(currentUser, review);
                        userVote = vote.map(v -> v.getType().name()).orElse(null);
                    }
                    return convertToResReviewDTO(review, userVote);
                })
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

    // ── Toggle Like ──────────────────────────────────────────────────────────────
    public ResReviewDTO toggleLike(long reviewId) throws Exception {
        User user = getAuthenticatedUser();
        Review review = getReview(reviewId);

        Optional<ReviewVote> existingVote = reviewVoteRepository.findByUserAndReview(user, review);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            if (vote.getType() == VoteType.LIKE) {
                // Đã like rồi → bỏ like (toggle off)
                reviewVoteRepository.delete(vote);
                review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
                reviewRepository.save(review);
                return convertToResReviewDTO(review, null);
            } else {
                // Đang dislike → chuyển sang like
                review.setDislikeCount(Math.max(0, review.getDislikeCount() - 1));
                review.setLikeCount(review.getLikeCount() + 1);
                vote.setType(VoteType.LIKE);
                reviewVoteRepository.save(vote);
                reviewRepository.save(review);
                return convertToResReviewDTO(review, "LIKE");
            }
        } else {
            // Chưa vote → thêm like mới
            ReviewVote vote = new ReviewVote();
            vote.setUser(user);
            vote.setReview(review);
            vote.setType(VoteType.LIKE);
            reviewVoteRepository.save(vote);
            review.setLikeCount(review.getLikeCount() + 1);
            reviewRepository.save(review);
            return convertToResReviewDTO(review, "LIKE");
        }
    }

    // ── Toggle Dislike ───────────────────────────────────────────────────────────
    public ResReviewDTO toggleDislike(long reviewId) throws Exception {
        User user = getAuthenticatedUser();
        Review review = getReview(reviewId);

        Optional<ReviewVote> existingVote = reviewVoteRepository.findByUserAndReview(user, review);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            if (vote.getType() == VoteType.DISLIKE) {
                // Đã dislike rồi → bỏ dislike (toggle off)
                reviewVoteRepository.delete(vote);
                review.setDislikeCount(Math.max(0, review.getDislikeCount() - 1));
                reviewRepository.save(review);
                return convertToResReviewDTO(review, null);
            } else {
                // Đang like → chuyển sang dislike
                review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
                review.setDislikeCount(review.getDislikeCount() + 1);
                vote.setType(VoteType.DISLIKE);
                reviewVoteRepository.save(vote);
                reviewRepository.save(review);
                return convertToResReviewDTO(review, "DISLIKE");
            }
        } else {
            // Chưa vote → thêm dislike mới
            ReviewVote vote = new ReviewVote();
            vote.setUser(user);
            vote.setReview(review);
            vote.setType(VoteType.DISLIKE);
            reviewVoteRepository.save(vote);
            review.setDislikeCount(review.getDislikeCount() + 1);
            reviewRepository.save(review);
            return convertToResReviewDTO(review, "DISLIKE");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────
    private Review getReview(long reviewId) throws Exception {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new Exception("Review not found"));
    }

    /** Trả về User đang đăng nhập. Ném Exception nếu chưa login. */
    private User getAuthenticatedUser() throws Exception {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new Exception("You must be logged in to vote"));
        User user = userRepository.findByEmail(email);
        if (user == null) throw new Exception("User not found");
        return user;
    }

    /** Trả về User đang đăng nhập hoặc null nếu chưa login. */
    private User getCurrentUser() {
        return SecurityUtils.getCurrentUserLogin()
                .map(userRepository::findByEmail)
                .orElse(null);
    }

    public ResReviewDTO convertToResReviewDTO(Review review, String userVote) {
        ResReviewDTO res = new ResReviewDTO();
        res.setId(review.getId());
        res.setRating(review.getRating());
        res.setContent(review.getContent());
        res.setTitle(review.getTitle());
        res.setPros(review.getPros());
        res.setCons(review.getCons());
        res.setRecommend(review.isRecommend());
        res.setLikeCount(review.getLikeCount());
        res.setDislikeCount(review.getDislikeCount());
        res.setCreatedAt(review.getCreatedAt());
        res.setUserVote(userVote);

        if (review.getUser() != null) {
            ResReviewDTO.UserReview userReview = new ResReviewDTO.UserReview();
            userReview.setId(review.getUser().getId());
            userReview.setName(review.getUser().getName());
            res.setUser(userReview);
        }

        return res;
    }
}
