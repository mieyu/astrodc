package mie.astronomy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebAccessConfig implements WebMvcConfigurer {
    private final AccessKeyInterceptor accessKeyInterceptor;
    private final AccessProperties properties;

    public WebAccessConfig(AccessKeyInterceptor accessKeyInterceptor, AccessProperties properties) {
        this.accessKeyInterceptor = accessKeyInterceptor;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessKeyInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/access/**", "/analyze/health", "/error");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
