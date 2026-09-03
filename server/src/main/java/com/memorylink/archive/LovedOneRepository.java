package com.memorylink.archive;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LovedOneRepository extends JpaRepository<LovedOne, Long> {

    List<LovedOne> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    List<LovedOne> findByFamilyIdInOrderByCreatedAtDesc(Collection<Long> familyIds);

    Optional<LovedOne> findFirstByUserIdOrderByIdAsc(Long userId);
}
