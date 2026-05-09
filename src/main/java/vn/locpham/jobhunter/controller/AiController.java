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
}
