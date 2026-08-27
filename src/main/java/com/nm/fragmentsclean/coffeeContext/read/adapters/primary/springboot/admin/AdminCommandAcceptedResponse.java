package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin;

import java.util.UUID;

public record AdminCommandAcceptedResponse(UUID commandId, String status) {
    public static AdminCommandAcceptedResponse pending(UUID commandId) {
        return new AdminCommandAcceptedResponse(commandId, "PENDING");
    }
}
