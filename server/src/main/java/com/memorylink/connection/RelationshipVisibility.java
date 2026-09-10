package com.memorylink.connection;

/**
 * 关系可见性：关系可以自动存在，但只有"我这一侧已确认"时才对我可见。
 */
public final class RelationshipVisibility {

    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING = "PENDING";
    public static final String REJECTED = "REJECTED";
    public static final String REMOVED = "REMOVED";

    private RelationshipVisibility() {
    }

    /** 该关系对我是否已建立（我确认过，且我给出了对对方的称谓）。 */
    public static boolean visibleTo(FamilyRelationship relationship, Long viewerId) {
        return relationFromMe(relationship, viewerId) != null;
    }

    /** "对方是我的谁"：未确认/已拒绝/已解除时返回 null。 */
    public static String relationFromMe(FamilyRelationship relationship, Long viewerId) {
        if (!isMine(relationship, viewerId)) {
            return null;
        }
        if (REMOVED.equals(relationship.getStatus())) {
            return null;
        }
        if (!ACTIVE.equals(sideStatus(relationship, viewerId))) {
            return null;
        }
        String relation = isUserA(relationship, viewerId)
                ? relationship.getRelationAToB() : relationship.getRelationBToA();
        return relation == null || relation.isBlank() ? null : relation;
    }

    /** "我是对方的谁"：对方未确认时返回 null。 */
    public static String relationFromOther(FamilyRelationship relationship, Long viewerId) {
        if (!isMine(relationship, viewerId)) {
            return null;
        }
        if (REMOVED.equals(relationship.getStatus())) {
            return null;
        }
        Long otherId = otherId(relationship, viewerId);
        if (!ACTIVE.equals(sideStatus(relationship, otherId))) {
            return null;
        }
        String relation = isUserA(relationship, otherId)
                ? relationship.getRelationAToB() : relationship.getRelationBToA();
        return relation == null || relation.isBlank() ? null : relation;
    }

    /** 我这一侧的状态：ACTIVE / PENDING / REJECTED，关系已解除时返回 REMOVED。 */
    public static String myStatus(FamilyRelationship relationship, Long viewerId) {
        if (!isMine(relationship, viewerId)) {
            return null;
        }
        if (REMOVED.equals(relationship.getStatus())) {
            return REMOVED;
        }
        return sideStatus(relationship, viewerId);
    }

    public static String sideStatus(FamilyRelationship relationship, Long userId) {
        if (relationship == null || userId == null) {
            return null;
        }
        if (isUserA(relationship, userId)) {
            return relationship.getAStatus();
        }
        if (isUserB(relationship, userId)) {
            return relationship.getBStatus();
        }
        return null;
    }

    public static Long otherId(FamilyRelationship relationship, Long viewerId) {
        if (relationship == null || viewerId == null) {
            return null;
        }
        if (isUserA(relationship, viewerId)) {
            return relationship.getUserBId();
        }
        if (isUserB(relationship, viewerId)) {
            return relationship.getUserAId();
        }
        return null;
    }

    public static boolean isMine(FamilyRelationship relationship, Long viewerId) {
        return relationship != null && viewerId != null
                && (isUserA(relationship, viewerId) || isUserB(relationship, viewerId));
    }

    private static boolean isUserA(FamilyRelationship relationship, Long userId) {
        return userId != null && userId.equals(relationship.getUserAId());
    }

    private static boolean isUserB(FamilyRelationship relationship, Long userId) {
        return userId != null && userId.equals(relationship.getUserBId());
    }
}
