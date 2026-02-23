package com.github.dogoodogoo.api.domain.fountain;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "managed_by")
    private String managedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
