package com.github.dogoodogoo.api.domain.trashbin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrashBinService {

    private final TrashBinRepository trashBinRepository;

    public List<TrashBinResponse> findInBounds(
            Double minLat, Double maxLat,
            Double minLng, Double maxLng,
            Double centerLat, Double centerLng,
            int size) {

        // 페이지는 첫 번째 페이지(0)를 고정으로 사용하며, 요청한 size만큼 데이터를 가져옵니다.
        return trashBinRepository.findInViewportWithCenterPriority(
                        minLat, maxLat, minLng, maxLng, centerLat, centerLng, PageRequest.of(0, size))
                .getContent()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TrashBinResponse convertToDto(TrashBin trashBin) {
        return TrashBinResponse.builder()
                .cityName(trashBin.getCityName())
                .address(trashBin.getAddress())
                .locationDesc(trashBin.getLocationDesc())
                .latitude(trashBin.getLatitude())
                .longitude(trashBin.getLongitude())
                .binType(trashBin.getBinType())
                .build();
    }
}
