package vn.locpham.jobhunter.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.locpham.jobhunter.domain.Expertise;

@Repository
public interface ExpertiseRepository extends JpaRepository<Expertise, Long>, JpaSpecificationExecutor<Expertise> {
    Page<Expertise> findAll(Specification<Expertise> spec, Pageable pageable);

    boolean existsByName(String name);

    List<Expertise> findByIdIn(List<Long> id);
}
