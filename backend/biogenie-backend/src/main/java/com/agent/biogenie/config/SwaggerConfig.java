package com.agent.biogenie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI (Swagger3) 配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("通用后端模板 API")
                        .description("SpringBoot3 + JDK17 + MySQL8 + Redis + MyBatis Plus 通用后端模板")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("开发者")
                                .email("dev@example.com")));
    }
}
