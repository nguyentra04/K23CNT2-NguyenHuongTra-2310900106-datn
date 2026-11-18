package datn.nht.dinesmart.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties props;

    /** Ảnh món ăn tải lên được phục vụ tại /uploads/** */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(props.upload().dir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**").addResourceLocations(dir.toUri().toString() + "/");
    }
}
