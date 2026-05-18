package vn.locpham.jobhunter.service;

import java.util.stream.Collectors;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.Skill;

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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
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
import java.nio.charset.StandardCharsets;

@Service
public class AiService {

    @Value("${locpham.gemini.api.key}")
    private String apiKey;

    @Value("${locpham.groq.api.key}")
    private String groqApiKey;

    @Value("${locpham.upload-file.base-uri}")
    private String baseUri;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private vn.locpham.jobhunter.repository.JobRepository jobRepository;

    @Autowired
    private vn.locpham.jobhunter.repository.CompanyRepository companyRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
        // Cấu hình UTF-8 cho tất cả String responses
        this.restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

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

    public String extractBase64FromPdf(String fileName, String folder) {
        try {
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }

            Path basePath;
            try {
                basePath = Paths.get(URI.create(baseUri));
            } catch (Exception ex) {
                basePath = Paths.get(baseUri.replace("file:///", ""));
            }
            Path filePath = basePath.resolve(folder).resolve(fileName);
            File file = filePath.toFile();

            if (!file.exists() || file.length() == 0) {
                return null;
            }

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            return java.util.Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            System.err.println("DEBUG PDF: Lỗi khi đọc file sang Base64: " + e.getMessage());
            return null;
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
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-2.0-flash-exp",
                "gemini-2.5-flash",
                "gemini-flash-latest"
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

    public Map<String, Object> analyzeResumeWithPdf(String base64Pdf, String jobDescription) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 0);
        result.put("feedback", "Không thể phân tích CV.");

        if (base64Pdf == null || base64Pdf.isEmpty()) {
            System.err.println("DEBUG AI: PDF Base64 trống. Bỏ qua chấm điểm AI.");
            return result;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("DEBUG AI: API key chưa được cấu hình!");
            return result;
        }

        String[] models = {
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-2.0-flash-exp",
                "gemini-2.5-flash",
                "gemini-flash-latest"
        };

        String trimmedJob = (jobDescription != null && jobDescription.length() > 3000)
                ? jobDescription.substring(0, 3000) + "..."
                : jobDescription;

        String prompt = "Bạn là chuyên gia tuyển dụng HR. Đánh giá mức độ phù hợp của CV với vị trí tuyển dụng.\n"
                + "Hãy ƯU TIÊN TẬP TRUNG vào kỹ năng cốt lõi và số năm kinh nghiệm để chấm điểm. Bạn vẫn CÓ THỂ NHẬN XÉT về thiết kế, font chữ, bố cục màu sắc của CV nhưng đừng trừ quá nhiều điểm vì thiết kế xấu.\n"
                + "Hãy chấm điểm một cách khách quan, công tâm và nương tay (ưu tiên điểm cao nếu kỹ năng sát với yêu cầu).\n"
                + "Mô tả công việc:\n" + trimmedJob + "\n\n"
                + "Trả về JSON (không markdown, không backtick) với 2 trường:\n"
                + "- score: số nguyên 0-100\n"
                + "- feedback: nhận xét 5-6 câu tiếng Việt (bao gồm cả chuyên môn và thiết kế)\n";

        for (String model : models) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", prompt);

                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mimeType", "application/pdf");
                inlineData.put("data", base64Pdf);

                Map<String, Object> inlineDataPart = new HashMap<>();
                inlineDataPart.put("inlineData", inlineData);

