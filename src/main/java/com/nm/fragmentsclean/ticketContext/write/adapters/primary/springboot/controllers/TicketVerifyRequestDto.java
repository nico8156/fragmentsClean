package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.controllers;

public record TicketVerifyRequestDto(
        String commandId,
        String ticketId,
        String ocrText,     // nullable
        String imageRef,    // nullable
        String clientAt
) {
}
