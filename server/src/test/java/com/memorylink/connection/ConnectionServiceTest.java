package com.memorylink.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memorylink.audit.AuditService;
import com.memorylink.connection.dto.UserCandidateResponse;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.family.MemberProfileService;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ConnectionRequestRepository requestRepository;
    @Mock
    private FamilyRelationshipRepository relationshipRepository;
    @Mock
    private FamilyService familyService;
    @Mock
    private FamilyMemberRepository familyMemberRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private MemberProfileService memberProfileService;

    private ConnectionService service;

    @BeforeEach
    void setUp() {
        service = new ConnectionService(userRepository, requestRepository, relationshipRepository,
                familyService, familyMemberRepository, auditService, memberProfileService);
    }

    private User user(Long id, String name, String birth, String place) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        if (birth != null) user.setBirthDate(LocalDate.parse(birth));
        user.setBirthPlace(place);
        return user;
    }

    @Test
    void searchReturnsExactNameMatchesExcludingSelf() {
        when(userRepository.findByName("张三")).thenReturn(List.of(
                user(1L, "张三", "1990-03-15", "上海"),
                user(2L, "张三", "1988-11-02", "北京")));

        List<UserCandidateResponse> result = service.search(2L, "张三");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).birthYear()).isEqualTo(1990);
        assertThat(result.get(0).birthMonth()).isEqualTo(3);
        assertThat(result.get(0).birthPlace()).isEqualTo("上海");
    }

    @Test
    void sendCreatesPendingRequests() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "李四", "1990-01-01", "杭州")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "张三", null, null)));

        int sent = service.send(1L, List.of(2L), "SON", "FATHER");

        assertThat(sent).isEqualTo(1);
        verify(requestRepository).save(any(ConnectionRequest.class));
    }

    @Test
    void acceptAddsRelationshipAndMember() {
        ConnectionRequest request = new ConnectionRequest();
        request.setId(10L);
        request.setRequesterId(1L);
        request.setTargetId(2L);
        request.setRelation("CHILD");
        when(requestRepository.findByIdAndTargetIdAndStatus(10L, 2L, "PENDING"))
                .thenReturn(Optional.of(request));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "李四", null, null)));
        Family family = new Family();
        family.setId(9L);
        when(familyService.getOrCreateDefaultFamily(1L, "李四")).thenReturn(family);
        when(familyMemberRepository.existsByFamilyIdAndUserId(9L, 2L)).thenReturn(false);

        service.accept(2L, 10L);

        verify(relationshipRepository).save(any(FamilyRelationship.class));
        verify(familyMemberRepository).save(any());
        assertThat(request.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void outgoingReturnsStatusAndTargetName() {
        ConnectionRequest request = new ConnectionRequest();
        request.setId(11L);
        request.setRequesterId(1L);
        request.setTargetId(2L);
        request.setRelation("SON");
        request.setInverseRelation("FATHER");
        request.setStatus("ACCEPTED");
        when(requestRepository.findByRequesterIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "张三", null, null)));

        var history = service.outgoing(1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).targetName()).isEqualTo("张三");
        assertThat(history.get(0).status()).isEqualTo("ACCEPTED");
        assertThat(history.get(0).relation()).isEqualTo("SON");
    }
}
