package com.github.dogoodogoo.api.domain.trashbin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrashBinService {

    private final TrashBinRepository trashBinRepository;

    private static final int MAX_RESPONSE_SIZE = 200;       //시야 범위 내 휴지통 조회 반환 건수 제약 200개

    public List<TrashBinResponse> findInBounds(
            Double minLat, Double maxLat,
            Double minLng, Double maxLng,
            Double centerLat, Double centerLng,
            int size) {

        int effectiveSize = Math.min(size, MAX_RESPONSE_SIZE);

        // 동일 좌표 쓰레기통 병합.(일반 쓰레기 + 분리수거)
        List<TrashBin> rawBins = trashBinRepository.findInViewportWithCenterPriority(
                    minLat, maxLat, minLng, maxLng, centerLat, centerLng, PageRequest.of(0, effectiveSize * 2))
                .getContent();

        // 병합 후 최종적으로 서버 제한치(200개)만 반환.
        return mergeDuplicateBins(rawBins).stream()
                .limit(effectiveSize)
                .collect(Collectors.toList());
    }

    private List<TrashBinResponse> mergeDuplicateBins(List<TrashBin> bins) {
        if (bins == null || bins.isEmpty()) return new ArrayList<>();

        Map<String, List<TrashBin>> grouped = bins.stream()
                .collect(Collectors.groupingBy(b -> b.getLatitude() + "," + b.getLongitude()));

        return grouped.values().stream()
                .map(group -> {
                    TrashBin first = group.get(0);

                    String combinedBinType = group.stream()
                            .map(TrashBin::getBinType)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.joining(", "));

                    return TrashBinResponse.builder()
                            .id(first.getId())
                            .cityName(first.getCityName())
                            .address(first.getAddress())
                            .locationDesc(first.getLocationDesc())
                            .latitude(first.getLatitude())
                            .longitude(first.getLongitude())
                            .binType(combinedBinType.isEmpty() ? "가로쓰레기통" : combinedBinType)
                            .build();
                })
                .collect(Collectors.toList());
    }
}