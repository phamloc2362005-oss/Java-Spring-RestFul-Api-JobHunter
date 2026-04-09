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

    private ResLoginDTO buildResLoginDTO(User user) {
        ResLoginDTO dto = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        userLogin.setId(user.getId());
        userLogin.setName(user.getName());
        userLogin.setEmail(user.getEmail());
        dto.setUser(userLogin);
        return dto;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // check user
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            // create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPassword("GOOGLE_LOGIN"); // dummy
            user = userService.handleCreateUser(user);
        }

        // generate JWT
        ResLoginDTO dto = buildResLoginDTO(user);
        String accessToken = securityUtils.createAccessToken(user.getEmail(), dto);
        String refreshToken = securityUtils.createRefreshToken(user.getEmail(), dto);
        this.userService.updateUserToken(refreshToken, email);
        // trả về FE
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // local dev là false, production HTTPS thì true
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // local dev là false, production HTTPS thì true
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        response.sendRedirect("http://localhost:4173");
    }
}