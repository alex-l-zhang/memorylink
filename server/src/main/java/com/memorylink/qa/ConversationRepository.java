package com.memorylink.qa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findTop10ByLovedOneIdOrderByCreatedAtDesc(Long lovedOneId);
}
