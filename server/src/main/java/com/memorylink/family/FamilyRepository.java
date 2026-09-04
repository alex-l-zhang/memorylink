package com.memorylink.family;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, Long> {

    Optional<Family> findFirstByCreatorIdOrderByIdAsc(Long creatorId);

    List<Family> findByCreatorId(Long creatorId);
}
