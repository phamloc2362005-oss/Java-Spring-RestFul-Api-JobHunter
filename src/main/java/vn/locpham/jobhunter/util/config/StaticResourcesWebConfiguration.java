package vn.locpham.jobhunter.util.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourcesWebConfiguration
        implements WebMvcConfigurer {
    // đường dẫn thư mục chứa file upload, đọc từ application.properties
    @Value("${locpham.upload-file.base-uri}")
    private String baseUri;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // map URL /storage/** tới thư mục file thật trong máy/server
        // ví dụ:
        // /storage/avatar/a.png -> file vật lý tại baseUri + a.png
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(baseUri);
    }
}
