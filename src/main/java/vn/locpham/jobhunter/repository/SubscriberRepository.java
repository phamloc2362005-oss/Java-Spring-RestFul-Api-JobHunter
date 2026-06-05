package vn.locpham.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.locpham.jobhunter.domain.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    boolean existsByEmail(String email);

    Subscriber findByEmail(String email);

    // Tìm tất cả subscriber có ít nhất 1 skill trùng với danh sách skillId
    @Query("SELECT DISTINCT s FROM Subscriber s JOIN s.skills sk WHERE sk.id IN :skillIds")
    List<Subscriber> findBySkillsIdIn(@Param("skillIds") List<Long> skillIds);
}
