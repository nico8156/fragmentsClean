package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.TICKET_EVENTS;
import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.TICKET_VERIFICATION_REQUESTED;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerificationCompletedEventHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler;
import com.nm.fragmentsclean.platform.eventing.contracts.TicketIntegrationEvents;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketAdminEventHandlers;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationProcessManager;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketSqsIntegrationEventHandlers {

    private final SqsIntegrationEventPayloadReader payloadReader;

    public TicketSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Bean
    SqsIntegrationEventHandler ticketVerifyAcceptedReadSqsIntegrationEventHandler(
            TicketVerifyAcceptedEventHandler handler, AccountErasureBarrier barrier) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_EVENTS, "ticket.verify.accepted",
                envelope -> { var event=payloadReader.read(envelope, TicketIntegrationEvents.VerifyAccepted.class);
                    barrier.ifActive(AccountErasureBarrier.Scope.TICKET,event.userId(),
                        ()->handler.handle(TicketIntegrationEventAcl.verifyAccepted(event))); });
    }

    @Bean
    SqsIntegrationEventHandler ticketVerificationCompletedReadSqsIntegrationEventHandler(
            TicketVerificationCompletedEventHandler handler, AccountErasureBarrier barrier) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_EVENTS, "ticket.verification.completed",
                envelope -> { var event=payloadReader.read(envelope, TicketIntegrationEvents.VerificationCompleted.class);
                    barrier.ifActive(AccountErasureBarrier.Scope.TICKET,event.userId(),
                        ()->handler.handle(TicketIntegrationEventAcl.verificationCompleted(event))); });
    }

    @Bean
    SqsIntegrationEventHandler ticketVerificationRequestedSqsIntegrationEventHandler(
            TicketVerificationProcessManager handler, AccountErasureBarrier barrier) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_VERIFICATION_REQUESTED, "ticket.verify.accepted",
                envelope -> { var event=payloadReader.read(envelope, TicketIntegrationEvents.VerifyAccepted.class);
                    barrier.ifActive(AccountErasureBarrier.Scope.TICKET,event.userId(),
                        ()->handler.handle(TicketIntegrationEventAcl.verifyAccepted(event))); });
    }

    @Bean SqsIntegrationEventHandler ticketAdminUpdatedReadSqsIntegrationEventHandler(TicketAdminEventHandlers handler,AccountErasureBarrier barrier) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_EVENTS, "ticket.admin.updated",
                envelope -> {var event=payloadReader.read(envelope,TicketIntegrationEvents.AdminUpdated.class);barrier.ifActive(AccountErasureBarrier.Scope.TICKET,event.userId(),()->handler.updated(TicketIntegrationEventAcl.adminUpdated(event)));});
    }

    @Bean SqsIntegrationEventHandler ticketAdminDeletedReadSqsIntegrationEventHandler(TicketAdminEventHandlers handler,AccountErasureBarrier barrier) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_EVENTS, "ticket.admin.deleted",
                envelope -> {var event=payloadReader.read(envelope,TicketIntegrationEvents.AdminDeleted.class);barrier.ifActive(AccountErasureBarrier.Scope.TICKET,event.userId(),()->handler.deleted(TicketIntegrationEventAcl.adminDeleted(event)));});
    }

    private record SimpleTicketSqsIntegrationEventHandler(
            String destination,
            String eventType,
            java.util.function.Consumer<IntegrationEventEnvelope> handler
    ) implements SqsIntegrationEventHandler {

        @Override
        public SqsIntegrationEventRoute route() {
            return new SqsIntegrationEventRoute(destination, eventType);
        }

        @Override
        public void handle(IntegrationEventEnvelope envelope) {
            handler.accept(envelope);
        }
    }
}
