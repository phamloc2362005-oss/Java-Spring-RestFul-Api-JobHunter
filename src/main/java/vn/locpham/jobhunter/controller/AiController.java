package vn.locpham.jobhunter.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.locpham.jobhunter.domain.reponse.RestResponse;
import vn.locpham.jobhunter.service.AiService;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
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
     */
    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    public ResponseEntity<RestResponse<Object>> chatWithAi(@RequestBody Map<String, Object> body) {
        String message = (String) body.getOrDefault("message", "");
        java.util.List<java.util.Map<String, String>> history = (java.util.List<java.util.Map<String, String>>) body
                .get("history");

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
            response.setData(aiResult.get("response"));
            response.setMessage("Nhận phản hồi từ AI thành công!");
        } else {
            response.setStatusCode(503);
            response.setError((String) aiResult.get("response"));
            return ResponseEntity.status(503).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
