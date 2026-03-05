package com.github.dogoodogoo.api.domain.walk;

import com.github.dogoodogoo.api.domain.trashbin.TrashBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalkPathRepository extends JpaRepository<TrashBin, Long>, WalkPathRepositoryCustom {
}
