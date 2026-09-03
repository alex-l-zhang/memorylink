package com.memorylink.archive;

import com.memorylink.archive.dto.OralHistoryResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.FamilyService;
import com.memorylink.storage.MediaStorage;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OralHistoryService {

    public static final int CODE_ARCHIVE_NOT_FOUND = 3002;
    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_INVALID = 2002;

    private static final Set<String> MEDIA_TYPES = Set.of("AUDIO", "VIDEO");
    private static final Set<String> VISIBILITY = Set.of("SELF_ONLY", "FAMILY");

    private final OralHistoryRepository oralHistoryRepository;
    private final LovedOneRepository lovedOneRepository;
    private final MediaFileRepository mediaFileRepository;
    private final FamilyService familyService;
    private final MediaStorage mediaStorage;

    public OralHistoryService(OralHistoryRepository oralHistoryRepository,
                              LovedOneRepository lovedOneRepository,
                              MediaFileRepository mediaFileRepository,
                              FamilyService familyService,
                              MediaStorage mediaStorage) {
        this.oralHistoryRepository = oralHistoryRepository;
        this.lovedOneRepository = lovedOneRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.familyService = familyService;
        this.mediaStorage = mediaStorage;
    }

    @Transactional
    public OralHistoryResponse upload(Long userId, Long lovedOneId, String mediaType,
                                      String title, String transcript, MultipartFile file) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        String type = mediaType == null ? "" : mediaType.trim().toUpperCase();
        if (!MEDIA_TYPES.contains(type)) {
            throw new BusinessException(CODE_INVALID, "口述历史仅支持 AUDIO/VIDEO");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CODE_INVALID, "文件不能为空");
        }
        String objectKey = "oral/%d/%s%s".formatted(
                lovedOneId, UUID.randomUUID(), extensionOf(file.getOriginalFilename(), type));
        try {
            mediaStorage.put(objectKey, file.getInputStream(), file.getSize(),
                    file.getContentType() == null ? contentTypeOf(type) : file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(5000, "上传失败，请稍后重试");
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setLovedOneId(lovedOneId);
        mediaFile.setUploaderId(userId);
        mediaFile.setMediaType(type);
        mediaFile.setObjectKey(objectKey);
        mediaFile.setSizeBytes(file.getSize());
        mediaFile.setStatus("ACTIVE");
        mediaFile = mediaFileRepository.save(mediaFile);

        OralHistory oral = new OralHistory();
        oral.setLovedOneId(lovedOneId);
        oral.setMediaFileId(mediaFile.getId());
        oral.setTitle(title);
        oral.setTranscript(transcript);
        oral.setUploadedBy(userId);
        oral.setVisibility(lovedOne.effectiveDeceased() ? "FAMILY" : "SELF_ONLY");
        oral = oralHistoryRepository.save(oral);
        return toResponse(oral);
    }

    @Transactional(readOnly = true)
    public List<OralHistoryResponse> list(Long userId, Long lovedOneId) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        boolean self = !lovedOne.effectiveDeceased() && userId.equals(lovedOne.getUserId());
        return oralHistoryRepository.findByLovedOneIdOrderByCreatedAtDesc(lovedOneId).stream()
                .filter(o -> lovedOne.effectiveDeceased()
                        || self
                        || "FAMILY".equals(o.getVisibility()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OralHistoryResponse updateVisibility(Long userId, Long lovedOneId, Long oralHistoryId,
                                                String visibility) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        OralHistory oral = oralHistoryRepository.findById(oralHistoryId)
                .filter(o -> o.getLovedOneId().equals(lovedOneId))
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "口述记录不存在"));
        String target = visibility == null ? "" : visibility.trim().toUpperCase();
        if (!VISIBILITY.contains(target)) {
            throw new BusinessException(CODE_INVALID, "visibility 仅支持 SELF_ONLY/FAMILY");
        }
        if (lovedOne.effectiveDeceased()) {
            if (!familyService.canManage(userId, lovedOne.getFamilyId())) {
                throw new BusinessException(CODE_FORBIDDEN, "仅家族创建者/共建者可修改故人口述可见性");
            }
        } else if (!userId.equals(lovedOne.getUserId())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅讲述者本人可修改可见性");
        }
        oral.setVisibility(target);
        oralHistoryRepository.save(oral);
        return toResponse(oral);
    }

    @Transactional
    public void delete(Long userId, Long lovedOneId, Long oralHistoryId) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        OralHistory oral = oralHistoryRepository.findById(oralHistoryId)
                .filter(o -> o.getLovedOneId().equals(lovedOneId))
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "口述记录不存在"));
        if (lovedOne.effectiveDeceased()) {
            if (!familyService.canManage(userId, lovedOne.getFamilyId())) {
                throw new BusinessException(CODE_FORBIDDEN, "仅家族创建者/共建者可删除故人口述");
            }
        } else if (!userId.equals(lovedOne.getUserId())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅讲述者本人可删除口述");
        }
        MediaFile mediaFile = mediaFileRepository.findById(oral.getMediaFileId())
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "口述素材不存在"));
        mediaStorage.delete(mediaFile.getObjectKey());
        mediaFileRepository.delete(mediaFile);
        oralHistoryRepository.delete(oral);
    }

    private LovedOne requireAccess(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (!familyService.canAccess(userId, lovedOne.getFamilyId())) {
            throw new BusinessException(CODE_FORBIDDEN, "无权访问该档案");
        }
        return lovedOne;
    }

    private OralHistoryResponse toResponse(OralHistory oral) {
        MediaFile mediaFile = mediaFileRepository.findById(oral.getMediaFileId()).orElse(null);
        String url = mediaFile == null ? null : mediaStorage.presignedGetUrl(mediaFile.getObjectKey());
        return new OralHistoryResponse(
                oral.getId(),
                oral.getLovedOneId(),
                oral.getMediaFileId(),
                mediaFile == null ? null : mediaFile.getMediaType(),
                oral.getTitle(),
                oral.getTranscript(),
                oral.getVisibility(),
                oral.getUploadedBy(),
                url,
                oral.getCreatedAt()
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
        return "AUDIO".equals(mediaType) ? ".m4a" : ".mp4";
    }

    private String contentTypeOf(String mediaType) {
        return "AUDIO".equals(mediaType) ? "audio/mp4" : "video/mp4";
    }
}
