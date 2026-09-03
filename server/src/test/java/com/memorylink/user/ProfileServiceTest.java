package com.memorylink.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.memorylink.user.dto.ProfileResponse;
import com.memorylink.user.dto.ProfileUpdateRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository);
    }

    @Test
    void updateSetsBirthDateAndName() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138001");
        user.setName("旧名");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileResponse response = profileService.update(1L,
                new ProfileUpdateRequest("新名", LocalDate.of(1990, 5, 1)));

        assertThat(response.name()).isEqualTo("新名");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 5, 1));
    }
}
