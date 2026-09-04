package com.memorylink.archive;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OralHistoryRepository extends JpaRepository<OralHistory, Long> {

    List<OralHistory> findByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);

    List<OralHistory> findByUploadedBy(Long uploadedBy);

    List<OralHistory> findByLovedOneIdIn(Collection<Long> lovedOneIds);
}
