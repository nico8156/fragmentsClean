package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.webSocket;

public record WsAckEnvelope(
        String type,
        String commandId,
        String targetId,
        int count,
        boolean me,
        long version,
        String updatedAt
) {
}
