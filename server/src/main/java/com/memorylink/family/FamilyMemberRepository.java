package com.memorylink.family;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByFamilyId(Long familyId);

    List<FamilyMember> findByUserId(Long userId);

    Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);
}
