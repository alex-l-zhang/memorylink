package com.memorylink.consent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    List<ConsentRecord> findByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);

    Optional<ConsentRecord> findFirstByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);

    @Query(value = "SELECT * FROM memorylink.consent_records "
            + "WHERE consentor_ids @> CAST(CONCAT('[', CAST(:userId AS text), ']') AS jsonb) "
            + "ORDER BY created_at DESC", nativeQuery = true)
    List<ConsentRecord> findByConsentorContaining(@Param("userId") Long userId);

    List<ConsentRecord> findByLovedOneIdIn(java.util.Collection<Long> lovedOneIds);
}
