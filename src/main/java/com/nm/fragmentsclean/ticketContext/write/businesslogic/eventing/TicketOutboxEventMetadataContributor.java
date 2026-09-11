package com.nm.fragmentsclean.ticketContext.write.businesslogic.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAccountDataErasedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminDeletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminUpdatedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TicketOutboxEventMetadataContributor implements OutboxEventMetadataContributor {
  @Override
  public Optional<OutboxEventMetadata> resolve(DomainEvent event) {
    if (event instanceof TicketVerifyAcceptedEvent ticketEvent) {
      return Optional.of(
          new OutboxEventMetadata(
              "Ticket", ticketEvent.ticketId().toString(), "user:" + ticketEvent.userId()));
    }
    if (event instanceof TicketVerificationCompletedEvent ticketEvent) {
      return Optional.of(
          new OutboxEventMetadata(
              "Ticket", ticketEvent.ticketId().toString(), "user:" + ticketEvent.userId()));
    }
    if (event instanceof TicketAdminUpdatedEvent ticketEvent)
      return Optional.of(
          new OutboxEventMetadata(
              "Ticket", ticketEvent.ticketId().toString(), "user:" + ticketEvent.userId()));
    if (event instanceof TicketAdminDeletedEvent ticketEvent)
      return Optional.of(
          new OutboxEventMetadata(
              "Ticket", ticketEvent.ticketId().toString(), "user:" + ticketEvent.userId()));
    if (event instanceof TicketAccountDataErasedEvent erased)
      return Optional.of(
          new OutboxEventMetadata(
              "TicketAccountDeletion",
              erased.userId().toString(),
              "accountDeletion:" + erased.userId()));
    return Optional.empty();
  }
}
