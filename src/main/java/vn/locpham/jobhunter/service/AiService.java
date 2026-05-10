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

    @Value("${locpham.groq.api.key}")
    private String groqApiKey;

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
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-flash-latest",
                "gemini-3-flash-preview"
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
}
