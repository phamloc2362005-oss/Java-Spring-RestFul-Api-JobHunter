package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.Review;
import vn.locpham.jobhunter.domain.reponse.ResReviewDTO;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.company.ResCompanyDTO;
import vn.locpham.jobhunter.repository.CompanyRepository;
import vn.locpham.jobhunter.repository.ReviewRepository;
import vn.locpham.jobhunter.repository.UserRepository;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository,
            ReviewRepository reviewRepository, ReviewService reviewService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
    }

    public Company handleCreateNewCompany(Company company) {
        return this.companyRepository.save(company);
    }

    public ResultPaginationDTO fetchAllCompanies(Specification<Company> spec, Pageable pageable) {
        Page<Company> pageCompany = this.companyRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageCompany.getTotalPages());
        mt.setTotal(pageCompany.getTotalElements());

        rs.setMeta(mt);

        // Map Company to ResCompanyDTO and add review statistics
        List<ResCompanyDTO> listCompany = pageCompany.getContent().stream().map(item -> {
            ResCompanyDTO dto = new ResCompanyDTO();
            dto.setId(item.getId());
            dto.setName(item.getName());
            dto.setAddress(item.getAddress());
            dto.setDescription(item.getDescription());
            dto.setLogo(item.getLogo());
            dto.setCreatedAt(item.getCreatedAt());
            dto.setUpdatedAt(item.getUpdatedAt());

            // Calculate statistics
            List<Review> reviews = this.reviewRepository.findByCompany(item);
            if (reviews != null && !reviews.isEmpty()) {
                double avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                long recommendCount = reviews.stream().filter(Review::isRecommend).count();
                double recommendPercent = (double) recommendCount / reviews.size() * 100;

                dto.setAverageRating(avgRating);
                dto.setRecommendPercentage(recommendPercent);
                dto.setTotalReviews((long) reviews.size());

                // Latest review
                Review latestReview = reviews.stream()
                        .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                        .orElse(null);
                if (latestReview != null) {
                    dto.setLatestReview(this.reviewService.convertToResReviewDTO(latestReview));
                }
            } else {
                dto.setAverageRating(0.0);
                dto.setRecommendPercentage(0.0);
                dto.setTotalReviews(0L);
            }

            return dto;
        }).collect(java.util.stream.Collectors.toList());

        rs.setResult(listCompany);
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
