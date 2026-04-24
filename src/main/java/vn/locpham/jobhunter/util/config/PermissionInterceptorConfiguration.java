package vn.locpham.jobhunter.util.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//lớp phân quyền
//user đã login rồi
//nhưng có được phép gọi API này không
@Configuration
public class PermissionInterceptorConfiguration implements WebMvcConfigurer {
    @Bean
    PermissionInterceptor getPermissionInterceptor() {
        // tạo bean interceptor để Spring quản lý
        return new PermissionInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] whiteList = {

                // các API public / không cần check permission bằng interceptor
                "/", "/api/v1/auth/**", "/storage/**",
                "/api/v1/companies/**", "/api/v1/jobs/**", "/api/v1/skills/**", "/api/v1/files", "/api/v1/email/**",
                "/api/v1/subscribers/**",
                "/api/v1/resumes/**",
                "/api/v1/password/**",
                "/api/v1/expertise/**",
        };
        // đăng ký PermissionInterceptor cho toàn bộ request
        // nhưng loại trừ các path trong whitelist ở trên
        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(whiteList);
    }
}
