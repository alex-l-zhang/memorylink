package com.memorylink.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memorylink.archive.dto.LovedOneRequest;
import com.memorylink.archive.dto.LovedOneResponse;
import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.common.BusinessException;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyRepository;
import com.memorylink.storage.MediaStorage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
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
    private FamilyRepository familyRepository;
    @Mock
    private MediaStorage mediaStorage;

    private LovedOneService service;

    @BeforeEach
    void setUp() {
        service = new LovedOneService(lovedOneRepository, mediaFileRepository, familyRepository, mediaStorage);
    }

    @Test
    void createCreatesDefaultFamilyWhenMissing() {
        when(familyRepository.findFirstByCreatorIdOrderByIdAsc(1L)).thenReturn(Optional.empty());
        Family family = new Family();
        family.setId(10L);
        family.setCreatorId(1L);
        when(familyRepository.save(any(Family.class))).thenReturn(family);

        LovedOne saved = new LovedOne();
        saved.setId(1L);
        saved.setFamilyId(10L);
        saved.setName("张爷爷");
        saved.setCreatedBy(1L);
        when(lovedOneRepository.save(any(LovedOne.class))).thenReturn(saved);

        LovedOneResponse response = service.create(1L, "小明",
                new LovedOneRequest("张爷爷", LocalDate.of(1940, 1, 1), LocalDate.of(2020, 5, 1), "上海", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.familyId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("张爷爷");
    }

    @Test
    void getAccessDeniedForOtherUsersArchive() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setCreatedBy(2L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));

        assertThatThrownBy(() -> service.get(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void uploadMediaStoresObjectAndSavesRecord() throws Exception {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setCreatedBy(1L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));

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
}
