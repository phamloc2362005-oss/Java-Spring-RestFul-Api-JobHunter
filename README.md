Google Login (OAuth2) Integration
Overview

Hệ thống hỗ trợ đăng nhập bằng Google thông qua OAuth2, sau đó chuyển đổi sang cơ chế xác thực nội bộ bằng JWT (access token + refresh token).

Google chỉ dùng để xác thực người dùng, còn quyền truy cập hệ thống được cấp bằng JWT của backend.

Authentication Flow
🔹 Tổng quan
Frontend → Google → Backend → JWT → Cookie → API
🔹 Chi tiết
1.User bấm Login with Google
2. Browser redirect đến:
http://localhost:8081/oauth2/authorization/google
User đăng nhập Google

Google redirect về backend:

/login/oauth2/code/google

5.Backend xử lý tại OAuth2LoginSuccessHandler:
Lấy thông tin user từ Google (email, name)
Kiểm tra user trong DB
Nếu chưa có → tạo mới user
Tạo:
access_token
refresh_token
Lưu refresh token vào DB
Set cookie:
access_token (httpOnly)
refresh_token (httpOnly)
Redirect về frontend

6. Frontend gọi:

GET /api/v1/auth/account

để lấy thông tin user hiện tại

Backend Implementation
🔹 Security Configuration
.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
Bật OAuth2 login
Sử dụng custom success handler để xử lý sau khi login thành công
🔹 OAuth2LoginSuccessHandler

Chịu trách nhiệm:

Nhận OAuth2User từ Google
Map sang User của hệ thống
Tạo JWT (reuse logic từ SecurityUtils)
Lưu refresh token vào DB
Set cookie
Redirect về frontend
🔹 JWT Generation

Sử dụng chung với login thường:

createAccessToken(...)
createRefreshToken(...)
Access token: dùng để gọi API
Refresh token: dùng để cấp lại access token


