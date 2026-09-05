package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.platform.eventing.contracts.TicketIntegrationEvents;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminDeletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminUpdatedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;

final class TicketIntegrationEventAcl {
    private TicketIntegrationEventAcl() { }

    static TicketVerifyAcceptedEvent verifyAccepted(TicketIntegrationEvents.VerifyAccepted e) {
        return new TicketVerifyAcceptedEvent(e.eventId(), e.commandId(), e.ticketId(), e.userId(), e.ocrText(), e.imageRef(),
                e.status(), e.version(), e.occurredAt(), e.clientAt());
    }
    static TicketVerificationCompletedEvent verificationCompleted(TicketIntegrationEvents.VerificationCompleted e) {
        var approved = e.approved() == null ? null : new TicketVerificationCompletedEvent.Approved(
                e.approved().amountCents(), e.approved().currency(), e.approved().ticketDate(), e.approved().merchantName(),
                e.approved().merchantAddress(), e.approved().paymentMethod(), e.approved().lineItems().stream()
                .map(item -> new Ticket.TicketLineItem(item.label(), item.quantity(), item.amountCents())).toList());
        var rejected = e.rejected() == null ? null
                : new TicketVerificationCompletedEvent.Rejected(e.rejected().reasonCode(), e.rejected().message());
        return new TicketVerificationCompletedEvent(e.eventId(), e.commandId(), e.ticketId(), e.userId(),
                TicketVerificationCompletedEvent.Outcome.valueOf(e.outcome()), e.version(), e.occurredAt(), e.clientAt(),
                approved, rejected, e.provider(), e.providerTraceId());
    }
    static TicketAdminUpdatedEvent adminUpdated(TicketIntegrationEvents.AdminUpdated e) {
        return new TicketAdminUpdatedEvent(e.eventId(), e.commandId(), e.ticketId(), e.userId(), e.status(), e.ocrText(),
                e.imageRef(), e.amountCents(), e.currency(), e.ticketDate(), e.merchantName(), e.merchantAddress(),
                e.paymentMethod(), e.rejectionReason(), e.version(), e.actorUserId(), e.occurredAt());
    }
    static TicketAdminDeletedEvent adminDeleted(TicketIntegrationEvents.AdminDeleted e) {
        return new TicketAdminDeletedEvent(e.eventId(), e.commandId(), e.ticketId(), e.userId(), e.actorUserId(),
                e.version(), e.occurredAt());
    }
}
