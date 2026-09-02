package com.memorylink.consent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    List<ConsentRecord> findByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);

    Optional<ConsentRecord> findFirstByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);
}
