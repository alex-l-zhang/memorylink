package com.memorylink.connection.dto;

/**
 * 关系图谱节点：以「我」为中心的一位家人。
 *
 * @param relationFromMe   对方是我的谁（未确认时为 null）
 * @param relationFromOther 我是对方的谁（对方未确认时为 null）
 * @param myStatus          我这一侧的状态：ACTIVE / PENDING / REJECTED
 * @param otherStatus       对方那一侧的状态
 */
public record GraphNodeResponse(
        Long userId,
        String name,
        Integer birthYear,
        Integer birthMonth,
        String birthPlace,
        String gender,
        Long relationshipId,
        String relationFromMe,
        String relationFromOther,
        String myStatus,
        String otherStatus,
        boolean pending,
        boolean isSelf
) {
}
