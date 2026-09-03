package com.memorylink.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.memorylink.archive.dto.OralHistoryResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.FamilyService;
import com.memorylink.storage.MediaStorage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class OralHistoryServiceTest {

    @Mock
    private OralHistoryRepository oralHistoryRepository;
    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private MediaFileRepository mediaFileRepository;
    @Mock
    private FamilyService familyService;
    @Mock
    private MediaStorage mediaStorage;

    private OralHistoryService service;

    @BeforeEach
    void setUp() {
        service = new OralHistoryService(oralHistoryRepository, lovedOneRepository,
                mediaFileRepository, familyService, mediaStorage);
    }

    private LovedOne person(boolean deceased) {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        lovedOne.setUserId(1L);
        if (deceased) {
            lovedOne.setDeathDate(java.time.LocalDate.of(2020, 1, 1));
        }
        return lovedOne;
    }

    private MultipartFile audioFile() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("story.mp3");
        when(file.getContentType()).thenReturn("audio/mpeg");
        when(file.getSize()).thenReturn(2048L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));
        return file;
    }

    private void stubMediaAndSave() {
        MediaFile saved = new MediaFile();
        saved.setId(10L);
        saved.setLovedOneId(1L);
        saved.setMediaType("AUDIO");
        saved.setObjectKey("oral/1/x.mp3");
        when(mediaFileRepository.save(any(MediaFile.class))).thenReturn(saved);
        when(mediaFileRepository.findById(10L)).thenReturn(Optional.of(saved));
        when(mediaStorage.presignedGetUrl(anyString())).thenReturn("http://minio/url");
        when(oralHistoryRepository.save(any(OralHistory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void uploadToLivingPersonDefaultsSelfOnly() throws Exception {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(person(false)));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        stubMediaAndSave();

        OralHistoryResponse response = service.upload(1L, 1L, "AUDIO", "我的故事", null, audioFile());

        assertThat(response.visibility()).isEqualTo("SELF_ONLY");
        assertThat(response.title()).isEqualTo("我的故事");
    }

    @Test
    void uploadToDeceasedPersonDefaultsFamily() throws Exception {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(person(true)));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        stubMediaAndSave();

        OralHistoryResponse response = service.upload(1L, 1L, "AUDIO", null, null, audioFile());

        assertThat(response.visibility()).isEqualTo("FAMILY");
    }

    @Test
    void livingListHidesSelfOnlyFromOtherMembers() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(person(false)));
        when(familyService.canAccess(2L, 9L)).thenReturn(true);
        OralHistory selfOnly = new OralHistory();
        selfOnly.setId(1L);
        selfOnly.setLovedOneId(1L);
        selfOnly.setMediaFileId(10L);
        selfOnly.setUploadedBy(1L);
        selfOnly.setVisibility("SELF_ONLY");
        OralHistory family = new OralHistory();
        family.setId(2L);
        family.setLovedOneId(1L);
        family.setMediaFileId(11L);
        family.setUploadedBy(1L);
        family.setVisibility("FAMILY");
        when(oralHistoryRepository.findByLovedOneIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(selfOnly, family));
        MediaFile mf = new MediaFile();
        mf.setId(11L);
        mf.setObjectKey("oral/1/x.mp3");
        when(mediaFileRepository.findById(11L)).thenReturn(Optional.of(mf));
        when(mediaStorage.presignedGetUrl(anyString())).thenReturn("http://minio/url");

        List<OralHistoryResponse> list = service.list(2L, 1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(2L);
    }

    @Test
    void livingVisibilityCannotBeChangedByOthers() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(person(false)));
        when(familyService.canAccess(2L, 9L)).thenReturn(true);
        OralHistory oral = new OralHistory();
        oral.setId(1L);
        oral.setLovedOneId(1L);
        oral.setMediaFileId(10L);
        when(oralHistoryRepository.findById(1L)).thenReturn(Optional.of(oral));

        assertThatThrownBy(() -> service.updateVisibility(2L, 1L, 1L, "FAMILY"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅讲述者本人");
    }
}
