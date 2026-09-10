package com.memorylink.archive;

import com.memorylink.archive.dto.LovedOneRequest;
import com.memorylink.archive.dto.LovedOneResponse;
import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyService;
import com.memorylink.storage.MediaStorage;
import com.memorylink.connection.FamilyRelationshipRepository;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LovedOneService {

    public static final int CODE_NOT_FOUND = 3002;
    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_INVALID_MEDIA_TYPE = 2002;

    private static final Set<String> MEDIA_TYPES = Set.of("PHOTO", "AUDIO", "VIDEO");

    private final LovedOneRepository lovedOneRepository;
    private final MediaFileRepository mediaFileRepository;
    private final FamilyService familyService;
    private final MediaStorage mediaStorage;
    private final FamilyRelationshipRepository relationshipRepository;

    public LovedOneService(LovedOneRepository lovedOneRepository,
                           MediaFileRepository mediaFileRepository,
                           FamilyService familyService,
                           MediaStorage mediaStorage,
                           FamilyRelationshipRepository relationshipRepository) {
        this.lovedOneRepository = lovedOneRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.familyService = familyService;
        this.mediaStorage = mediaStorage;
        this.relationshipRepository = relationshipRepository;
    }

    @Transactional
    public LovedOneResponse create(Long userId, String userName, LovedOneRequest request) {
        Family family = familyService.getOrCreateDefaultFamily(userId, userName);

        LovedOne lovedOne = new LovedOne();
        lovedOne.setFamilyId(family.getId());
        lovedOne.setName(request.name());
        lovedOne.setBirthDate(request.birthDate());
        lovedOne.setDeathDate(request.deathDate());
        lovedOne.setBirthPlace(request.birthPlace());
        lovedOne.setBio(request.bio());
        lovedOne.setCreatedBy(userId);
        lovedOne.setStatus("ACTIVE");
        lovedOne.setDeceased(isDeceasedByDate(request.deathDate()));
        lovedOne = lovedOneRepository.save(lovedOne);
        return toResponse(lovedOne);
    }

    @Transactional(readOnly = true)
    public List<LovedOneResponse> list(Long userId) {
        List<FamilyMember> memberships = familyService.membershipsOf(userId).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .toList();
        List<Long> familyIds = memberships.stream().map(FamilyMember::getFamilyId).toList();
        if (familyIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> roleByFamily = memberships.stream()
                .collect(Collectors.toMap(FamilyMember::getFamilyId, FamilyMember::getRole, (a, b) -> a));
        List<LovedOne> persons = lovedOneRepository.findByFamilyIdInOrderByCreatedAtDesc(familyIds);
        List<LovedOne> result = new ArrayList<>();
        Map<Long, LovedOne> chosenByUser = new LinkedHashMap<>();
        for (LovedOne person : persons) {
            Long personUserId = person.getUserId();
            if (personUserId == null) {
                result.add(person);
                continue;
            }
            LovedOne existing = chosenByUser.get(personUserId);
            if (existing == null) {
                chosenByUser.put(personUserId, person);
                result.add(person);
            } else if (score(person, userId, roleByFamily) > score(existing, userId, roleByFamily)) {
                int index = result.indexOf(existing);
                if (index >= 0) {
                    result.set(index, person);
                }
                chosenByUser.put(personUserId, person);
            }
        }
        return result.stream().map(person -> toResponse(person, relationToMe(userId, person))).toList();
    }

    /** 同一账号在多家族存在多张成员卡时，选择展示优先级更高的一张。 */
    private int score(LovedOne person, Long viewerId, Map<Long, String> roleByFamily) {
        int score = relationToMe(viewerId, person) != null ? 2 : 0;
        if ("OWNER".equals(roleByFamily.get(person.getFamilyId()))) {
            score += 1;
        }
        return score;
    }

    @Transactional(readOnly = true)
    public LovedOneResponse get(Long userId, Long id) {
        return toResponse(requireAccess(userId, id));
    }

    @Transactional
    public LovedOneResponse update(Long userId, Long id, LovedOneRequest request) {
        LovedOne lovedOne = requireAccess(userId, id);
        boolean boundToUser = lovedOne.getUserId() != null;
        if (!lovedOne.effectiveDeceased() && boundToUser) {
            if (!userId.equals(lovedOne.getUserId())) {
                throw new BusinessException(CODE_FORBIDDEN, "仅本人可编辑自己的资料");
            }
        } else if (!familyService.canManage(userId, lovedOne.getFamilyId())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅家族创建者/共建者可编辑该资料");
        }
        lovedOne.setName(request.name());
        lovedOne.setBirthDate(request.birthDate());
        lovedOne.setDeathDate(request.deathDate());
        lovedOne.setBirthPlace(request.birthPlace());
        lovedOne.setBio(request.bio());
        lovedOne.setDeceased(isDeceasedByDate(request.deathDate()));
        lovedOne = lovedOneRepository.save(lovedOne);
        return toResponse(lovedOne);
    }

    @Transactional
    public MediaResponse uploadMedia(Long userId, Long lovedOneId, String mediaType, MultipartFile file) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        String type = mediaType == null ? "" : mediaType.trim().toUpperCase();
        if (!MEDIA_TYPES.contains(type)) {
            throw new BusinessException(CODE_INVALID_MEDIA_TYPE, "mediaType 仅支持 PHOTO/AUDIO/VIDEO");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(2002, "文件不能为空");
        }

        String objectKey = "lovedones/%d/%s%s".formatted(
                lovedOneId, UUID.randomUUID(), extensionOf(file.getOriginalFilename(), type));
        try {
            mediaStorage.put(objectKey, file.getInputStream(), file.getSize(),
                    file.getContentType() == null ? contentTypeOf(type) : file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(5000, "素材上传失败，请稍后重试");
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setLovedOneId(lovedOneId);
        mediaFile.setUploaderId(userId);
        mediaFile.setMediaType(type);
        mediaFile.setObjectKey(objectKey);
        mediaFile.setSizeBytes(file.getSize());
        mediaFile.setStatus("ACTIVE");
        mediaFile = mediaFileRepository.save(mediaFile);
        return toMediaResponse(mediaFile, true);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listMedia(Long userId, Long lovedOneId) {
        requireAccess(userId, lovedOneId);
        return mediaFileRepository.findByLovedOneIdOrderByCreatedAtDesc(lovedOneId)
                .stream().map(mf -> toMediaResponse(mf, true)).toList();
    }

    @Transactional(readOnly = true)
    public String mediaUrl(Long userId, Long lovedOneId, Long mediaId) {
        requireAccess(userId, lovedOneId);
        MediaFile mediaFile = mediaFileRepository.findByIdAndLovedOneId(mediaId, lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "素材不存在"));
        return mediaStorage.presignedGetUrl(mediaFile.getObjectKey());
    }

    @Transactional
    public void deleteMedia(Long userId, Long lovedOneId, Long mediaId) {
        requireAccess(userId, lovedOneId);
        MediaFile mediaFile = mediaFileRepository.findByIdAndLovedOneId(mediaId, lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "素材不存在"));
        mediaStorage.delete(mediaFile.getObjectKey());
        mediaFileRepository.delete(mediaFile);
    }

    private LovedOne requireAccess(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "档案不存在"));
        if (!familyService.canAccess(userId, lovedOne.getFamilyId())) {
            throw new BusinessException(CODE_FORBIDDEN, "无权访问该档案");
        }
        return lovedOne;
    }

    private LovedOneResponse toResponse(LovedOne lovedOne) {
        return toResponse(lovedOne, null);
    }

    private LovedOneResponse toResponse(LovedOne lovedOne, String relationToMe) {
        return new LovedOneResponse(
                lovedOne.getId(),
                lovedOne.getFamilyId(),
                lovedOne.getName(),
                lovedOne.getBirthDate(),
                lovedOne.getDeathDate(),
                lovedOne.getBirthPlace(),
                lovedOne.getBio(),
                lovedOne.getStatus(),
                lovedOne.effectiveDeceased(),
                lovedOne.isAiPersonaEnabled(),
                lovedOne.getUserId(),
                relationToMe,
                lovedOne.getCreatedAt()
        );
    }

    private String relationToMe(Long viewerId, LovedOne person) {
        Long otherId = person.getUserId();
        if (otherId == null || otherId.equals(viewerId)) {
            return null;
        }
        var direct = relationshipRepository.findByUserAIdAndUserBId(viewerId, otherId);
        if (direct.isPresent()) {
            return direct.get().getRelationAToB();
        }
        var reverse = relationshipRepository.findByUserAIdAndUserBId(otherId, viewerId);
        return reverse.map(rel -> rel.getRelationBToA()).orElse(null);
    }

    private boolean isDeceasedByDate(LocalDate deathDate) {
        return deathDate != null && !deathDate.isAfter(LocalDate.now());
    }

    private MediaResponse toMediaResponse(MediaFile mediaFile, boolean withUrl) {
        return new MediaResponse(
                mediaFile.getId(),
                mediaFile.getLovedOneId(),
                mediaFile.getMediaType(),
                mediaFile.getObjectKey(),
                mediaFile.getSizeBytes(),
                mediaFile.getCreatedAt(),
                withUrl ? mediaStorage.presignedGetUrl(mediaFile.getObjectKey()) : null
        );
    }

    private String extensionOf(String originalName, String mediaType) {
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1
                    && originalName.substring(dot + 1).matches("[A-Za-z0-9]{1,8}")) {
                return originalName.substring(dot).toLowerCase();
            }
        }
        return switch (mediaType) {
            case "PHOTO" -> ".jpg";
            case "AUDIO" -> ".m4a";
            default -> ".mp4";
        };
    }

    private String contentTypeOf(String mediaType) {
        return switch (mediaType) {
            case "PHOTO" -> "image/jpeg";
            case "AUDIO" -> "audio/mp4";
            default -> "video/mp4";
        };
    }
}
