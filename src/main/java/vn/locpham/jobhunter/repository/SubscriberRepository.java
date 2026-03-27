package vn.locpham.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.locpham.jobhunter.domain.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    boolean existsByEmail(String email);
}
