package com.github.dogoodogoo.api.domain.petplace;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetPlaceService {

    private final PetPlaceRepository petPlaceRepository;

    public List<PetPlaceResponse> findInBounds(Double minLat, Double maxLat, Double minLng, Double maxLng, int size) {
        return petPlaceRepository.findByLocation(minLat, maxLat, minLng, maxLng, PageRequest.of(0, size))
                .getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private PetPlaceResponse convertToDto(PetPlace petPlace){
        return PetPlaceResponse.builder()
                .placeName(petPlace.getPlaceName())
                .category(petPlace.getCategory())
                .address(petPlace.getAddress())
                .latitude(petPlace.getLatitude())
                .longitude(petPlace.getLongitude())
                .petInfo(petPlace.getPetInfo())
                .build();
    }
}
