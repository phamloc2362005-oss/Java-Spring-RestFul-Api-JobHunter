package vn.locpham.jobhunter.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.locpham.jobhunter.domain.Expertise;
import vn.locpham.jobhunter.domain.ExpertiseCategory;

@Repository
public interface ExpertiseCategoryRepository
        extends JpaRepository<ExpertiseCategory, Long>, JpaSpecificationExecutor<ExpertiseCategory> {
    Page<ExpertiseCategory> findAll(Specification<ExpertiseCategory> spec, Pageable pageable);

    boolean existsByName(String name);

    List<ExpertiseCategory> findByIdIn(List<Long> id);
}
