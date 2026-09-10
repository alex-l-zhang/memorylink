package com.memorylink.connection.dto;

import java.util.List;

public record RelationshipGraphResponse(
        GraphNodeResponse self,
        List<GraphNodeResponse> nodes,
        long pendingCount
) {
}
