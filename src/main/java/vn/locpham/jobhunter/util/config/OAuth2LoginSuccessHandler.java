package vn.locpham.jobhunter.util.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResLoginDTO;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public OAuth2LoginSuccessHandler(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    // Hàm build object trả về giống login thường
    // chứa thông tin user (id, name, email)

    private ResLoginDTO buildResLoginDTO(User user) {
        ResLoginDTO dto = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        userLogin.setId(user.getId());
        userLogin.setName(user.getName());
        userLogin.setEmail(user.getEmail());
        dto.setUser(userLogin);
        return dto;
    }

    // Hàm này sẽ được Spring tự gọi khi:
    // login Google thành công

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        // B1: lấy thông tin user từ Google
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // B2: kiểm tra user đã tồn tại trong DB chưa
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            // create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            // password fake (vì login bằng Google không dùng password)
            user.setPassword("GOOGLE_LOGIN");
            user = userService.handleCreateUser(user);
        }

        // B3: tạo object response giống login thường
        ResLoginDTO dto = buildResLoginDTO(user);
        // B4: tạo access token (dùng gọi API)
        String accessToken = securityUtils.createAccessToken(user.getEmail(), dto);
        // B5: tạo refresh token (dùng xin access token mới)
        String refreshToken = securityUtils.createRefreshToken(user.getEmail(), dto);
        // B6: lưu refresh token vào DB
        this.userService.updateUserToken(refreshToken, email);
        // B7: set cookie cho FE
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // local dev là false, production HTTPS thì true
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);
        // refresh token cookie
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // local dev là false, production HTTPS thì true
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);// sống 7 ngày
        // add cookie vào response
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        // B8: redirect về FE sau khi login Google thành công
        response.sendRedirect("http://localhost:4173");
    }
}