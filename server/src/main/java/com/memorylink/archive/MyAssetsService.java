package com.memorylink.archive;

import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.archive.dto.MySelfResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyService;
import com.memorylink.storage.MediaStorage;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MyAssetsService {

    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_INVALID = 2002;
    public static final int CODE_NOT_FOUND = 3002;

    private static final Set<String> MEDIA_TYPES = Set.of("PHOTO", "AUDIO", "VIDEO");

    private final LovedOneRepository lovedOneRepository;
    private final MediaFileRepository mediaFileRepository;
    private final FamilyService familyService;
    private final MediaStorage mediaStorage;
    private final UserRepository userRepository;

    public MyAssetsService(LovedOneRepository lovedOneRepository,
                           MediaFileRepository mediaFileRepository,
                           FamilyService familyService,
                           MediaStorage mediaStorage,
                           UserRepository userRepository) {
        this.lovedOneRepository = lovedOneRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.familyService = familyService;
        this.mediaStorage = mediaStorage;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MySelfResponse selfPerson(Long userId) {
        return selfPersonEntity(userId).map(this::toSelfResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listMedia(Long userId) {
        Optional<LovedOne> self = selfPersonEntity(userId);
        if (self.isEmpty()) {
            return List.of();
        }
        return mediaFileRepository.findByLovedOneIdOrderByCreatedAtDesc(self.get().getId()).stream()
                .map(this::toMediaResponse).toList();
    }

    @Transactional
    public MediaResponse uploadMedia(Long userId, String mediaType, MultipartFile file) {
        LovedOne self = selfPersonEntity(userId).orElseGet(() -> createSelfPerson(userId));
        String type = mediaType == null ? "" : mediaType.trim().toUpperCase();
        if (!MEDIA_TYPES.contains(type)) {
            throw new BusinessException(CODE_INVALID, "mediaType 仅支持 PHOTO/AUDIO/VIDEO");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CODE_INVALID, "文件不能为空");
        }
        String objectKey = "my/%d/%s%s".formatted(
                self.getId(), UUID.randomUUID(), extensionOf(file.getOriginalFilename(), type));
        try {
            mediaStorage.put(objectKey, file.getInputStream(), file.getSize(),
                    file.getContentType() == null ? contentTypeOf(type) : file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(5000, "上传失败，请稍后重试");
        }
        MediaFile mediaFile = new MediaFile();
        mediaFile.setLovedOneId(self.getId());
        mediaFile.setUploaderId(userId);
        mediaFile.setMediaType(type);
        mediaFile.setObjectKey(objectKey);
        mediaFile.setSizeBytes(file.getSize());
        mediaFile.setStatus("ACTIVE");
        mediaFile = mediaFileRepository.save(mediaFile);
        return toMediaResponse(mediaFile);
    }

    @Transactional
    public void deleteMedia(Long userId, Long mediaId) {
        LovedOne self = selfPersonEntity(userId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "素材不存在"));
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .filter(m -> m.getLovedOneId().equals(self.getId()))
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "素材不存在"));
        if (!userId.equals(mediaFile.getUploaderId())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅上传者本人可删除自己的素材");
        }
        mediaStorage.delete(mediaFile.getObjectKey());
        mediaFileRepository.delete(mediaFile);
    }

    private Optional<LovedOne> selfPersonEntity(Long userId) {
        return lovedOneRepository.findFirstByUserIdOrderByIdAsc(userId)
                .filter(p -> !p.effectiveDeceased());
    }

    private LovedOne createSelfPerson(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "用户不存在"));
        Family family = familyService.getOrCreateDefaultFamily(userId, user.getName());
        LovedOne person = new LovedOne();
        person.setFamilyId(family.getId());
        person.setName(user.getName() == null ? "我的档案" : user.getName());
        person.setCreatedBy(userId);
        person.setUserId(userId);
        person.setDeceased(false);
        person.setStatus("ACTIVE");
        return lovedOneRepository.save(person);
    }

    private MySelfResponse toSelfResponse(LovedOne lovedOne) {
        return new MySelfResponse(lovedOne.getId(), lovedOne.getName(),
                lovedOne.effectiveDeceased(), lovedOne.isAiPersonaEnabled());
    }

    private MediaResponse toMediaResponse(MediaFile mediaFile) {
        return new MediaResponse(
                mediaFile.getId(),
                mediaFile.getLovedOneId(),
                mediaFile.getMediaType(),
                mediaFile.getObjectKey(),
                mediaFile.getSizeBytes(),
                mediaFile.getCreatedAt(),
                mediaStorage.presignedGetUrl(mediaFile.getObjectKey())
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
