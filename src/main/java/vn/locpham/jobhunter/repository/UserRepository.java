package vn.locpham.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.locpham.jobhunter.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
