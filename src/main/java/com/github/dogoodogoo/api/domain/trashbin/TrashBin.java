package com.github.dogoodogoo.api.domain.trashbin;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "trash_bins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrashBin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name")
    private String cityName;

    private String address;

    @Column(name = "location_desc")
    private String locationDesc;

    private Double latitude;

    private Double longitude;

    @Column(name = "bin_type")
    private String binType;

    @Column(name = "bin_place_type")
    private String binPlaceType;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point geom;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}