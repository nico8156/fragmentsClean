package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.TICKET_EVENTS;
import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.TICKET_VERIFICATION_REQUESTED;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerificationCompletedEventHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.ProcessTicketVerificationEventHandler;
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
            TicketVerifyAcceptedEventHandler handler) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_EVENTS, "ticket.verify.accepted",
                envelope -> handler.handle(payloadReader.read(envelope, TicketVerifyAcceptedEvent.class)));
    }

    @Bean
    SqsIntegrationEventHandler ticketVerificationCompletedReadSqsIntegrationEventHandler(
            TicketVerificationCompletedEventHandler handler) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_EVENTS, "ticket.verification.completed",
                envelope -> handler.handle(payloadReader.read(envelope, TicketVerificationCompletedEvent.class)));
    }

    @Bean
    SqsIntegrationEventHandler ticketVerificationRequestedSqsIntegrationEventHandler(
            ProcessTicketVerificationEventHandler handler) {
        return new SimpleTicketSqsIntegrationEventHandler(TICKET_VERIFICATION_REQUESTED, "ticket.verify.accepted",
                envelope -> handler.handle(payloadReader.read(envelope, TicketVerifyAcceptedEvent.class)));
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
