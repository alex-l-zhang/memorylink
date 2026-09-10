package com.memorylink.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.memorylink.archive.dto.LovedOneRequest;
import com.memorylink.archive.dto.LovedOneResponse;
import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyService;
import com.memorylink.storage.MediaStorage;
import com.memorylink.connection.FamilyRelationshipRepository;
import com.memorylink.family.FamilyMember;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class LovedOneServiceTest {

    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private MediaFileRepository mediaFileRepository;
    @Mock
    private FamilyService familyService;
    @Mock
    private MediaStorage mediaStorage;
    @Mock
    private FamilyRelationshipRepository relationshipRepository;

    private LovedOneService service;

    @BeforeEach
    void setUp() {
        service = new LovedOneService(lovedOneRepository, mediaFileRepository, familyService,
                mediaStorage, relationshipRepository);
    }

    @Test
    void createCreatesUnderDefaultFamily() {
        Family family = new Family();
        family.setId(10L);
        when(familyService.getOrCreateDefaultFamily(1L, "13800138001")).thenReturn(family);

        LovedOne saved = new LovedOne();
        saved.setId(1L);
        saved.setFamilyId(10L);
        saved.setName("张爷爷");
        saved.setCreatedBy(1L);
        when(lovedOneRepository.save(any(LovedOne.class))).thenReturn(saved);

        LovedOneResponse response = service.create(1L, "13800138001",
                new LovedOneRequest("张爷爷", LocalDate.of(1940, 1, 1), LocalDate.of(2020, 5, 1), "上海", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.familyId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("张爷爷");
    }

    @Test
    void getForbiddenForNonFamilyMember() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));
        when(familyService.canAccess(1L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> service.get(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void updateSavesChangedFields() {
        LovedOne existing = new LovedOne();
        existing.setId(1L);
        existing.setFamilyId(9L);
        existing.setName("张爷爷");
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(familyService.canManage(1L, 9L)).thenReturn(true);
        when(lovedOneRepository.save(any(LovedOne.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LovedOneResponse response = service.update(1L, 1L,
                new LovedOneRequest("张爷爷", LocalDate.of(1940, 1, 1), LocalDate.of(2020, 5, 1), "浙江绍兴", "补充的生平"));

        assertThat(response.birthPlace()).isEqualTo("浙江绍兴");
        assertThat(response.bio()).isEqualTo("补充的生平");
    }

    @Test
    void updateOtherMembersBoundProfileDenied() {
        LovedOne other = new LovedOne();
        other.setId(1L);
        other.setFamilyId(9L);
        other.setUserId(2L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(other));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, 1L,
                new LovedOneRequest("改名", null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅本人");
    }

    @Test
    void updateDeceasedByNonManagerDenied() {
        LovedOne deceased = new LovedOne();
        deceased.setId(1L);
        deceased.setFamilyId(9L);
        deceased.setDeathDate(LocalDate.of(2020, 1, 1));
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(deceased));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(familyService.canManage(1L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(1L, 1L,
                new LovedOneRequest("改名", null, LocalDate.of(2020, 1, 1), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家族创建者/共建者");
    }

    @Test
    void uploadMediaStoresObjectAndSavesRecord() throws Exception {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        MediaFile saved = new MediaFile();
        saved.setId(5L);
        saved.setLovedOneId(1L);
        saved.setMediaType("PHOTO");
        saved.setObjectKey("lovedones/1/xxx.jpg");
        saved.setSizeBytes(1024L);
        when(mediaFileRepository.save(any(MediaFile.class))).thenReturn(saved);
        when(mediaStorage.presignedGetUrl(anyString())).thenReturn("http://minio/url");

        MediaResponse response = service.uploadMedia(1L, 1L, "PHOTO", file);

        verify(mediaStorage).put(anyString(), any(), anyLong(), anyString());
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.url()).isEqualTo("http://minio/url");
    }

    @Test
    void deleteMediaRemovesObjectAndRecord() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(5L);
        mediaFile.setLovedOneId(1L);
        mediaFile.setObjectKey("lovedones/1/xxx.jpg");
        when(mediaFileRepository.findByIdAndLovedOneId(5L, 1L)).thenReturn(Optional.of(mediaFile));

        service.deleteMedia(1L, 1L, 5L);

        verify(mediaStorage).delete("lovedones/1/xxx.jpg");
        verify(mediaFileRepository).delete(mediaFile);
    }

    @Test
    void deleteMediaNotFoundThrowsAndDoesNotTouchStorage() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(mediaFileRepository.findByIdAndLovedOneId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteMedia(1L, 1L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("素材不存在");
        verifyNoInteractions(mediaStorage);
    }

    @Test
    void listDeduplicatesMultipleCardsOfSameUser() {
        FamilyMember owner = new FamilyMember();
        owner.setFamilyId(9L);
        owner.setUserId(1L);
        owner.setRole("OWNER");
        owner.setStatus("ACTIVE");
        FamilyMember viewer = new FamilyMember();
        viewer.setFamilyId(10L);
        viewer.setUserId(1L);
        viewer.setRole("VIEWER");
        viewer.setStatus("ACTIVE");
        when(familyService.membershipsOf(1L)).thenReturn(List.of(owner, viewer));

        LovedOne cardA = new LovedOne();
        cardA.setId(1L);
        cardA.setFamilyId(9L);
        cardA.setUserId(1L);
        cardA.setName("测试用户");
        LovedOne cardB = new LovedOne();
        cardB.setId(2L);
        cardB.setFamilyId(10L);
        cardB.setUserId(1L);
        cardB.setName("测试用户");
        when(lovedOneRepository.findByFamilyIdInOrderByCreatedAtDesc(any())).thenReturn(List.of(cardB, cardA));

        List<LovedOneResponse> result = service.list(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).familyId()).isEqualTo(9L);
    }
}
