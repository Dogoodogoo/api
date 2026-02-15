package com.github.dogoodogoo.api.domain.facility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dogoodogoo.api.infra.adapter.PetTourApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetFacilityService {

    private final PetTourApiAdapter petTourApiAdapter;
    private final ObjectMapper objectMapper;

    public PetFacilityResponse getFacilities(int page, int size) {
        try {
            // 1. 원시 데이터(JSON) 가져오기
            String json = petTourApiAdapter.fetchPetTourV2List(page, size);
            
            //응답 데이터 구조 확인을 위한 로그
            log.info("TourAPI Raw Response: {}", json);

            // 2. JSON 파싱
            JsonNode root = objectMapper.readTree(json);

            // 3. 응답 구조 유효성 검사 (response 필드가 있는지 확인)
            if (!root.has("response")) {
                log.error("Invalid API Response Structure: 'response' field missing.");
                throw new RuntimeException("공공데이터 API 응답 구조가 올바르지 않습니다: " + json);
            }

            JsonNode responseNode = root.path("response");
            
            // 4. 헤더 검증
            JsonNode header = responseNode.path("header");
            String resultCode = header.path("resultCode").asText();
            String resultMsg = header.path("resultMsg").asText();

            if (!"0000".equals(resultCode)) {
                log.error("TourAPI Error Detected: Code=[{}], Msg=[{}]", resultCode, resultMsg);
                throw new RuntimeException("공공데이터 서버 응답 오류: " + resultMsg + " (Code: " + resultCode + ")");
            }

            JsonNode body = responseNode.path("body");
            JsonNode itemsNode = body.path("items").path("item");

            List<PetFacilityResponse.FacilityItem> items = new ArrayList<>();

            if (itemsNode.isArray()) {
                for (JsonNode node : itemsNode) {
                    items.add(mapToItem(node));
                }
            } else if (itemsNode.isObject()) {
                items.add(mapToItem(itemsNode));
            }

            return PetFacilityResponse.builder()
                    .totalCount(body.path("totalCount").asInt(0))
                    .pageNo(body.path("pageNo").asInt(page))
                    .numOfRows(body.path("numOfRows").asInt(size))
                    .items(items)
                    .build();

        } catch (Exception e) {
            log.error("Critical error in PetFacilityService", e);
            throw new RuntimeException("시설 데이터 처리 중 기술적 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private PetFacilityResponse.FacilityItem mapToItem(JsonNode node) {
        return PetFacilityResponse.FacilityItem.builder()
                .title(node.path("title").asText("명칭 미상"))
                .address(node.path("addr1").asText("주소 정보 없음"))
                .tel(node.path("tel").asText("전화번호 없음"))
                .latitude(node.path("mapy").asDouble(0.0))
                .longitude(node.path("mapx").asDouble(0.0))
                .categoryId(node.path("contenttypeid").asText("기타"))
                .petInfo(node.path("petTursmInfo").asText(""))
                .petEtiquette(node.path("relaAcdntPntit").asText(""))
                .build();
    }
}