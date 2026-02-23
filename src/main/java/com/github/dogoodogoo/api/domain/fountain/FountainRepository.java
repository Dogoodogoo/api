package com.github.dogoodogoo.api.domain.fountain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FountainRepository extends JpaRepository<Fountain, Long> {
}
