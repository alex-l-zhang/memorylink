package com.memorylink.archive;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LovedOneRepository extends JpaRepository<LovedOne, Long> {

    List<LovedOne> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    List<LovedOne> findByFamilyId(Long familyId);

    List<LovedOne> findByFamilyIdInOrderByCreatedAtDesc(Collection<Long> familyIds);

    Optional<LovedOne> findFirstByUserIdOrderByIdAsc(Long userId);

    List<LovedOne> findByUserId(Long userId);

    List<LovedOne> findByCreatedBy(Long userId);

    List<LovedOne> findByFamilyIdIn(Collection<Long> familyIds);

    @Modifying
    @Query("UPDATE LovedOne l SET l.createdBy = NULL WHERE l.createdBy = :userId")
    void detachCreatedBy(@Param("userId") Long userId);
}
