package vn.locpham.jobhunter.util.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;

import vn.locpham.jobhunter.util.SecurityUtils;
//lớp xác thực
//có đăng nhập chưa
//token hợp lệ không
//API này có public không

@Configuration // báo cho Spring biết đây là file config
@EnableMethodSecurity(securedEnabled = true) // bật phân quyền bằng annotation như @Secured
public class SecurityConfiguration {
    // đọc secret key từ application.properties
    @Value("${locpham.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // dùng BCrypt để hash password khi đăng ký
        // và để Spring Security so password khi login
        return new BCryptPasswordEncoder();

    }

    private SecretKey getSecretKey() {
        // giải mã chuỗi secret base64 thành mảng byte
        byte[] keyBytes = Base64.from(jwtKey).decode();

        // biến mảng byte thành SecretKey để ký/giải mã JWT
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtils.JWT_ALGORITHM.getName());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        // bean dùng để tạo JWT (access token, refresh token)
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        // tạo decoder để giải mã và validate JWT
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(getSecretKey())
                .macAlgorithm(SecurityUtils.JWT_ALGORITHM)
                .build();
        // custom lại decode để nếu lỗi thì in log ra console
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) {
                try {
                    return jwtDecoder.decode(token);
                } catch (Exception e) {
                    System.out.println(">>> JWT error: " + e.getMessage());
                    throw e;
                }

            }

        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, CookieBearerTokenResolver cookieBearerTokenResolver)
            throws Exception {

        // các endpoint được public, không cần login vẫn gọi được
        String[] whiteList = {
                "/",
                "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/register",
                "/storage/**",
                "/api/v1/email/**",
                "/api/v1/password/forgot", "/api/v1/password/otp", "/api/v1/password/reset",
                "/oauth2/**",
                "/login/oauth2/**",
                "/api/v1/recommendations/jobs"

        };
        http

                // tắt CSRF, thường làm vậy khi làm REST API
                .csrf(c -> c.disable())
                // bật CORS theo cấu hình mặc định
                .cors(Customizer.withDefaults())
                // cấu hình quyền truy cập cho từng API
                .authorizeHttpRequests(
                        authz -> authz
                                .requestMatchers(whiteList).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                                .anyRequest().authenticated())
                // cấu hình login bằng Google OAuth2
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
                // cấu hình xác thực bằng JWT khi gọi API
                // checktoken
                .oauth2ResourceServer((oauth2) -> oauth2
                        // lấy token từ header/cookie bằng class custom
                        .bearerTokenResolver(cookieBearerTokenResolver) // cách xác thực token khi gọi API
                        // dùng jwt decoder ở trên để check token
                        .jwt(Customizer.withDefaults())
                        // nếu token sai thì trả lỗi theo class custom
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .exceptionHandling(

                        // cấu hình lỗi bảo mật
                        exceptions -> exceptions
                                // chưa login / token sai -> 401
                                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()) // 401
                                // có login nhưng không đủ quyền -> 403
                                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())) // 403
                // quản lý session
                // IF_REQUIRED nghĩa là chỉ tạo session nếu cần
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // converter này biến claim "permission" trong JWT thành authority của Spring
        // Security
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // không thêm prefix kiểu ROLE_
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        // đọc quyền từ claim tên "permission"
        grantedAuthoritiesConverter.setAuthoritiesClaimName("permission");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

}
