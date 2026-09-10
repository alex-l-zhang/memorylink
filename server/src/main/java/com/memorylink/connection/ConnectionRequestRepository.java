package com.memorylink.connection;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

    List<ConnectionRequest> findByTargetIdAndStatusOrderByCreatedAtDesc(Long targetId, String status);

    List<ConnectionRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, String status);

    Optional<ConnectionRequest> findByIdAndTargetIdAndStatus(Long id, Long targetId, String status);

    boolean existsByRequesterIdAndTargetIdAndStatus(Long requesterId, Long targetId, String status);

    boolean existsByTargetIdAndRequesterIdAndStatus(Long targetId, Long requesterId, String status);

    List<ConnectionRequest> findByRequesterId(Long requesterId);

    List<ConnectionRequest> findByTargetId(Long targetId);

    List<ConnectionRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<ConnectionRequest> findByTargetIdOrderByCreatedAtDesc(Long targetId);
}
