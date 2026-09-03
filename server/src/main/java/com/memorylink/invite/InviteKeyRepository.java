package com.memorylink.invite;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteKeyRepository extends JpaRepository<InviteKey, Long> {

    Optional<InviteKey> findFirstByCodeHashOrderByIdDesc(String codeHash);
}
