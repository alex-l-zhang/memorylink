package com.memorylink.archive;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);

    Optional<MediaFile> findByIdAndLovedOneId(Long id, Long lovedOneId);

    List<MediaFile> findByUploaderId(Long uploaderId);

    List<MediaFile> findByLovedOneIdIn(Collection<Long> lovedOneIds);
}
