package com.memorylink.archive;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LovedOneRepository extends JpaRepository<LovedOne, Long> {

    List<LovedOne> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
