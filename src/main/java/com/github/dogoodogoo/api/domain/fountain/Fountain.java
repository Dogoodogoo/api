package com.github.dogoodogoo.api.domain.fountain;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "drinking_fountains")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Fountain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fountain_name")
    private String fountainName;

    private String address;

    private Double latitude;

    private Double longitude;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point geom;

    @Column(name = "managed_by")
    private String managedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
