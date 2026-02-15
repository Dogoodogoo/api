package com.github.dogoodogoo.api.infra.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetTourApiAdapter {

    @Value("${api.service-key:}")
    private String serviceKey;

    private final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2";

    public String fetchPetTourV2List(int page, int perPage) {
        if (serviceKey == null || serviceKey.isBlank()) {
            log.error("API Service Key is missing in secret.yml");
            throw new IllegalStateException("인증키가 설정되지 않았습니다.");
        }

        try {
            // URLEncoder를 통해 Decoding 키를 안전하게 처리하며 목록 조회를 위한 파라미터를 구성합니다.
            StringBuilder urlBuilder = new StringBuilder(BASE_URL);
            urlBuilder.append("?serviceKey=").append(URLEncoder.encode(serviceKey, StandardCharsets.UTF_8));
            urlBuilder.append("&_type=json");
            urlBuilder.append("&numOfRows=").append(perPage);
            urlBuilder.append("&pageNo=").append(page);
            urlBuilder.append("&MobileOS=ETC");
            urlBuilder.append("&MobileApp=DoGooDoGoo");

            // 필요 시 지역 코드(areaCode)를 추가하여 필터링할 수 있습니다.
            //urlBuilder.append("&areaCode=1"); // 1: 서울

            URI uri = new URI(urlBuilder.toString());
            //log.info("Requesting TourAPI areaBasedList2 via: {}", uri);
            log.info("Requesting Seoul Pet Facilities (areaCode: 1) via: {}", uri);

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(uri, String.class);

            if (response == null || response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR")) {
                log.error("TourAPI Auth Failed. Response: {}", response);
                throw new RuntimeException("API 인증 실패");
            }

            return response;

        } catch (Exception e) {
            log.error("Network Error: {}", e.getMessage());
            throw new RuntimeException("외부 API 통신 실패: " + e.getMessage());
        }
    }
}