package com.github.dogoodogoo.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("어디가개(DogooDogoo) API 명세서")
                        .description("반려견 산책 경로 추천 및 주변 공간 시설(PostGIS) 조회 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("주진홍")
                                .url("[https://github.com/dogoodogoo](https://github.com/dogoodogoo)")));
    }
}