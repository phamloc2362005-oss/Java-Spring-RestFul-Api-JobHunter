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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import vn.locpham.jobhunter.domain.Resume;
import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.repository.ResumeRepository;

@Service
public class AiService {

    @Value("${locpham.gemini.api.key}")
    private String apiKey;

    @Value("${locpham.upload-file.base-uri}")
    private String baseUri;

    @Autowired
    private ResumeRepository resumeRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractTextFromPdf(String fileName, String folder) {
        try {
            // Nếu fileName chứa "/" thì chỉ lấy phần tên file cuối cùng
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }

            // Chuyển đổi baseUri sang đường dẫn local (dùng URI.create() cho Windows-safe)
            Path basePath;
            try {
                basePath = Paths.get(URI.create(baseUri));
            } catch (Exception ex) {
                // fallback nếu URI.create() thất bại
                basePath = Paths.get(baseUri.replace("file:///", ""));
            }
            Path filePath = basePath.resolve(folder).resolve(fileName);
            File file = filePath.toFile();

            System.out.println("DEBUG PDF: Đang đọc file tại: " + file.getAbsolutePath());
            System.out.println("DEBUG PDF: Dung lượng file: " + file.length() + " bytes");

            if (!file.exists() || file.length() == 0) {
                System.err.println("DEBUG PDF: KHÔNG TÌM THẤY FILE HOẶC FILE TRỐNG!");
                return "";
            }

            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);

                if (text == null || text.trim().isEmpty()) {
                    System.out.println("DEBUG PDF: Lần 1 trống, thử lại không sort...");
                    stripper.setSortByPosition(false);
                    text = stripper.getText(document);
                }

