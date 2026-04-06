package com.github.dogoodogoo.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 모든 API 경로 맵핑
                .allowedOrigins("https://dogoodogoo.com")   // 프론트엔드 주소 등록
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 허용할 HTTP 메소드
                .allowedHeaders("*")
                .allowCredentials(true) // 쿠키,인증 정보 포함 허용 여부
                .maxAge(3600);
    }
}