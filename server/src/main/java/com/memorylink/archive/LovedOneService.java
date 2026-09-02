package com.memorylink.archive;

import com.memorylink.archive.dto.LovedOneRequest;
import com.memorylink.archive.dto.LovedOneResponse;
import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyRepository;
import com.memorylink.storage.MediaStorage;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    private final FamilyRepository familyRepository;
    private final MediaStorage mediaStorage;

    public LovedOneService(LovedOneRepository lovedOneRepository,
                           MediaFileRepository mediaFileRepository,
                           FamilyRepository familyRepository,
                           MediaStorage mediaStorage) {
        this.lovedOneRepository = lovedOneRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.familyRepository = familyRepository;
        this.mediaStorage = mediaStorage;
    }

    @Transactional
    public LovedOneResponse create(Long userId, String userName, LovedOneRequest request) {
        Family family = familyRepository.findFirstByCreatorIdOrderByIdAsc(userId)
                .orElseGet(() -> createDefaultFamily(userId, userName));

        LovedOne lovedOne = new LovedOne();
        lovedOne.setFamilyId(family.getId());
        lovedOne.setName(request.name());
        lovedOne.setBirthDate(request.birthDate());
        lovedOne.setDeathDate(request.deathDate());
        lovedOne.setBirthPlace(request.birthPlace());
        lovedOne.setBio(request.bio());
        lovedOne.setCreatedBy(userId);
        lovedOne.setStatus("ACTIVE");
        lovedOne = lovedOneRepository.save(lovedOne);
        return toResponse(lovedOne);
    }

    @Transactional(readOnly = true)
    public List<LovedOneResponse> list(Long userId) {
        Family family = familyRepository.findFirstByCreatorIdOrderByIdAsc(userId).orElse(null);
        if (family == null) {
            return List.of();
        }
        return lovedOneRepository.findByFamilyIdOrderByCreatedAtDesc(family.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LovedOneResponse get(Long userId, Long id) {
        return toResponse(requireOwned(userId, id));
    }

    @Transactional
    public MediaResponse uploadMedia(Long userId, Long lovedOneId, String mediaType, MultipartFile file) {
        LovedOne lovedOne = requireOwned(userId, lovedOneId);
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
        requireOwned(userId, lovedOneId);
        return mediaFileRepository.findByLovedOneIdOrderByCreatedAtDesc(lovedOneId)
                .stream().map(mf -> toMediaResponse(mf, true)).toList();
    }

    @Transactional(readOnly = true)
    public String mediaUrl(Long userId, Long lovedOneId, Long mediaId) {
        requireOwned(userId, lovedOneId);
        MediaFile mediaFile = mediaFileRepository.findByIdAndLovedOneId(mediaId, lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "素材不存在"));
        return mediaStorage.presignedGetUrl(mediaFile.getObjectKey());
    }

    private Family createDefaultFamily(Long userId, String userName) {
        Family family = new Family();
        family.setName((userName == null ? "我的" : userName) + "的家族");
        family.setCreatorId(userId);
        family.setStatus("ACTIVE");
        return familyRepository.save(family);
    }

    private LovedOne requireOwned(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "档案不存在"));
        if (!userId.equals(lovedOne.getCreatedBy())) {
            throw new BusinessException(CODE_FORBIDDEN, "无权访问该档案");
        }
        return lovedOne;
    }

    private LovedOneResponse toResponse(LovedOne lovedOne) {
        return new LovedOneResponse(
                lovedOne.getId(),
                lovedOne.getFamilyId(),
                lovedOne.getName(),
                lovedOne.getBirthDate(),
                lovedOne.getDeathDate(),
                lovedOne.getBirthPlace(),
                lovedOne.getBio(),
                lovedOne.getStatus(),
                lovedOne.getCreatedAt()
        );
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
