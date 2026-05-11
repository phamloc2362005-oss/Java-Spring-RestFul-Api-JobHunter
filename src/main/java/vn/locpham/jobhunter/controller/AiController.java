package vn.locpham.jobhunter.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Body: { "rawInput": "Tên: Nguyễn Văn A. Làm dev java 3 năm tại công ty ABC..." }
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
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object parsed = mapper.readValue((String) aiResult.get("content"), Object.class);
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
     * Body: { "jobTitle": "...", "jobDescription": "...", "jobLevel": "...", "skills": "..." }
     * Trả về JSON array 5 câu hỏi phỏng vấn.
     */
    @PostMapping("/interview/questions")
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
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object parsed = mapper.readValue((String) aiResult.get("questions"), Object.class);
                response.setData(parsed);
                response.setMessage("Tạo câu hỏi phỏng vấn thành công!");
            } catch (Exception e) {
                response.setData(aiResult.get("questions"));
                response.setMessage("Tạo câu hỏi phỏng vấn thành công (raw)!");
            }
        } else {
            response.setStatusCode(500);
            response.setError("AI không thể tạo câu hỏi. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ai/interview/evaluate
     * Body: { "question": "...", "answer": "...", "jobContext": "..." }
     * Trả về JSON { score, feedback, suggestion, rating }.
     */
    @PostMapping("/interview/evaluate")
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

        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setData(aiResult);
        response.setMessage("Đánh giá câu trả lời thành công!");

        return ResponseEntity.ok(response);
    }
}
