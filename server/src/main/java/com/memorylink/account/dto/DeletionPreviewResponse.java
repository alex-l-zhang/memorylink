package com.memorylink.account.dto;

public record DeletionPreviewResponse(
        int ownedFamilies,
        int memberLinks,
        int selfProfiles,
        int myMedia,
        int myOralHistories,
        int myConversations
) {
}
