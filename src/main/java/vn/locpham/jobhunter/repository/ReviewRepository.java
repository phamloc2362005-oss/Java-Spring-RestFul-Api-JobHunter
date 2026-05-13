package vn.locpham.jobhunter.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByCompany(Company company, Pageable pageable);

    List<Review> findByCompanyOrderByCreatedAtDesc(Company company);

    long countByCompany(Company company);

    List<Review> findByCompany(Company company);
}
