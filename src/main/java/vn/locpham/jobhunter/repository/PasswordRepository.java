package vn.locpham.jobhunter.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.locpham.jobhunter.domain.PasswordReset;

@Repository
public interface PasswordRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
