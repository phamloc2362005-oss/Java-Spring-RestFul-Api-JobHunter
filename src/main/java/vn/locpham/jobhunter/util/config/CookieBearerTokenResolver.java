package vn.locpham.jobhunter.util.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {
    // resolver mặc định: đọc token từ header Authorization: Bearer xxx
    private final DefaultBearerTokenResolver defaultBearerTokenResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String bearerToken = defaultBearerTokenResolver.resolve(request);
        // B1: thử lấy token từ header trước
        if (bearerToken != null) {
            return bearerToken;
        }
        // B2: nếu không có header thì lấy từ cookie
        if (request.getCookies() == null) {
            return null;
        }
        // duyệt cookie tìm access_token
        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}