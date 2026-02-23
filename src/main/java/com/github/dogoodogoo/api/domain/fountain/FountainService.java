package com.github.dogoodogoo.api.domain.fountain;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 음수대 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FountainService {

    private final FountainRepository fountainRepository;

    public List<FountainResponse> findAll(int page, int size) {
        return fountainRepository.findAll(PageRequest.of(page - 1, size))
                .getContent()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private FountainResponse convertToDto(Fountain fountain) {
        return FountainResponse.builder()
                .fountainName(fountain.getFountainName())
                .address(fountain.getAddress())
                .latitude(fountain.getLatitude())
                .longitude(fountain.getLongitude())
                .managedBy(fountain.getManagedBy())
                .build();
    }
}