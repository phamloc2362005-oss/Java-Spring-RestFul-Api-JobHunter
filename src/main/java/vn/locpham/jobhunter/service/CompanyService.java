package vn.locpham.jobhunter.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Review;
import vn.locpham.jobhunter.domain.reponse.ResReviewDTO;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.company.ResCompanyDTO;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.repository.CompanyRepository;
import vn.locpham.jobhunter.repository.JobRepository;
import vn.locpham.jobhunter.repository.ReviewRepository;
import vn.locpham.jobhunter.repository.UserRepository;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final JobRepository jobRepository;

    // Hardcoded max reviews cap (tránh over-score)
    private static final double MAX_REVIEWS_CAP = 500.0;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository,
            ReviewRepository reviewRepository, ReviewService reviewService, JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.jobRepository = jobRepository;
    }

    public Company handleCreateNewCompany(Company company) {
        return this.companyRepository.save(company);
    }

    // ══════════════════════════════════════════════════════
    //  Composite Score = 0 → 100
    //  Component 1: Rating          (40 pts max)
    //  Component 2: Recommend %     (30 pts max)
    //  Component 3: Review count    (20 pts max, log scale)
    //  Component 4: Has active jobs (10 pts)
    // ══════════════════════════════════════════════════════
    private double calculateRankScore(Double avgRating, Double recommendPct, Long totalReviews, boolean hasActiveJobs) {
        // Component 1: Rating (0–40)
        double ratingScore = (avgRating != null ? avgRating : 0.0) / 5.0 * 40.0;

        // Component 2: Recommend % (0–30)
        double recommendScore = (recommendPct != null ? recommendPct : 0.0) / 100.0 * 30.0;

        // Component 3: Review count — log scale, capped at MAX_REVIEWS_CAP (0–20)
        long reviews = (totalReviews != null ? totalReviews : 0L);
        double reviewScore = 0.0;
        if (reviews > 0) {
            double logValue = Math.log10(Math.min(reviews, (long) MAX_REVIEWS_CAP) + 1.0)
                    / Math.log10(MAX_REVIEWS_CAP + 1.0);
            reviewScore = logValue * 20.0;
        }

        // Component 4: Has active jobs (0 or 10)
        double jobScore = hasActiveJobs ? 10.0 : 0.0;

        return ratingScore + recommendScore + reviewScore + jobScore;
    }

    public ResultPaginationDTO fetchAllCompanies(Specification<Company> spec, Pageable pageable) {
        // Fetch ALL companies matching spec (no pagination yet — to score & sort globally)
        List<Company> allCompanies = this.companyRepository.findAll(spec);

        // Map + score each company
        List<ResCompanyDTO> scoredList = allCompanies.stream().map(item -> {
            ResCompanyDTO dto = new ResCompanyDTO();
            dto.setId(item.getId());
            dto.setName(item.getName());
            dto.setAddress(item.getAddress());
            dto.setDescription(item.getDescription());
            dto.setLogo(item.getLogo());
            dto.setCreatedAt(item.getCreatedAt());
            dto.setUpdatedAt(item.getUpdatedAt());

            // Review statistics
            List<Review> reviews = this.reviewRepository.findByCompany(item);
            if (reviews != null && !reviews.isEmpty()) {
                double avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                long recommendCount = reviews.stream().filter(Review::isRecommend).count();
                double recommendPercent = (double) recommendCount / reviews.size() * 100.0;

                dto.setAverageRating(avgRating);
                dto.setRecommendPercentage(recommendPercent);
                dto.setTotalReviews((long) reviews.size());

                // Most-liked review (fallback to latest if no likes yet)
                Review featuredReview = reviews.stream()
                        .max(Comparator.comparingInt((Review r) -> r.getLikeCount())
                                .thenComparing(Review::getCreatedAt))
                        .orElse(null);
                if (featuredReview != null) {
                    dto.setLatestReview(this.reviewService.convertToResReviewDTO(featuredReview));
                }
            } else {
                dto.setAverageRating(0.0);
                dto.setRecommendPercentage(0.0);
                dto.setTotalReviews(0L);
            }

            // Active jobs check
            boolean hasActiveJobs = this.jobRepository.countByCompanyIdAndActiveTrue(item.getId()) > 0;

            // Calculate composite rank score
            double rankScore = calculateRankScore(
                    dto.getAverageRating(),
                    dto.getRecommendPercentage(),
                    dto.getTotalReviews(),
                    hasActiveJobs);
            dto.setRankScore(Math.round(rankScore * 10.0) / 10.0); // round to 1 decimal

            return dto;
        }).collect(Collectors.toList());

        // Sort globally by rankScore DESC
        scoredList.sort(Comparator.comparingDouble(ResCompanyDTO::getRankScore).reversed());

        // Manual pagination
        int page = pageable.getPageNumber();           // 0-indexed
        int size = pageable.getPageSize();
        int total = scoredList.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<ResCompanyDTO> pageContent = scoredList.subList(fromIndex, toIndex);

        // Build result
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(page + 1);
        mt.setPageSize(size);
        mt.setPages((int) Math.ceil((double) total / size));
        mt.setTotal(total);
        rs.setMeta(mt);
        rs.setResult(pageContent);
        return rs;
    }

    public Company handleUpdateCompany(Company reqCompany) {
        Optional<Company> optionalCompany = this.companyRepository.findById(reqCompany.getId());
        if (optionalCompany.isPresent()) {
            Company updateCompany = optionalCompany.get();
            updateCompany.setName(reqCompany.getName());
            updateCompany.setAddress(reqCompany.getAddress());
            updateCompany.setLogo(reqCompany.getLogo());
            updateCompany.setDescription(reqCompany.getDescription());
            return this.companyRepository.save(updateCompany);
        }
        return null;
    }

    public void handleDeleteCompany(long id) {
        Optional<Company> comOptional = this.companyRepository.findById(id);
        if (comOptional.isPresent()) {
            Company com = comOptional.get();
            List<User> users = this.userRepository.findByCompany(com);
            this.userRepository.deleteAll(users);
        }
        this.companyRepository.deleteById(id);
    }

    public Optional<Company> findById(long id) {
        return this.companyRepository.findById(id);
    }
}
