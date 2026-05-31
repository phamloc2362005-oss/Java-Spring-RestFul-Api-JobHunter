package vn.locpham.jobhunter.service;

import java.util.List;

import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.ChatMessage;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.repository.ChatMessageRepository;
import vn.locpham.jobhunter.repository.UserRepository;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lưu một tin nhắn chat vào DB.
     *
     * @param email   email của user đang đăng nhập (lấy từ SecurityUtils)
     * @param role    "user" hoặc "model"
     * @param content nội dung tin nhắn
     * @param time    chuỗi thời gian hiển thị UI, ví dụ "14:32"
     */
    public void saveMessage(String email, String role, String content, String time) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return; // Guest hoặc user không tồn tại → không lưu
        }

        ChatMessage message = new ChatMessage();
        message.setRole(role);
        message.setContent(content);
        message.setTime(time);
        message.setUser(user);

        chatMessageRepository.save(message);
    }

    /**
     * Lấy toàn bộ lịch sử chat của user, sắp xếp theo thời gian tạo tăng dần.
     *
     * @param email email của user
     * @return danh sách ChatMessage
     */
    public List<ChatMessage> getHistory(String email) {
        return chatMessageRepository.findByUserEmailOrderByCreatedAtAsc(email);
    }

    /**
     * Xóa toàn bộ lịch sử chat của user.
     *
     * @param email email của user
     */
    public void clearHistory(String email) {
        chatMessageRepository.deleteByUserEmail(email);
    }
}
