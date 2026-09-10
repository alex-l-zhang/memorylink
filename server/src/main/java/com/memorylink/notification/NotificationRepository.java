package com.memorylink.notification;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndStatusInOrderByCreatedAtDesc(Long recipientId, List<String> statuses);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndStatusIn(Long recipientId, List<String> statuses);

    boolean existsByRecipientIdAndRelationshipIdAndStatusIn(
            Long recipientId, Long relationshipId, List<String> statuses);

    void deleteByRecipientIdOrOtherUserId(Long recipientId, Long otherUserId);

    void deleteByRelationshipIdIn(Collection<Long> relationshipIds);
}
