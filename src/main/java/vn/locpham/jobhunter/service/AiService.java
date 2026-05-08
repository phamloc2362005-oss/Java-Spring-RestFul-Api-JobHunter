package vn.locpham.jobhunter.service;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiService {

    @Value("${locpham.gemini.api.key}")
    private String apiKey;

    @Value("${locpham.upload-file.base-uri}")
    private String baseUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractTextFromPdf(String fileName, String folder) {
        try {
            URI uri = new URI(baseUri + folder + "/" + fileName);
            Path path = Paths.get(uri);
            File file = new File(path.toString());
            
            if (!file.exists()) {
                return "";
            }

            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (Exception e) {
            System.err.println("Error extracting text from PDF: " + e.getMessage());
            return "";
        }
    }

    public Map<String, Object> analyzeResume(String resumeText, String jobDescription) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 0);
        result.put("feedback", "Không thể phân tích CV.");

        if (apiKey == null || apiKey.equals("YOUR_API_KEY_HERE")) {
            System.err.println("Vui lòng cấu hình locpham.gemini.api.key");
            return result;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String prompt = "Bạn là một chuyên gia tuyển dụng (HR). Nhiệm vụ của bạn là đánh giá mức độ phù hợp của một CV đối với một mô tả công việc.\n" +
                "Mô tả công việc (Job Description):\n" + jobDescription + "\n\n" +
                "Nội dung CV ứng viên:\n" + resumeText + "\n\n" +
                "Hãy phân tích và trả về đúng định dạng JSON chuẩn (không markdown, không backticks) với 2 trường:\n" +
                "- score: Số nguyên từ 0 đến 100 thể hiện phần trăm độ phù hợp.\n" +
                "- feedback: Nhận xét ngắn gọn (khoảng 2-3 câu) giải thích điểm số và chỉ ra điểm mạnh/yếu của CV.\n";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> partObj = new HashMap<>();
            partObj.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(partObj));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    String textResponse = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    
                    // Xóa markdown json block nếu có (Gemini thường bọc trong ```json ... ```)
                    textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                    
                    JsonNode resultJson = objectMapper.readTree(textResponse);
                    if (resultJson.has("score")) {
                        result.put("score", resultJson.get("score").asInt());
                    }
                    if (resultJson.has("feedback")) {
                        result.put("feedback", resultJson.get("feedback").asText());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
        }

        return result;
    }
}
