package com.memorylink.archive;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OralHistoryRepository extends JpaRepository<OralHistory, Long> {

    List<OralHistory> findByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);
}
