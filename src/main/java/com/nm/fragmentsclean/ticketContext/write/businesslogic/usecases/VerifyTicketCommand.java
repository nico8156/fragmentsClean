package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;

import java.time.Instant;
import java.util.UUID;

public record VerifyTicketCommand(
        UUID commandId,
        UUID ticketId,     // tempId côté front mappé en UUID (ou généré côté front)
        UUID userId,
        String imageRef,   // nullable
        String ocrText,    // nullable (mais au moins un des deux devrait être présent)
        Instant clientAt   // correspond à "at" côté front
) implements AuthenticatedCommand {
    @Override
    public UUID receiptCommandId() {
        return commandId;
    }

    @Override
    public UUID requesterId() {
        return userId;
    }

    @Override
    public String receiptType() {
        return "ticket.verify.v1";
    }
}
