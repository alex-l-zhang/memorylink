package com.memorylink.connection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RelationshipVisibilityTest {

    private FamilyRelationship relationship(String status, String aStatus, String bStatus,
                                            String aToB, String bToA) {
        FamilyRelationship relationship = new FamilyRelationship();
        relationship.setId(1L);
        relationship.setUserAId(10L);
        relationship.setUserBId(20L);
        relationship.setStatus(status);
        relationship.setAStatus(aStatus);
        relationship.setBStatus(bStatus);
        relationship.setRelationAToB(aToB);
        relationship.setRelationBToA(bToA);
        return relationship;
    }

    @Test
    void visibleOnlyAfterMySideConfirmed() {
        FamilyRelationship pending = relationship("ACTIVE", "PENDING", "ACTIVE", null, "SON");
        assertThat(RelationshipVisibility.visibleTo(pending, 10L)).isFalse();
        assertThat(RelationshipVisibility.visibleTo(pending, 20L)).isTrue();
        assertThat(RelationshipVisibility.myStatus(pending, 10L)).isEqualTo("PENDING");
    }

    @Test
    void removedRelationshipIsInvisibleToBothSides() {
        FamilyRelationship removed = relationship("REMOVED", "ACTIVE", "ACTIVE", "FATHER", "SON");
        assertThat(RelationshipVisibility.visibleTo(removed, 10L)).isFalse();
        assertThat(RelationshipVisibility.visibleTo(removed, 20L)).isFalse();
        assertThat(RelationshipVisibility.relationFromOther(removed, 10L)).isNull();
    }

    @Test
    void relationFromOtherHiddenUntilOtherSideConfirms() {
        FamilyRelationship mine = relationship("ACTIVE", "ACTIVE", "PENDING", "FATHER", "SON");
        assertThat(RelationshipVisibility.relationFromMe(mine, 10L)).isEqualTo("FATHER");
        assertThat(RelationshipVisibility.relationFromOther(mine, 10L)).isNull();
    }

    @Test
    void rejectedSideStaysHiddenButCanBeConfirmedAgain() {
        FamilyRelationship rejected = relationship("ACTIVE", "REJECTED", "PENDING", null, "SON");
        assertThat(RelationshipVisibility.visibleTo(rejected, 10L)).isFalse();
        assertThat(RelationshipVisibility.myStatus(rejected, 10L)).isEqualTo("REJECTED");
        rejected.setAStatus("ACTIVE");
        rejected.setRelationAToB("FATHER");
        assertThat(RelationshipVisibility.visibleTo(rejected, 10L)).isTrue();
    }
}
