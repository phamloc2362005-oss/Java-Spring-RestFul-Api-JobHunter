package vn.locpham.jobhunter.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.locpham.jobhunter.domain.ChatMessage;
import vn.locpham.jobhunter.domain.reponse.RestResponse;
import vn.locpham.jobhunter.service.AiService;
import vn.locpham.jobhunter.service.ChatMessageService;
import vn.locpham.jobhunter.util.SecurityUtils;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final ChatMessageService chatMessageService;

    public AiController(AiService aiService, ChatMessageService chatMessageService) {
        this.aiService = aiService;
        this.chatMessageService = chatMessageService;
    }

    /**
     * POST /api/v1/ai/generate-cv
     * Body: { "rawInput": "Tên: Nguyễn Văn A. Làm dev java 3 năm tại công ty
     * ABC..." }
     * Trả về JSON nội dung CV đã được AI chuẩn hóa.
     */
    @PostMapping("/generate-cv")
    public ResponseEntity<RestResponse<Object>> generateCv(@RequestBody Map<String, String> body) {
        String rawInput = body.get("rawInput");

        if (rawInput == null || rawInput.trim().isEmpty()) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(400);
            error.setError("Vui lòng nhập thông tin CV.");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> aiResult = this.aiService.generateCvContent(rawInput);

        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);

        boolean success = Boolean.TRUE.equals(aiResult.get("success"));
        if (success) {
            // Parse JSON string thành Object để trả về đúng cấu trúc
            try {
                // Dùng readTree (JsonNode) để giữ nguyên Unicode tiếng Việt
                JsonNode parsed = new ObjectMapper().readTree((String) aiResult.get("content"));
                response.setData(parsed);
                response.setMessage("Tạo nội dung CV thành công!");
            } catch (Exception e) {
                // Fallback: trả về raw string nếu parse JSON thất bại
                response.setData(aiResult.get("content"));
                response.setMessage("Tạo nội dung CV thành công (raw)!");
            }
        } else {
            response.setStatusCode(500);
            response.setError((String) aiResult.get("content"));
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ai/interview/questions
     * Body: { "jobTitle": "...", "jobDescription": "...", "jobLevel": "...",
     * "skills": "..." }
     * Trả về JSON array 5 câu hỏi phỏng vấn.
     */
    @PostMapping(value = "/interview/questions", produces = "application/json;charset=UTF-8")
    public ResponseEntity<RestResponse<Object>> generateInterviewQuestions(
            @RequestBody Map<String, String> body) {
        String jobTitle = body.getOrDefault("jobTitle", "");
        String jobDescription = body.getOrDefault("jobDescription", "");
        String jobLevel = body.getOrDefault("jobLevel", "");
        String skills = body.getOrDefault("skills", "");

        if (jobTitle.trim().isEmpty()) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(400);
            error.setError("Thiếu thông tin job.");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> aiResult = this.aiService.generateInterviewQuestions(
                jobTitle, jobDescription, jobLevel, skills);

        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);

        boolean success = Boolean.TRUE.equals(aiResult.get("success"));
        if (success) {
            try {
                // Dùng readTree (JsonNode) thay vì readValue(Object.class) để
                // giữ nguyên Unicode tiếng Việt, tránh bị mất dấu khi re-serialize
                ObjectMapper mapper = new ObjectMapper();
                JsonNode parsed = mapper.readTree((String) aiResult.get("questions"));
                response.setData(parsed);
                response.setMessage("Tạo câu hỏi phỏng vấn thành công!");
            } catch (Exception e) {
                System.err.println("DEBUG ERROR PARSING QUESTIONS: " + e.getMessage());
                // Fallback: trả về raw string để FE tự parse
                response.setData(aiResult.get("questions"));
                response.setMessage("Tạo câu hỏi phỏng vấn thành công (raw)!");
            }
        } else {
            response.setStatusCode(503);
            response.setError("AI không thể tạo câu hỏi lúc này. Hết quota hoặc API lỗi. Vui lòng thử lại.");
            return ResponseEntity.status(503).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ai/interview/evaluate
     * Body: { "question": "...", "answer": "...", "jobContext": "..." }
     * Trả về JSON { score, feedback, suggestion, rating }.
     */
    @PostMapping(value = "/interview/evaluate", produces = "application/json;charset=UTF-8")
    public ResponseEntity<RestResponse<Object>> evaluateInterviewAnswer(
            @RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        String answer = body.getOrDefault("answer", "");
        String jobContext = body.getOrDefault("jobContext", "");

        if (question.trim().isEmpty() || answer.trim().isEmpty()) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(400);
            error.setError("Thiếu câu hỏi hoặc câu trả lời.");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> aiResult = this.aiService.evaluateInterviewAnswer(question, answer, jobContext);

        boolean success = Boolean.TRUE.equals(aiResult.get("success"));
        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);

        if (success) {
            // Chỉ trả về các field cần thiết, bỏ qua field 'success'
            java.util.Map<String, Object> cleanData = new java.util.HashMap<>();
            cleanData.put("score", aiResult.get("score"));
            cleanData.put("feedback", aiResult.get("feedback"));
            cleanData.put("suggestion", aiResult.get("suggestion"));
            cleanData.put("rating", aiResult.get("rating"));
            response.setData(cleanData);
            response.setMessage("Đánh giá câu trả lời thành công!");
        } else {
            response.setStatusCode(503);
            response.setError("AI không thể đánh giá câu trả lời lúc này. Vui lòng thử lại.");
            return ResponseEntity.status(503).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ai/chat
     * Body: { "message": "...", "history": [{"role": "user", "content": "..."},
     * ...] }
     * Tự động lưu user message và AI response vào DB (nếu user đã đăng nhập).
     */
    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    public ResponseEntity<RestResponse<Object>> chatWithAi(@RequestBody Map<String, Object> body) {
        String message = (String) body.getOrDefault("message", "");
        java.util.List<java.util.Map<String, String>> history = (java.util.List<java.util.Map<String, String>>) body
                .get("history");
        String displayTime = (String) body.getOrDefault("time", "");

        if (message.trim().isEmpty()) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(400);
            error.setError("Tin nhắn không được để trống.");
            return ResponseEntity.badRequest().body(error);
        }

        java.util.Map<String, Object> aiResult = this.aiService.chatWithAi(message, history);

        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);

        boolean success = Boolean.TRUE.equals(aiResult.get("success"));
        if (success) {
            String aiText = (String) aiResult.get("response");
            response.setData(aiText);
            response.setMessage("Nhận phản hồi từ AI thành công!");

            // Lưu cả 2 tin nhắn vào DB nếu user đã đăng nhập
            SecurityUtils.getCurrentUserLogin().ifPresent(email -> {
                String now = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                String timeToUse = (displayTime != null && !displayTime.isEmpty()) ? displayTime : now;
                chatMessageService.saveMessage(email, "user", message, timeToUse);
                chatMessageService.saveMessage(email, "model", aiText, now);
            });
        } else {
            response.setStatusCode(503);
            response.setError((String) aiResult.get("response"));
            return ResponseEntity.status(503).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/ai/chat/history
     * Lấy toàn bộ lịch sử chat của user đang đăng nhập.
     */
    @GetMapping(value = "/chat/history", produces = "application/json;charset=UTF-8")
    public ResponseEntity<RestResponse<Object>> getChatHistory() {
        String email = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (email == null) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(401);
            error.setError("Bạn cần đăng nhập để xem lịch sử chat.");
            return ResponseEntity.status(401).body(error);
        }

        List<ChatMessage> messages = chatMessageService.getHistory(email);

        // Map sang DTO gọn để trả về FE
        List<Map<String, String>> result = messages.stream().map(m -> {
            Map<String, String> dto = new java.util.LinkedHashMap<>();
            dto.put("role", m.getRole());
            dto.put("content", m.getContent());
            dto.put("time", m.getTime() != null ? m.getTime() : "");
            return dto;
        }).collect(Collectors.toList());

        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setData(result);
        response.setMessage("Lấy lịch sử chat thành công!");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/ai/chat/history
     * Xóa toàn bộ lịch sử chat của user đang đăng nhập.
     */
    @DeleteMapping(value = "/chat/history", produces = "application/json;charset=UTF-8")
    public ResponseEntity<RestResponse<Object>> clearChatHistory() {
        String email = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (email == null) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(401);
            error.setError("Bạn cần đăng nhập để xóa lịch sử chat.");
            return ResponseEntity.status(401).body(error);
        }

        chatMessageService.clearHistory(email);

        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setMessage("Đã xóa toàn bộ lịch sử chat thành công!");
        return ResponseEntity.ok(response);
    }
}
