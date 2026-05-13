package vn.locpham.jobhunter.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.User;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String email);

    Page<User> findAll(Specification<User> spec, Pageable pageable);

    boolean existsByEmail(String email);

    User findByRefreshTokenAndEmail(String token, String email);

    List<User> findByCompany(Company company);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.skills LEFT JOIN FETCH u.expertise WHERE u.id = :id")
    Optional<User> findByIdWithSkillsAndExpertise(@Param("id") Long id);
}
