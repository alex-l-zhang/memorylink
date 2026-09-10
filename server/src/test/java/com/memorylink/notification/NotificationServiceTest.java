package com.memorylink.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.audit.AuditService;
import com.memorylink.connection.FamilyRelationship;
import com.memorylink.connection.FamilyRelationshipRepository;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.MemberProfileService;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FamilyRelationshipRepository relationshipRepository;
    @Mock
    private FamilyMemberRepository familyMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private MemberProfileService memberProfileService;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, relationshipRepository,
                familyMemberRepository, userRepository, auditService, lovedOneRepository,
                memberProfileService);
    }

    private FamilyMember member(Long familyId, Long userId) {
        FamilyMember member = new FamilyMember();
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setStatus("ACTIVE");
        member.setRole("VIEWER");
        return member;
    }

    private FamilyRelationship confirmedRelationship(Long userA, Long userB) {
        FamilyRelationship relationship = new FamilyRelationship();
        relationship.setId(100L);
        relationship.setUserAId(userA);
        relationship.setUserBId(userB);
        relationship.setRelationAToB("FATHER");
        relationship.setRelationBToA("DAUGHTER");
        relationship.setStatus("ACTIVE");
        relationship.setAStatus("ACTIVE");
        relationship.setBStatus("ACTIVE");
        return relationship;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    @Test
    void fanoutCoversBothCirclesSoFamilyMembersGetMessages() {
        // 张力(4) 加入张耘嘉(48) 的家族 43；张力自己的家族 10 里有张耘嫣(22)
        when(familyMemberRepository.findByUserId(4L))
                .thenReturn(List.of(member(10L, 4L), member(43L, 4L)));
        when(familyMemberRepository.findByUserId(48L)).thenReturn(List.of(member(43L, 48L)));
        when(familyMemberRepository.findByFamilyId(10L))
                .thenReturn(List.of(member(10L, 22L), member(10L, 4L)));
        when(familyMemberRepository.findByFamilyId(43L))
                .thenReturn(List.of(member(43L, 48L), member(43L, 4L)));
        when(relationshipRepository.findByUserAId(4L))
                .thenReturn(List.of(confirmedRelationship(22L, 4L)));
        when(relationshipRepository.findByUserAId(48L)).thenReturn(List.of());
        when(relationshipRepository.findByUserBId(any())).thenReturn(List.of());
        when(relationshipRepository.findByUserAIdAndUserBId(any(), any())).thenReturn(Optional.empty());
        when(relationshipRepository.findByUserBIdAndUserAId(any(), any())).thenReturn(Optional.empty());
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, "张力")));
        when(userRepository.findById(22L)).thenReturn(Optional.of(user(22L, "张耘嫣")));
        when(userRepository.findById(48L)).thenReturn(Optional.of(user(48L, "张耘嘉")));
        when(notificationRepository.existsByRecipientIdAndRelationshipIdAndStatusIn(any(), any(), anyList()))
                .thenReturn(false);

        service.fanoutOnJoin(43L, 4L, 48L, "SON", null);

        List<FamilyRelationship> relationships = new ArrayList<>();
        ArgumentCaptor<FamilyRelationship> relationshipCaptor =
                ArgumentCaptor.forClass(FamilyRelationship.class);
        verify(relationshipRepository, atLeastOnce()).save(relationshipCaptor.capture());
        relationships.addAll(relationshipCaptor.getAllValues());
        assertThat(relationships).anyMatch(r ->
                (r.getUserAId().equals(48L) && r.getUserBId().equals(22L))
                        || (r.getUserAId().equals(22L) && r.getUserBId().equals(48L)));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
        List<Notification> notifications = notificationCaptor.getAllValues();
        assertThat(notifications).anyMatch(n -> n.getRecipientId().equals(22L));
        assertThat(notifications).anyMatch(n -> n.getRecipientId().equals(48L));
    }

    @Test
    void confirmMovesUserIntoOtherFamilyWhenTheyShareNone() {
        Notification notification = new Notification();
        notification.setId(5L);
        notification.setRecipientId(22L);
        notification.setType("RELATION_CONFIRM");
        notification.setRelationshipId(9L);
        FamilyRelationship relationship = new FamilyRelationship();
        relationship.setId(9L);
        relationship.setUserAId(22L);
        relationship.setUserBId(48L);
        relationship.setStatus("ACTIVE");
        relationship.setAStatus("PENDING");
        relationship.setBStatus("PENDING");
        when(notificationRepository.findByIdAndRecipientId(5L, 22L))
                .thenReturn(Optional.of(notification));
        when(relationshipRepository.findById(9L)).thenReturn(Optional.of(relationship));
        when(familyMemberRepository.findByUserId(22L)).thenReturn(List.of(member(10L, 22L)));
        when(familyMemberRepository.findByUserId(48L)).thenReturn(List.of(member(43L, 48L)));
        LovedOne selfCard = new LovedOne();
        selfCard.setId(70L);
        selfCard.setFamilyId(43L);
        selfCard.setUserId(48L);
        when(lovedOneRepository.findFirstByUserIdOrderByIdAsc(48L)).thenReturn(Optional.of(selfCard));

        service.confirm(22L, 5L, "FATHER");

        assertThat(relationship.getAStatus()).isEqualTo("ACTIVE");
        assertThat(relationship.getRelationAToB()).isEqualTo("FATHER");
        ArgumentCaptor<FamilyMember> captor = ArgumentCaptor.forClass(FamilyMember.class);
        verify(familyMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo(43L);
        assertThat(captor.getValue().getUserId()).isEqualTo(22L);
        assertThat(captor.getValue().getRelationSource()).isEqualTo("RELATION_CONFIRM");
    }

    @Test
    void confirmDoesNotAddDuplicateMembershipWhenFamilyIsShared() {
        Notification notification = new Notification();
        notification.setId(6L);
        notification.setRecipientId(22L);
        notification.setType("RELATION_CONFIRM");
        notification.setRelationshipId(10L);
        FamilyRelationship relationship = new FamilyRelationship();
        relationship.setId(10L);
        relationship.setUserAId(22L);
        relationship.setUserBId(48L);
        relationship.setStatus("ACTIVE");
        relationship.setAStatus("PENDING");
        relationship.setBStatus("ACTIVE");
        when(notificationRepository.findByIdAndRecipientId(6L, 22L))
                .thenReturn(Optional.of(notification));
        when(relationshipRepository.findById(10L)).thenReturn(Optional.of(relationship));
        when(familyMemberRepository.findByUserId(22L)).thenReturn(List.of(member(43L, 22L)));
        when(familyMemberRepository.findByUserId(48L)).thenReturn(List.of(member(43L, 48L)));

        service.confirm(22L, 6L, "OLDER_SISTER");

        verify(familyMemberRepository, never()).save(any(FamilyMember.class));
        verify(memberProfileService, never()).ensureSelfProfile(any());
    }

    @Test
    void confirmAllUsesSuggestedRelationThenFallback() {
        Notification withSuggestion = new Notification();
        withSuggestion.setId(7L);
        withSuggestion.setRecipientId(48L);
        withSuggestion.setType("RELATION_CONFIRM");
        withSuggestion.setRelationshipId(11L);
        withSuggestion.setStatus("UNREAD");
        withSuggestion.setSuggestedRelation("FATHER");
        Notification withoutSuggestion = new Notification();
        withoutSuggestion.setId(8L);
        withoutSuggestion.setRecipientId(48L);
        withoutSuggestion.setType("RELATION_CONFIRM");
        withoutSuggestion.setRelationshipId(12L);
        withoutSuggestion.setStatus("UNREAD");
        when(notificationRepository.findByRecipientIdAndStatusInOrderByCreatedAtDesc(eq(48L), anyList()))
                .thenReturn(List.of(withSuggestion, withoutSuggestion));
        when(notificationRepository.findByIdAndRecipientId(7L, 48L))
                .thenReturn(Optional.of(withSuggestion));
        when(notificationRepository.findByIdAndRecipientId(8L, 48L))
                .thenReturn(Optional.of(withoutSuggestion));
        FamilyRelationship first = new FamilyRelationship();
        first.setId(11L);
        first.setUserAId(48L);
        first.setUserBId(22L);
        first.setStatus("ACTIVE");
        first.setAStatus("PENDING");
        FamilyRelationship second = new FamilyRelationship();
        second.setId(12L);
        second.setUserAId(48L);
        second.setUserBId(19L);
        second.setStatus("ACTIVE");
        second.setAStatus("PENDING");
        when(relationshipRepository.findById(11L)).thenReturn(Optional.of(first));
        when(relationshipRepository.findById(12L)).thenReturn(Optional.of(second));
        when(familyMemberRepository.findByUserId(48L)).thenReturn(List.of(member(43L, 48L)));
        when(familyMemberRepository.findByUserId(22L)).thenReturn(List.of(member(43L, 22L)));
        when(familyMemberRepository.findByUserId(19L)).thenReturn(List.of(member(43L, 19L)));

        int confirmed = service.confirmAll(48L);

        assertThat(confirmed).isEqualTo(2);
        assertThat(first.getRelationAToB()).isEqualTo("FATHER");
        assertThat(second.getRelationAToB()).isEqualTo("FAMILY");
    }
}