                Map<String, Object> partObj = new HashMap<>();
                partObj.put("parts", List.of(textPart, inlineDataPart));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("contents", List.of(partObj));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                System.out.println("DEBUG AI: Đang thử model: " + model + " với định dạng PDF trực tiếp");
                URI uri = URI.create(url.trim());
                ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);
                System.out.println("DEBUG AI: Model " + model + " => HTTP " + response.getStatusCode());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        String textResponse = candidates.get(0)
                                .path("content").path("parts").get(0).path("text").asText();

                        textResponse = textResponse.replace("```json", "").replace("```", "").trim();
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

                        System.out.println(
                                "DEBUG AI: ✅ Thành công với model " + model + " | Score=" + result.get("score"));
                        return result;
                    }
                }
            } catch (HttpClientErrorException e) {
                System.err.println("DEBUG AI: ❌ Model " + model + " lỗi HTTP: " + e.getStatusCode() + " - "
                        + (e.getResponseBodyAsString().length() > 200 ? e.getResponseBodyAsString().substring(0, 200)
                                : e.getResponseBodyAsString()));
            } catch (Exception e) {
                System.err.println("DEBUG AI: ❌ Model " + model + " lỗi: " + e.getMessage());
            }
        }
        System.err.println("DEBUG AI: Tất cả model đều fail.");
        result.put("feedback", "AI tạm thời không khả dụng (hết quota hoặc lỗi xử lý PDF).");
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

                // ==========================================
                // CÁCH 1: DÙNG GEMINI ĐỌC TRỰC TIẾP FILE PDF BẰNG MẮT (Multimodal - Chuẩn nhất)
                // (Để demo cách 1: Uncomment đoạn code bên dưới, Comment đoạn code cách 2)
                // ==========================================
                String base64Pdf = extractBase64FromPdf(resume.getUrl(), "resume");
                if (base64Pdf != null && !base64Pdf.isEmpty()) {
                    java.util.Map<String, Object> aiResult = analyzeResumeWithPdf(base64Pdf, jobDescription);
                    if (aiResult.containsKey("score")) {
                        resume.setAiScore((Integer) aiResult.get("score"));
                    }
                    if (aiResult.containsKey("feedback")) {
                        resume.setAiFeedback((String) aiResult.get("feedback"));
                    }
                    System.out.println("DEBUG: Chấm điểm AI (Cách 1 - Đọc PDF) thành công: " + resume.getAiScore());
                } else {
                    System.err.println("DEBUG: KHÔNG ĐỌC HOẶC MÃ HÓA ĐƯỢC FILE PDF!");
                }

                // ==========================================
                // CÁCH 2: DÙNG THƯ VIỆN PDFBox ĐỂ BÓC CHỮ (Text-to-Text - Đỡ tốn tài nguyên
                // hơn)
                // (Để demo cách 2: Uncomment đoạn code bên dưới, Comment đoạn code cách 1)
                // ==========================================
                /*
                 * String pdfText = extractTextFromPdf(resume.getUrl(), "resume");
                 * if (pdfText != null && !pdfText.isEmpty()) {
                 * java.util.Map<String, Object> aiResult = analyzeResume(pdfText,
                 * jobDescription);
                 * if (aiResult.containsKey("score")) {
                 * resume.setAiScore((Integer) aiResult.get("score"));
                 * }
                 * if (aiResult.containsKey("feedback")) {
                 * resume.setAiFeedback((String) aiResult.get("feedback"));
                 * }
                 * System.out.println("DEBUG: Chấm điểm AI (Cách 2 - Bóc chữ) thành công: " +
                 * resume.getAiScore());
                 * } else {
                 * System.err.println("DEBUG: KHÔNG BÓC TÁCH ĐƯỢC CHỮ TỪ PDF!");
                 * }
                 */
            }
        } catch (Exception e) {
            System.err.println("DEBUG AI: Lỗi khi xử lý ngầm: " + e.getMessage());
        }

        // Cập nhật lại Resume trong DB
        resumeRepository.save(resume);
        System.out.println("DEBUG: Đã lưu kết quả AI vào database cho Resume ID: " + resume.getId());
    }

    /**
     * AI Resume Builder: Nhận thông tin thô từ ứng viên, trả về nội dung CV đã được
     * chuẩn hóa.
     */
    public Map<String, Object> generateCvContent(String rawInput) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("content", "Không thể tạo nội dung CV.");

        if (rawInput == null || rawInput.trim().isEmpty()) {
            result.put("content", "Vui lòng nhập thông tin CV.");
            return result;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            result.put("content", "API key chưa được cấu hình.");
            return result;
        }

        String[] models = {
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-2.0-flash-exp",
                "gemini-2.5-flash",
                "gemini-flash-latest"
        };

        String prompt = "Bạn là chuyên gia viết CV IT hàng đầu. Nhiệm vụ: Biến thông tin thô của khách hàng thành bản CV CHUYÊN NGHIỆP, ĐẦY ĐẶN, LẤP ĐẦY ĐÚNG 1 TRANG A4.\n"
                + "Dữ liệu thô từ khách hàng: " + rawInput + "\n\n"
                + "NGUYÊN TẮC VÀNG:\n"
                + "1. CV phải DÀI HƠN và CHI TIẾT HƠN thông tin khách hàng cung cấp. Phát triển mỗi ý thành câu dài, chuyên nghiệp.\n"
                + "2. NẾU THIẾU: Bịa thêm kinh nghiệm, kỹ năng phù hợp ngành nghề.\n"
                + "3. NHƯNG KHÔNG VƯỢT QUÁ 1 TRANG A4. Tuân thủ số lượng bên dưới.\n"
                + "4. Mỗi bullet point: 1 câu dài 15-25 từ, có động từ mạnh và số liệu.\n"
                + "5. CHỈ TRẢ VỀ JSON.\n\n"
                + "SỐ LƯỢNG CHÍNH XÁC:\n"
                + "- careerObjective: 2-3 câu (khoảng 40-60 từ).\n"
                + "- skills: 6-7 kỹ năng.\n"
                + "- interests: 3 sở thích.\n"
                + "- education: 1 mục.\n"
                + "- experiences: 2 công việc, mỗi công việc 3 bullet points.\n\n"
                + "JSON SCHEMA (viết dài như ví dụ):\n"
                + "{\n"
                + "  \"name\": \"Họ và Tên\",\n"
                + "  \"jobTitle\": \"VỊ TRÍ ỨNG TUYỂN\",\n"
                + "  \"personalInfo\": { \"dob\": \"15/06/1999\", \"gender\": \"Nam\", \"phone\": \"0912 345 678\", \"email\": \"ten@gmail.com\", \"address\": \"Quận 1, TP.HCM\" },\n"
                + "  \"careerObjective\": \"Với hơn 2 năm kinh nghiệm phát triển ứng dụng web sử dụng Java Spring Boot và ReactJS, tôi mong muốn gia nhập đội ngũ công nghệ năng động để xây dựng sản phẩm phần mềm chất lượng cao và nâng cao năng lực chuyên môn.\",\n"
                + "  \"skills\": [ {\"name\": \"Java Spring Boot\", \"level\": 85}, {\"name\": \"RESTful API\", \"level\": 90}, {\"name\": \"MySQL\", \"level\": 80}, {\"name\": \"Docker\", \"level\": 70}, {\"name\": \"Git/GitHub\", \"level\": 85}, {\"name\": \"ReactJS\", \"level\": 65} ],\n"
                + "  \"interests\": [\"Nghiên cứu công nghệ mới\", \"Đọc blog kỹ thuật\", \"Chạy bộ\"],\n"
                + "  \"education\": [ {\"timeRange\": \"2017 - 2021\", \"major\": \"Kỹ thuật Phần mềm\", \"school\": \"Đại học Bách Khoa TP.HCM\", \"desc\": \"Tốt nghiệp loại Giỏi, GPA 3.2/4.0\"} ],\n"
                + "  \"experiences\": [ {\"timeRange\": \"03/2022 - Hiện tại\", \"title\": \"Java Backend Developer\", \"company\": \"FPT Software\", \"bullets\": [\"Thiết kế và phát triển hệ thống RESTful API phục vụ hơn 50,000 người dùng sử dụng Java Spring Boot và Hibernate ORM.\", \"Tối ưu hóa hiệu suất truy vấn cơ sở dữ liệu Oracle, giảm 35% thời gian phản hồi API từ 800ms xuống 520ms.\", \"Phối hợp với team Frontend và QA trong quy trình Agile/Scrum, đảm bảo delivery đúng sprint với bug rate dưới 5%.\"]} ]\n"
                + "}\n";

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

                System.out.println("DEBUG AI CV Builder: Đang thử model: " + model);
                URI uri = URI.create(url.trim());

                // Ép RestTemplate dùng UTF-8 để không bị lỗi font Tiếng Việt
                restTemplate.getMessageConverters().add(0,
                        new org.springframework.http.converter.StringHttpMessageConverter(
                                java.nio.charset.StandardCharsets.UTF_8));

                ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        String textResponse = candidates.get(0)
                                .path("content").path("parts").get(0).path("text").asText();

                        System.out.println("DEBUG RAW AI RESPONSE: " + textResponse);

                        textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                        int jsonStart = textResponse.indexOf("{");
                        int jsonEnd = textResponse.lastIndexOf("}");
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            textResponse = textResponse.substring(jsonStart, jsonEnd + 1);
                        }

                        System.out.println("DEBUG AI CV Builder: ✅ Thành công với model " + model);
                        result.put("success", true);
                        result.put("content", textResponse);
                        return result;
                    }
                }
            } catch (HttpClientErrorException e) {
                System.err.println("DEBUG AI CV Builder: ❌ Model " + model + " lỗi HTTP: " + e.getStatusCode());
            } catch (Exception e) {
                System.err.println("DEBUG AI CV Builder: ❌ Model " + model + " lỗi: " + e.getMessage());
            }
        }

        // ===== FALLBACK: GROQ API (khi Gemini hết quota) =====
        System.out.println("DEBUG AI CV Builder: ⚡ Gemini hết quota, chuyển sang Groq...");
        try {
            String groqUrl = "https://api.groq.com/openai/v1/chat/completions";
            HttpHeaders groqHeaders = new HttpHeaders();
            groqHeaders.setContentType(MediaType.APPLICATION_JSON);
            groqHeaders.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> groqMessage = new HashMap<>();
            groqMessage.put("role", "user");
            groqMessage.put("content", prompt);

            Map<String, Object> groqBody = new HashMap<>();
            groqBody.put("model", "llama-3.3-70b-versatile");
            groqBody.put("messages", List.of(groqMessage));
            groqBody.put("temperature", 0.7);
            groqBody.put("max_tokens", 4096);

            restTemplate.getMessageConverters().add(0,
                    new org.springframework.http.converter.StringHttpMessageConverter(
                            java.nio.charset.StandardCharsets.UTF_8));

            HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqBody, groqHeaders);
            ResponseEntity<String> groqResponse = restTemplate.postForEntity(groqUrl, groqEntity, String.class);

            if (groqResponse.getStatusCode().is2xxSuccessful() && groqResponse.getBody() != null) {
                JsonNode groqRoot = objectMapper.readTree(groqResponse.getBody());
                JsonNode choices = groqRoot.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String textResponse = choices.get(0).path("message").path("content").asText();
                    System.out.println("DEBUG RAW GROQ RESPONSE: " + textResponse);

                    textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                    int jsonStart = textResponse.indexOf("{");
                    int jsonEnd = textResponse.lastIndexOf("}");
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        textResponse = textResponse.substring(jsonStart, jsonEnd + 1);
                    }

                    System.out.println("DEBUG AI CV Builder: ✅ Thành công với Groq!");
                    result.put("success", true);
                    result.put("content", textResponse);
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG AI CV Builder: ❌ Groq cũng lỗi: " + e.getMessage());
        }

        result.put("content", "AI tạm thời không khả dụng. Vui lòng thử lại sau.");
        return result;
    }

    /**
     * AI Mock Interview: Sinh 5 câu hỏi phỏng vấn dựa trên thông tin job.
     * Dùng Groq làm primary (nhanh), Gemini làm fallback.
     */
    public Map<String, Object> generateInterviewQuestions(String jobTitle, String jobDescription,
            String jobLevel, String skills) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("questions", "[]");

        String prompt = "Bạn là chuyên gia tuyển dụng IT cao cấp. Hãy tạo đúng 5 câu hỏi phỏng vấn chuyên sâu cho vị trí sau:\n"
                + "- Vị trí: " + jobTitle + "\n"
                + "- Cấp bậc: " + jobLevel + "\n"
                + "- Kỹ năng yêu cầu: " + skills + "\n"
                + "- Mô tả công việc: " + (jobDescription != null && jobDescription.length() > 2000
                        ? jobDescription.substring(0, 2000)
                        : jobDescription)
                + "\n\n"
                + "NGUYÊN TẮC:\n"
                + "1. Câu hỏi phải THỰC TẾ, sâu sắc, phù hợp với cấp bậc " + jobLevel + ".\n"
                + "2. Mix giữa câu hỏi kỹ thuật, tình huống thực tế và câu hỏi hành vi (behavioral).\n"
                + "3. Mỗi câu hỏi phải rõ ràng, không mơ hồ.\n"
                + "4. CHỈ TRẢ VỀ JSON array, không có markdown, không giải thích thêm.\n\n"
                + "JSON SCHEMA:\n"
                + "[{\"id\": 1, \"question\": \"Câu hỏi 1?\", \"type\": \"technical\", \"hint\": \"Gợi ý điểm cần đề cập\"}, ...]\n"
                + "type có thể là: technical | behavioral | situational";

        // PRIMARY: Gemini (ưu tiên model ổn định nhất lên đầu)
        String[] models = { "gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.0-flash-lite",
                "gemini-flash-latest" };
        for (String model : models) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", prompt);
                Map<String, Object> partObj = new HashMap<>();
                partObj.put("parts", List.of(textPart));
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("contents", List.of(partObj));
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                URI uri = URI.create(url.trim());

                // Dùng byte[] để tránh charset issue
                ResponseEntity<byte[]> response = restTemplate.postForEntity(uri, entity, byte[].class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String rawBody = new String(response.getBody(), StandardCharsets.UTF_8);
                    JsonNode root = objectMapper.readTree(rawBody);
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        String textResponse = candidates.get(0)
                                .path("content").path("parts").get(0).path("text").asText();
                        System.out.println("DEBUG RAW GEMINI RESPONSE: " + textResponse);

                        // Làm sạch JSON mạnh mẽ hơn
                        textResponse = textResponse.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                                .replaceAll("(?s)```\\s*(.*?)\\s*```", "$1")
                                .trim();

                        int arrStart = textResponse.indexOf("[");
                        int arrEnd = textResponse.lastIndexOf("]");
                        if (arrStart >= 0 && arrEnd > arrStart) {
                            textResponse = textResponse.substring(arrStart, arrEnd + 1);
                        }
                        System.out.println("DEBUG CLEANED RESPONSE: " + textResponse);
                        System.out.println("DEBUG Interview Questions: ✅ Gemini " + model + " thành công!");
                        result.put("success", true);
                        result.put("questions", textResponse);
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("DEBUG Interview Questions: ❌ Gemini " + model + " lỗi: " + e.getMessage());
            }
        }

        // FALLBACK: Groq
        try {
            String groqUrl = "https://api.groq.com/openai/v1/chat/completions";
            HttpHeaders groqHeaders = new HttpHeaders();
            groqHeaders.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
            groqHeaders.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> groqMessage = new HashMap<>();
            groqMessage.put("role", "user");
            groqMessage.put("content", prompt);

            Map<String, Object> groqBody = new HashMap<>();
            groqBody.put("model", "llama-3.3-70b-versatile");
            groqBody.put("messages", List.of(groqMessage));
            groqBody.put("temperature", 0.7);
            groqBody.put("max_tokens", 2048);

            HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqBody, groqHeaders);
            ResponseEntity<byte[]> groqResponse = restTemplate.postForEntity(groqUrl, groqEntity, byte[].class);

            if (groqResponse.getStatusCode().is2xxSuccessful() && groqResponse.getBody() != null) {
                String textResponse = new String(groqResponse.getBody(), StandardCharsets.UTF_8);
                JsonNode groqRoot = objectMapper.readTree(textResponse);
                JsonNode choices = groqRoot.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    textResponse = choices.get(0).path("message").path("content").asText();
                    textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                    int arrStart = textResponse.indexOf("[");
                    int arrEnd = textResponse.lastIndexOf("]");
                    if (arrStart >= 0 && arrEnd > arrStart) {
                        textResponse = textResponse.substring(arrStart, arrEnd + 1);
                    }
                    System.out.println("DEBUG Interview Questions: ✅ Groq (fallback) thành công!");
                    result.put("success", true);
                    result.put("questions", textResponse);
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG Interview Questions: ❌ Groq fallback lỗi: " + e.getMessage());
        }

        result.put("questions", "[]");
        return result;
    }

    /**
     * AI Mock Interview: Chấm điểm + nhận xét câu trả lời phỏng vấn.
     * Dùng Groq làm primary, Gemini làm fallback.
     */
    public Map<String, Object> evaluateInterviewAnswer(String question, String answer, String jobContext) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("score", 0);
        result.put("feedback", "Không thể đánh giá câu trả lời.");
        result.put("suggestion", "");

        if (answer == null || answer.trim().length() < 10) {
            result.put("feedback", "Câu trả lời quá ngắn để đánh giá.");
            return result;
        }

        String prompt = "Bạn là HR chuyên nghiệp đang phỏng vấn ứng viên IT. Hãy đánh giá câu trả lời sau.\n"
                + "Ngữ cảnh công việc: " + jobContext + "\n\n"
                + "Câu hỏi phỏng vấn: " + question + "\n\n"
                + "Câu trả lời của ứng viên: " + answer + "\n\n"
                + "Hãy chấm điểm và nhận xét KHÁCH QUAN, XÂY DỰNG (không quá khắt khe, không quá dễ dãi).\n"
                + "CHỈ TRẢ VỀ JSON, không markdown, không giải thích:\n"
                + "{\"score\": <number 0.0-10.0>, "
                + "\"feedback\": \"<2-3 sentences in Vietnamese about strengths/weaknesses>\", "
                + "\"suggestion\": \"<1-2 sentences in Vietnamese for improvement>\", "
                + "\"rating\": \"<Excellent|Good|Fair|Needs Improvement|Failed>\"}";

        // PRIMARY: Gemini
        String[] models = { "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-2.0-flash-exp", "gemini-2.5-flash",
                "gemini-flash-latest" };
        for (String model : models) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", prompt);
                Map<String, Object> partObj = new HashMap<>();
                partObj.put("parts", List.of(textPart));
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("contents", List.of(partObj));
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                URI uri = URI.create(url.trim());

                // Dùng byte[] để tránh charset issue
                ResponseEntity<byte[]> response = restTemplate.postForEntity(uri, entity, byte[].class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String rawBody = new String(response.getBody(), StandardCharsets.UTF_8);
                    JsonNode root = objectMapper.readTree(rawBody);
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        String textResponse = candidates.get(0)
                                .path("content").path("parts").get(0).path("text").asText();
                        textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                        int jsonStart = textResponse.indexOf("{");
                        int jsonEnd = textResponse.lastIndexOf("}");
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            textResponse = textResponse.substring(jsonStart, jsonEnd + 1);
                        }
                        JsonNode evalJson = objectMapper.readTree(textResponse);
                        result.put("success", true);
                        result.put("score", evalJson.path("score").asDouble());
                        result.put("feedback", evalJson.path("feedback").asText());
                        result.put("suggestion", evalJson.path("suggestion").asText());
                        result.put("rating", evalJson.path("rating").asText());
                        System.out.println(
                                "DEBUG Interview Evaluate: ✅ Gemini " + model + " thành công!");
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println(
                        "DEBUG Interview Evaluate: ❌ Gemini " + model + " lỗi: " + e.getMessage());
            }
        }

        // FALLBACK: Groq
        try {
            String groqUrl = "https://api.groq.com/openai/v1/chat/completions";
            HttpHeaders groqHeaders = new HttpHeaders();
            groqHeaders.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
            groqHeaders.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> groqMessage = new HashMap<>();
            groqMessage.put("role", "user");
            groqMessage.put("content", prompt);

            Map<String, Object> groqBody = new HashMap<>();
            groqBody.put("model", "llama-3.3-70b-versatile");
            groqBody.put("messages", List.of(groqMessage));
            groqBody.put("temperature", 0.5);
            groqBody.put("max_tokens", 512);

            HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqBody, groqHeaders);
            ResponseEntity<byte[]> groqResponse = restTemplate.postForEntity(groqUrl, groqEntity, byte[].class);

            if (groqResponse.getStatusCode().is2xxSuccessful() && groqResponse.getBody() != null) {
                String groqBody_str = new String(groqResponse.getBody(), StandardCharsets.UTF_8);
                JsonNode groqRoot = objectMapper.readTree(groqBody_str);
                JsonNode choices = groqRoot.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String textResponse = choices.get(0).path("message").path("content").asText();
                    textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                    int jsonStart = textResponse.indexOf("{");
                    int jsonEnd = textResponse.lastIndexOf("}");
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        textResponse = textResponse.substring(jsonStart, jsonEnd + 1);
                    }
                    JsonNode evalJson = objectMapper.readTree(textResponse);
                    result.put("success", true);
                    result.put("score", evalJson.path("score").asDouble());
                    result.put("feedback", evalJson.path("feedback").asText());
                    result.put("suggestion", evalJson.path("suggestion").asText());
                    result.put("rating", evalJson.path("rating").asText());
                    System.out.println("DEBUG Interview Evaluate: ✅ Groq (fallback) thành công!");
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG Interview Evaluate: ❌ Groq fallback lỗi: " + e.getMessage());
        }

        return result;
    }

    /**
     * AI Recommendation: Re-rank jobs based on user profile and provide summaries.
     */
    public List<Map<String, Object>> rankJobsWithAi(User user, List<Job> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("DEBUG AI: API key chưa được cấu hình!");
            return null;
        }

        // Convert User profile to text
        StringBuilder userProfile = new StringBuilder();
        userProfile.append("Name: ").append(user.getName()).append("\n");
        userProfile.append("Level: ").append(user.getLevel()).append("\n");
        userProfile.append("Expertise: ").append(user.getExpertise() != null ? user.getExpertise().getName() : "N/A")
                .append("\n");
        userProfile.append("Skills: ")
                .append(user.getSkills().stream().map(Skill::getName).collect(Collectors.joining(", ")))
                .append("\n");
        userProfile.append("Address: ").append(user.getAddress()).append("\n");

        // Convert Jobs to text
        StringBuilder jobsList = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Job job = candidates.get(i);
            jobsList.append("Job ID: ").append(job.getId()).append("\n");
            jobsList.append("Title: ").append(job.getName()).append("\n");
            jobsList.append("Description: ")
                    .append(job.getDescription() != null ? job.getDescription().replaceAll("<[^>]*>", " ") : "")
                    .append("\n");
            jobsList.append("Required: ")
                    .append(job.getRequired() != null ? job.getRequired().replaceAll("<[^>]*>", " ") : "")
                    .append("\n");
            jobsList.append("---\n");
        }

        String prompt = "Bạn là chuyên gia tư vấn nghề nghiệp. Dựa trên thông tin ứng viên sau đây:\n"
                + userProfile.toString() + "\n"
                + "Hãy đánh giá mức độ phù hợp với danh sách công việc bên dưới:\n"
                + jobsList.toString() + "\n"
                + "YÊU CẦU:\n"
                + "1. Trả về một mảng JSON các object.\n"
                + "2. Mỗi object gồm: jobId (số), score (số thực 0-100), summary (1 câu tiếng Việt ngắn gọn giải thích tại sao phù hợp, ví dụ: 'Phù hợp với kỹ năng Java và kinh nghiệm 2 năm của bạn').\n"
                + "3. Chỉ trả về JSON array, không markdown, không giải thích thêm.\n"
                + "CHÚ Ý: Hãy chấm điểm khách quan dựa trên sự tương đồng về kỹ năng và cấp bậc.";

        String[] models = { "gemini-2.5-flash", "gemini-2.0-flash", "gemini-flash-latest" };

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
                ResponseEntity<String> response = restTemplate.postForEntity(URI.create(url), entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String textResponse = root.path("candidates").get(0).path("content").path("parts").get(0)
                            .path("text")
                            .asText();

                    textResponse = textResponse.replace("```json", "").replace("```", "").trim();
                    int arrStart = textResponse.indexOf("[");
                    int arrEnd = textResponse.lastIndexOf("]");
                    if (arrStart >= 0 && arrEnd > arrStart) {
                        textResponse = textResponse.substring(arrStart, arrEnd + 1);
                    }

                    List<Map<String, Object>> resultList = objectMapper.readValue(textResponse,
                            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                            });
                    System.out.println("DEBUG AI Recommendation: ✅ Thành công với model " + model);
                    return resultList;
                }
            } catch (Exception e) {
                System.err.println("DEBUG AI Recommendation: ❌ Model " + model + " lỗi: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * AI Chatbot: Chat with users using real database context (Jobs & Companies).
     */
    public Map<String, Object> chatWithAi(String userMessage, List<Map<String, String>> history) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);

        if (apiKey == null || apiKey.isEmpty()) {
            result.put("response", "Hệ thống AI chưa được cấu hình API Key. Vui lòng liên hệ quản trị viên.");
            return result;
        }

        // 1. Fetch system context (Active Jobs & Companies)
        StringBuilder context = new StringBuilder();
        context.append("BẠN LÀ TRỢ LÝ HỖ TRỢ TUYỂN DỤNG CỦA CỔNG THÔNG TIN VIỆC LÀM JOBHUNTER.\n");
        context.append(
                "Nhiệm vụ của bạn là tư vấn nghề nghiệp, trả lời câu hỏi và gợi ý công việc dựa TRÊN DỮ LIỆU THỰC TẾ dưới đây:\n\n");

        context.append("=== DANH SÁCH VIỆC LÀM ĐANG TUYỂN (ACTIVE) ===\n");
        try {
            List<Job> activeJobs = jobRepository.countByActiveTrue() > 0
                    ? jobRepository.findTop5ByOrderByCreatedAtDesc() // Get recent active jobs to keep context clean &
                                                                     // small
                    : List.of();
            if (activeJobs.isEmpty()) {
                context.append("- Hiện tại chưa có việc làm nào đăng tuyển.\n");
            } else {
                for (Job job : activeJobs) {
                    context.append("- Job ID: ").append(job.getId())
                            .append(", Vị trí: ").append(job.getName())
                            .append(", Công ty: ").append(job.getCompany() != null ? job.getCompany().getName() : "N/A")
                            .append(", Địa điểm: ").append(job.getLocation())
                            .append(", Mức lương: ")
                            .append(job.getSalary() > 0 ? (job.getSalary() / 1_000_000) + " triệu VNĐ" : "Thỏa thuận")
                            .append(", Cấp bậc: ").append(job.getLevel() != null ? job.getLevel().name() : "N/A")
                            .append(", Kỹ năng yêu cầu: ")
                            .append(job.getSkills().stream().map(Skill::getName).collect(Collectors.joining(", ")))
                            .append("\n");
                }
            }
        } catch (Exception e) {
            context.append("- Lỗi tải danh sách việc làm.\n");
        }

        context.append("\n=== DANH SÁCH CÔNG TY NỔI BẬT ===\n");
        try {
            var companies = companyRepository.findAll();
            if (companies.isEmpty()) {
                context.append("- Chưa có công ty nào đăng ký.\n");
            } else {
                int limit = Math.min(companies.size(), 5);
                for (int i = 0; i < limit; i++) {
                    var c = companies.get(i);
                    context.append("- ").append(c.getName()).append(" (Địa chỉ: ").append(c.getAddress()).append(")\n");
                }
            }
        } catch (Exception e) {
            context.append("- Lỗi tải danh sách công ty.\n");
        }

        context.append("\n=== QUY TẮC TRẢ LỜI ===\n");
        context.append("1. Trả lời thân thiện, ngắn gọn, lịch sự bằng tiếng Việt.\n");
        context.append(
                "2. Nếu người dùng hỏi về công việc, hãy giới thiệu các công việc tương đồng từ 'DANH SÁCH VIỆC LÀM ĐANG TUYỂN' ở trên. Ghi rõ tên công ty, mức lương và khuyên ứng viên ứng tuyển.\n");
        context.append("3. Không tự chế (hallucinate) tên công ty hoặc vị trí không có trong danh sách trên.\n");
        context.append(
                "4. Nếu người dùng hỏi những câu hỏi ngoài lề (như nấu ăn, thời tiết), hãy khéo léo từ chối và hướng họ quay lại chủ đề tuyển dụng/career advisor của JobHunter.\n\n");

        // 2. Build Chat prompt with history
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(context.toString());
        promptBuilder.append("=== LỊCH SỬ TRÒ CHUYỆN ===\n");
        if (history != null) {
            for (Map<String, String> msg : history) {
                String role = "user".equalsIgnoreCase(msg.get("role")) ? "Người dùng" : "Trợ lý AI";
                promptBuilder.append(role).append(": ").append(msg.get("content")).append("\n");
            }
        }
        promptBuilder.append("Người dùng: ").append(userMessage).append("\n");
        promptBuilder.append("Trợ lý AI: ");

        String prompt = promptBuilder.toString();
        String[] models = { "gemini-2.5-flash", "gemini-2.0-flash", "gemini-flash-latest" };

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
                ResponseEntity<String> response = restTemplate.postForEntity(URI.create(url), entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String textResponse = root.path("candidates").get(0).path("content").path("parts").get(0)
                            .path("text").asText();

                    result.put("success", true);
                    result.put("response", textResponse.trim());
                    System.out.println("DEBUG AI Chat: ✅ Thành công với model " + model);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("DEBUG AI Chat: ❌ Model " + model + " lỗi: " + e.getMessage());
            }
        }

        // ===== FALLBACK: GROQ =====
        try {
            String groqUrl = "https://api.groq.com/openai/v1/chat/completions";
            HttpHeaders groqHeaders = new HttpHeaders();
            groqHeaders.setContentType(MediaType.APPLICATION_JSON);
            groqHeaders.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> groqMessage = new HashMap<>();
            groqMessage.put("role", "user");
            groqMessage.put("content", prompt);

            Map<String, Object> groqBody = new HashMap<>();
            groqBody.put("model", "llama-3.3-70b-versatile");
            groqBody.put("messages", List.of(groqMessage));
            groqBody.put("temperature", 0.7);
            groqBody.put("max_tokens", 1024);

            HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqBody, groqHeaders);
            ResponseEntity<String> groqResponse = restTemplate.postForEntity(groqUrl, groqEntity, String.class);

            if (groqResponse.getStatusCode().is2xxSuccessful() && groqResponse.getBody() != null) {
                JsonNode groqRoot = objectMapper.readTree(groqResponse.getBody());
                JsonNode choices = groqRoot.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String textResponse = choices.get(0).path("message").path("content").asText();
                    result.put("success", true);
                    result.put("response", textResponse.trim());
                    System.out.println("DEBUG AI Chat: ✅ Thành công với Groq fallback!");
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG AI Chat: ❌ Groq fallback lỗi: " + e.getMessage());
        }

        result.put("response",
                "Xin lỗi bạn, chatbot hiện tại đang quá tải hoặc gặp sự cố kết nối AI. Vui lòng thử lại sau.");
        return result;
    }
}
