# 🚀 JobHunter — Backend API

> RESTful API cho nền tảng tuyển dụng **JobHunter**, xây dựng bằng **Spring Boot 3** + **Java 17**.

---

## 📋 Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tech Stack](#tech-stack)
- [Tính năng](#tính-năng)
- [Cài đặt & Chạy dự án](#cài-đặt--chạy-dự-án)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [API Endpoints](#api-endpoints)
- [Biến môi trường](#biến-môi-trường)

---

## Giới thiệu

JobHunter Backend cung cấp toàn bộ REST API cho ứng dụng tuyển dụng, bao gồm xác thực JWT, phân quyền theo vai trò, tìm kiếm việc làm, AI tích hợp, quản lý hồ sơ ứng tuyển và nhiều hơn nữa.

---

## Tech Stack

| Layer | Công nghệ |
|---|---|
| **Framework** | Spring Boot 3.2.4 |
| **Language** | Java 17 |
| **Build Tool** | Gradle (Kotlin DSL) |
| **Database** | MySQL 8 |
| **ORM** | Spring Data JPA / Hibernate |
| **Security** | Spring Security + JWT (OAuth2 Resource Server) |
| **OAuth2** | Google Login (OAuth2 Client) |
| **Mail** | Spring Mail (SMTP) |
| **Template** | Thymeleaf (email templates) |
| **PDF** | Apache PDFBox 3 |
| **Filter** | Spring Filter (turkraft) |
| **Validation** | Jakarta Bean Validation |

---

## Tính năng

### 🔐 Xác thực & Phân quyền
- Đăng ký / Đăng nhập bằng email + password
- **Google OAuth2** login
- JWT Access Token + Refresh Token (cookie-based)
- Phân quyền theo **Role & Permission** (RBAC)
- Đổi mật khẩu, Quên mật khẩu (OTP qua email)

### 💼 Việc làm & Công ty
- CRUD Jobs, Companies với tìm kiếm nâng cao (Spring Filter)
- Lọc theo skill, địa điểm, mức lương, loại hình
- Đánh dấu job yêu thích (Favorites)
- Tự động gợi ý việc làm theo profile người dùng (AI Recommendation)

### 📄 Ứng tuyển (Resume)
- Nộp CV (upload file PDF)
- Theo dõi trạng thái: PENDING → REVIEWING → APPROVED / REJECTED
- AI chấm điểm độ phù hợp CV với JD (AI Score + Feedback)

### ⭐ Review Công ty
- Người dùng viết review + rating cho công ty
- Like / Dislike review (1 vote/user, toggle được, lưu DB)
- Lấy review đại diện theo thuật toán: like nhiều nhất → dislike ít nhất

### 📧 Job Alert
- Đăng ký nhận email khi có việc làm mới phù hợp với skills
- Tự động gửi email khi admin tạo job mới

### 📰 Bài viết (Articles)
- CRUD Articles cho blog nghề nghiệp
- Public read, Admin write

### 🤖 AI Integration
- AI chatbot tư vấn nghề nghiệp
- AI gợi ý việc làm phù hợp
- AI phân tích CV và cho feedback

---

## Cài đặt & Chạy dự án

### Yêu cầu
- Java 17+
- MySQL 8+
- Gradle 8+

### 1. Clone repo
```bash
git clone https://github.com/phamloc2362005-oss/Java-Spring-RestFul-Api-JobHunter.git
cd Java-Spring-RestFul-Api-JobHunter
```

### 2. Tạo database
```sql
CREATE DATABASE jobhunter CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Cấu hình `application.properties`
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/jobhunter
spring.datasource.username=root
spring.datasource.password=yourpassword

# JWT
locpham.jwt.base64-secret=YOUR_BASE64_SECRET_KEY
locpham.jwt.access-token-validity-in-seconds=86400
locpham.jwt.refresh-token-validity-in-seconds=604800

# Mail (Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@gmail.com
spring.mail.password=your-app-password

# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# Gemini AI
locpham.gemini.api-key=YOUR_GEMINI_API_KEY

# JPA
spring.jpa.hibernate.ddl-auto=update
```

### 4. Chạy ứng dụng
```bash
./gradlew bootRun
```

Server khởi động tại: `http://localhost:8081`

---

## Cấu trúc thư mục

```
src/main/java/vn/locpham/jobhunter/
├── controller/          # REST Controllers
├── domain/
│   ├── request/         # Request DTOs
│   └── reponse/         # Response DTOs
├── repository/          # Spring Data JPA Repositories
├── service/             # Business Logic
└── util/
    ├── config/          # Security, CORS, Mail config
    ├── error/           # Global exception handling
    └── annotattion/     # Custom annotations
```

---

## API Endpoints

### Auth
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/login` | Đăng nhập |
| POST | `/api/v1/auth/register` | Đăng ký |
| GET | `/api/v1/auth/account` | Lấy thông tin user hiện tại |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Đăng xuất |

### Jobs
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/jobs` | Lấy danh sách jobs (public) |
| GET | `/api/v1/jobs/:id` | Chi tiết job |
| POST | `/api/v1/jobs` | Tạo job (Admin) |
| PUT | `/api/v1/jobs/:id` | Cập nhật job (Admin) |
| DELETE | `/api/v1/jobs/:id` | Xóa job (Admin) |

### Companies
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/companies` | Danh sách công ty (public) |
| GET | `/api/v1/companies/:id` | Chi tiết công ty |
| POST | `/api/v1/companies` | Tạo công ty (Admin) |
| PUT | `/api/v1/companies/:id` | Cập nhật (Admin) |
| DELETE | `/api/v1/companies/:id` | Xóa (Admin) |

### Reviews
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/reviews/by-company?companyId=` | Lấy reviews theo công ty |
| POST | `/api/v1/reviews` | Viết review (User) |
| PUT | `/api/v1/reviews/:id/like` | Toggle like review |
| PUT | `/api/v1/reviews/:id/dislike` | Toggle dislike review |

### Password
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/password/forgot` | Gửi OTP quên mật khẩu |
| POST | `/api/v1/password/otp` | Xác minh OTP |
| POST | `/api/v1/password/reset` | Đặt lại mật khẩu |
| PUT | `/api/v1/password/change` | Đổi mật khẩu (đã đăng nhập) |

---

## Biến môi trường

Tất cả cấu hình nhạy cảm đặt trong `application.properties` (không commit lên git).

---

## License

MIT
