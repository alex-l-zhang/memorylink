package com.memorylink.connection;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRelationshipRepository extends JpaRepository<FamilyRelationship, Long> {

    Optional<FamilyRelationship> findByUserAIdAndUserBId(Long userAId, Long userBId);

    Optional<FamilyRelationship> findByUserBIdAndUserAId(Long userBId, Long userAId);

    List<FamilyRelationship> findByUserAId(Long userAId);

    List<FamilyRelationship> findByUserBId(Long userBId);
}
