package com.github.dogoodogoo.api.domain.stroll;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 알고리즘을 통해 생성된 산책 경로 정보를 저장하는 도메인 엔티티.
 */
@Entity
@Table(name = "stroll_routes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrollRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "stroll_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String strollName;

    @Column(nullable = false)
    private Double totalDistance;

    @Column(nullable = false)
    private Integer estimatedTime;

    @Column(nullable = false)
    private Double matchScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrollDogSize dogSize;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "stroll_route_paths", joinColumns = @JoinColumn(name = "stroll_id"))
    @OrderColumn(name = "path_order")
    @Builder.Default
    private List<StrollCoordinate> path = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "stroll_route_waypoints", joinColumns = @JoinColumn(name = "stroll_id"))
    @OrderColumn(name = "waypoint_order")
    @Builder.Default
    private List<StrollWaypoint> waypoints = new ArrayList<>();
}