package com.github.dogoodogoo.api.domain.facility;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class PetFacilityController {

    private final PetFacilityService petFacilityService;

    @GetMapping
    public PetFacilityResponse getFacilities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return petFacilityService.getFacilities(page, size);
    }
}