package com.agent.biogenie.config;

import com.agent.biogenie.common.auth.AuthConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final CorsProperties corsProperties;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var corsRegistration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(AuthConstants.HEADER_ACCESS_TOKEN)
                .maxAge(3600)
                .allowCredentials(corsProperties.isAllowCredentials());

        // allowCredentials=true 时不能用 allowedOrigins("*")，需用 allowedOriginPatterns
        if (corsProperties.isAllowAll() || containsWildcardOrigin()) {
            corsRegistration.allowedOriginPatterns("*");
        } else {
            corsRegistration.allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]));
        }
    }

    private boolean containsWildcardOrigin() {
        return corsProperties.getAllowedOrigins() != null
                && corsProperties.getAllowedOrigins().stream().anyMatch("*"::equals);
    }

    /**
     * 拦截器配置（登录鉴权已迁移至 AOP，此处仅保留日志拦截）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/*.ico",
                        "/*.html"
                );
    }

    /**
     * 静态资源映射 (Swagger UI)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/");
    }
}