                int len = text != null ? text.trim().length() : 0;
                System.out.println("DEBUG PDF: Đọc thành công, độ dài: " + len + " ký tự");
                return text;
            }
        } catch (Exception e) {
            System.err.println("DEBUG PDF: Lỗi khi đọc PDF: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    public Map<String, Object> analyzeResume(String resumeText, String jobDescription) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 0);
        result.put("feedback", "Không thể phân tích CV.");

        if (resumeText == null || resumeText.trim().isEmpty()) {
            System.err.println(
                    "DEBUG AI: CV trống hoặc không thể trích xuất văn bản (có thể là ảnh). Bỏ qua chấm điểm AI.");
            result.put("feedback",
                    "Không thể đọc được nội dung CV (có thể file là hình ảnh hoặc bị lỗi format). Vui lòng thử lại với CV dạng text/PDF chuẩn.");
            return result;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("DEBUG AI: API key chưa được cấu hình!");
            return result;
        }

        // Danh sách model fallback - thử theo thứ tự ưu tiên
        String[] models = {
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-flash-latest",
                "gemini-3-flash-preview"
        };

        // Giới hạn độ dài để tránh vượt token limit
        String trimmedCv = resumeText.length() > 8000
                ? resumeText.substring(0, 8000) + "..."
                : resumeText;
        String trimmedJob = (jobDescription != null && jobDescription.length() > 3000)
                ? jobDescription.substring(0, 3000) + "..."
                : jobDescription;

        String prompt = "Bạn là chuyên gia tuyển dụng HR. Đánh giá mức độ phù hợp của CV với vị trí tuyển dụng.\n"
                + "Mô tả công việc:\n" + trimmedJob + "\n\n"
                + "Nội dung CV:\n" + trimmedCv + "\n\n"
                + "Trả về JSON (không markdown, không backtick) với 2 trường:\n"
                + "- score: số nguyên 0-100\n"
                + "- feedback: nhận xét 2-3 câu tiếng Việt\n";

        for (String model : models) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
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

                System.out.println("DEBUG AI: Đang thử model: " + model);
                URI uri = URI.create(url.trim());
                ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);
                System.out.println("DEBUG AI: Model " + model + " => HTTP " + response.getStatusCode());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        String textResponse = candidates.get(0)
                                .path("content").path("parts").get(0).path("text").asText();

                        // Strip markdown block nếu có
                        textResponse = textResponse.replace("```json", "").replace("```", "").trim();

                        // Tìm JSON object trong response
                        int jsonStart = textResponse.indexOf("{");
                        int jsonEnd = textResponse.lastIndexOf("}");
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            textResponse = textResponse.substring(jsonStart, jsonEnd + 1);
                        }

                        System.out.println("DEBUG AI: Raw JSON từ Gemini: " + textResponse);
                        JsonNode resultJson = objectMapper.readTree(textResponse);

                        if (resultJson.has("score")) {
                            result.put("score", resultJson.get("score").asInt());
                        }
                        if (resultJson.has("feedback")) {
                            result.put("feedback", resultJson.get("feedback").asText());
                        }

                        System.out.println("DEBUG AI: ✅ Thành công với model " + model
                                + " | Score=" + result.get("score"));
                        return result; // Thành công, không cần thử model tiếp theo
                    }
                }

            } catch (HttpClientErrorException e) {
                int statusCode = e.getStatusCode().value();
                String body = e.getResponseBodyAsString();
                String shortBody = body.length() > 200 ? body.substring(0, 200) : body;

                if (statusCode == 429) {
                    System.err.println("DEBUG AI: ❌ Model " + model
                            + " bị QUOTA LIMIT (429): " + shortBody);
                } else if (statusCode == 404) {
                    System.err.println("DEBUG AI: ❌ Model " + model + " không tồn tại (404). Chi tiết: " + shortBody);
                } else {
                    System.err.println("DEBUG AI: ❌ Model " + model
                            + " HTTP " + statusCode + ": " + shortBody);
                }
                // Thử model tiếp theo trong danh sách

            } catch (Exception e) {
                System.err.println("DEBUG AI: ❌ Model " + model + " lỗi: " + e.getMessage());
                // Thử model tiếp theo trong danh sách
            }
        }

        // Tất cả model đều fail
        System.err.println("DEBUG AI: Tất cả model đều fail. Resume vẫn được lưu nhưng không có điểm AI.");
        result.put("feedback", "AI tạm thời không khả dụng (hết quota). CV đã được lưu, điểm AI sẽ được cập nhật sau.");
        return result;
    }

    @Async
    public void scoreResumeAsync(Resume resume, Job job) {
        System.out.println("DEBUG: Đang chấm điểm AI ngầm cho Resume ID: " + resume.getId());
        try {
            if (resume.getUrl() != null && resume.getUrl().toLowerCase().endsWith(".pdf")) {
                String rawDesc = job.getDescription() != null ? job.getDescription() : "";
                String plainDesc = rawDesc.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                String rawRequired = job.getRequired() != null ? job.getRequired() : "";
                String plainRequired = rawRequired.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                String jobDescription = "Title: " + job.getName()
                        + "\nLevel: " + (job.getLevel() != null ? job.getLevel().toString() : "N/A")
                        + "\nLocation: " + (job.getLocation() != null ? job.getLocation() : "N/A")
                        + "\nDescription: " + plainDesc
                        + "\nRequirements: " + plainRequired;

                String pdfText = extractTextFromPdf(resume.getUrl(), "resume");
                if (pdfText != null && !pdfText.isEmpty()) {
                    java.util.Map<String, Object> aiResult = analyzeResume(pdfText, jobDescription);
                    if (aiResult.containsKey("score")) {
                        resume.setAiScore((Integer) aiResult.get("score"));
                    }
                    if (aiResult.containsKey("feedback")) {
                        resume.setAiFeedback((String) aiResult.get("feedback"));
                    }
                    System.out.println("DEBUG: Chấm điểm AI thành công: " + resume.getAiScore());
                } else {
                    System.err.println("DEBUG: KHÔNG ĐỌC ĐƯỢC NỘI DUNG PDF!");
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG AI: Lỗi khi xử lý ngầm: " + e.getMessage());
        }

        // Cập nhật lại Resume trong DB
        resumeRepository.save(resume);
        System.out.println("DEBUG: Đã lưu kết quả AI vào database cho Resume ID: " + resume.getId());
    }
}
