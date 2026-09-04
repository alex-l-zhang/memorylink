package com.memorylink.account;

import com.memorylink.account.dto.DeletionPreviewResponse;
import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.archive.MediaFile;
import com.memorylink.archive.MediaFileRepository;
import com.memorylink.archive.OralHistory;
import com.memorylink.archive.OralHistoryRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.consent.ConsentRecordRepository;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyRepository;
import com.memorylink.invite.InviteKeyRepository;
import com.memorylink.qa.Conversation;
import com.memorylink.qa.ConversationRepository;
import com.memorylink.storage.MediaStorage;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    public static final int CODE_USER_NOT_FOUND = 3003;
    public static final int CODE_BAD_PASSWORD = 1003;

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final LovedOneRepository lovedOneRepository;
    private final MediaFileRepository mediaFileRepository;
    private final OralHistoryRepository oralHistoryRepository;
    private final ConversationRepository conversationRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final InviteKeyRepository inviteKeyRepository;
    private final MediaStorage mediaStorage;
    private final PasswordEncoder passwordEncoder;
    private final com.memorylink.audit.AuditService auditService;

    public AccountService(UserRepository userRepository,
                          FamilyRepository familyRepository,
                          FamilyMemberRepository familyMemberRepository,
                          LovedOneRepository lovedOneRepository,
                          MediaFileRepository mediaFileRepository,
                          OralHistoryRepository oralHistoryRepository,
                          ConversationRepository conversationRepository,
                          ConsentRecordRepository consentRecordRepository,
                          InviteKeyRepository inviteKeyRepository,
                          MediaStorage mediaStorage,
                          PasswordEncoder passwordEncoder,
                          com.memorylink.audit.AuditService auditService) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.lovedOneRepository = lovedOneRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.oralHistoryRepository = oralHistoryRepository;
        this.conversationRepository = conversationRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.inviteKeyRepository = inviteKeyRepository;
        this.mediaStorage = mediaStorage;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DeletionPreviewResponse preview(Long userId) {
        return new DeletionPreviewResponse(
                familyRepository.findByCreatorId(userId).size(),
                familyMemberRepository.findByUserId(userId).size(),
                lovedOneRepository.findByUserId(userId).size(),
                mediaFileRepository.findByUploaderId(userId).size(),
                oralHistoryRepository.findByUploadedBy(userId).size(),
                conversationRepository.findByUserId(userId).size()
        );
    }

    @Transactional
    public void delete(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(CODE_BAD_PASSWORD, "密码错误，无法注销");
        }
        purge(user);
        auditService.log("USER", userId, "ACCOUNT_DELETED", "user:" + userId,
                Map.of("phone", mask(user.getPhone())));
        userRepository.delete(user);
    }

    private void purge(User user) {
        Long uid = user.getId();
        for (Family family : familyRepository.findByCreatorId(uid)) {
            List<FamilyMember> others = familyMemberRepository.findByFamilyId(family.getId()).stream()
                    .filter(m -> !m.getUserId().equals(uid) && "ACTIVE".equals(m.getStatus()))
                    .toList();
            if (others.isEmpty()) {
                purgeFamily(family.getId());
            } else {
                FamilyMember successor = others.get(0);
                family.setCreatorId(successor.getUserId());
                familyRepository.save(family);
                boolean hasOwner = others.stream().anyMatch(m -> "OWNER".equals(m.getRole()));
                if (!hasOwner) {
                    successor.setRole("OWNER");
                    familyMemberRepository.save(successor);
                }
            }
        }
        familyMemberRepository.deleteAll(familyMemberRepository.findByUserId(uid));
        for (LovedOne person : lovedOneRepository.findByUserId(uid)) {
            purgePerson(person.getId());
        }
        oralHistoryRepository.deleteAll(oralHistoryRepository.findByUploadedBy(uid));
        for (MediaFile mediaFile : mediaFileRepository.findByUploaderId(uid)) {
            deleteMediaObject(mediaFile);
        }
        conversationRepository.deleteAll(conversationRepository.findByUserId(uid));
        inviteKeyRepository.deleteAll(inviteKeyRepository.findByCreatedBy(uid));
        lovedOneRepository.detachCreatedBy(uid);
    }

    private void purgePerson(Long personId) {
        List<Long> ids = List.of(personId);
        consentRecordRepository.deleteAll(consentRecordRepository.findByLovedOneIdIn(ids));
        oralHistoryRepository.deleteAll(oralHistoryRepository.findByLovedOneIdIn(ids));
        for (MediaFile mediaFile : mediaFileRepository.findByLovedOneIdIn(ids)) {
            deleteMediaObject(mediaFile);
        }
        conversationRepository.deleteAll(conversationRepository.findByLovedOneIdIn(ids));
        lovedOneRepository.deleteById(personId);
    }

    private void purgeFamily(Long familyId) {
        for (LovedOne person : lovedOneRepository.findByFamilyId(familyId)) {
            purgePerson(person.getId());
        }
        familyMemberRepository.deleteAll(familyMemberRepository.findByFamilyId(familyId));
        familyRepository.deleteById(familyId);
    }

    private void deleteMediaObject(MediaFile mediaFile) {
        try {
            mediaStorage.delete(mediaFile.getObjectKey());
        } catch (Exception ignored) {
            // 对象可能已不存在，仍继续清理记录
        }
        mediaFileRepository.delete(mediaFile);
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
