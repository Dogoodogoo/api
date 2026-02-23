package com.github.dogoodogoo.api.domain.petplace;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PetPlaceController {

    private final PetPlaceService petPlaceService;

    /**
     * 지도 API와 연동하여 현재 영역 내의 반려견 동반 장소 데이터를 반환합니다.
     * 엔드포인트를 /pet-places로 단축하여 가독성을 높였습니다.
     */
    @GetMapping("/pet-places")
    public Map<String, Object> getPetPlaces(
            @RequestParam Double minLat, @RequestParam Double maxLat,
            @RequestParam Double minLng, @RequestParam Double maxLng,
            @RequestParam(defaultValue = "1000") int size) {

        return Map.of("items", petPlaceService.findInBounds(minLat, maxLat, minLng, maxLng, size));
    }
}
