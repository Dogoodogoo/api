package com.github.dogoodogoo.api.domain.stroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StrollRouteRepository extends JpaRepository<StrollRoute, UUID>, StrollRouteRepositoryCustom {
}