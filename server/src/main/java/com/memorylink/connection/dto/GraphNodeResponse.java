package com.memorylink.connection.dto;

/**
 * 关系图谱节点：以「我」为中心的一位家人。
 *
 * @param relationFromMe   对方是我的谁（未确认时为 null）
 * @param relationFromOther 我是对方的谁（对方未确认时为 null）
 * @param myStatus          我这一侧的状态：ACTIVE / PENDING / REJECTED
 * @param otherStatus       对方那一侧的状态
 * @param extended          是否为"家人的家人"（二级节点，我与 TA 尚无关系）
 * @param viaUserId         二级节点的关系路径经过谁（例如"张耘嫣"）
 * @param viaUserName       路径中间人的姓名
 * @param relationFromVia   中间人对 TA 的称谓（例如 MOTHER → "张耘嫣的母亲"）
 * @param requestPending    我已向 TA 发起建立联系、等待对方同意
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
        boolean isSelf,
        boolean extended,
        Long viaUserId,
        String viaUserName,
        String relationFromVia,
        boolean requestPending
) {
}
