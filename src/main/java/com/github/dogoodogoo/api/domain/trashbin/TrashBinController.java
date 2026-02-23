package com.github.dogoodogoo.api.domain.trashbin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrashBinController {

    private final TrashBinService trashBinService;

    @GetMapping("/trash-bins")
    public Map<String, Object> getTrashBins(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng,
            @RequestParam Double centerLat,
            @RequestParam Double centerLng,
            @RequestParam(defaultValue = "1000") int size) {

        return Map.of("items", trashBinService.findInBounds(
                minLat, maxLat, minLng, maxLng, centerLat, centerLng, size));
    }

}
