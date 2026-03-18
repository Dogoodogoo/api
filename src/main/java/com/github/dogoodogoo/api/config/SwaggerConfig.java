package com.github.dogoodogoo.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        Info info = new Info()
                .title("어디가개(Dogoodogoo) API 명세서")
                .description("반려견 산책 경로 추천 및 주변 공간 시설(PostGIS) 조회 API")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("JhinsLog")
                        .url("https://github.com/Dogoodogoo/api/tree/develop"));

        Server devServer = new Server()
                .url("http://jhin.iptime.org:8080")
                .description("개발 서버");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}