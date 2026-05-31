package vn.locpham.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import vn.locpham.jobhunter.domain.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Lấy toàn bộ lịch sử chat của một user (theo email), sắp xếp theo thời gian tạo tăng dần.
     */
    List<ChatMessage> findByUserEmailOrderByCreatedAtAsc(String email);

    /**
     * Xóa toàn bộ lịch sử chat của một user (theo email).
     */
    @Transactional
    void deleteByUserEmail(String email);
}
