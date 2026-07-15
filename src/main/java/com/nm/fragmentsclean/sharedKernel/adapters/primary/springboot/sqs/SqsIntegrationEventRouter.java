package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SqsIntegrationEventRouter implements SqsIntegrationEventRouting {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegrationEventRouter.class);

    private final InboxMessageRepository inbox;
    private final Map<SqsIntegrationEventRoute, List<SqsIntegrationEventHandler>> handlers;

    public SqsIntegrationEventRouter(
            InboxMessageRepository inbox,
            List<SqsIntegrationEventHandler> handlers
    ) {
        this.inbox = inbox;
        this.handlers = handlers.stream()
                .collect(Collectors.groupingBy(
                        SqsIntegrationEventHandler::route,
                        Collectors.toUnmodifiableList()
                ));
    }

    @Override
    public void route(IntegrationEventEnvelope envelope) {
        if (!inbox.claim(envelope)) {
            log.info("[sqs] duplicate suppressed eventId={} destination={}",
                    envelope.eventId(), envelope.destination());
            return;
        }

        try {
            dispatch(envelope);
            inbox.markProcessed(envelope);
        } catch (Exception e) {
            inbox.markFailed(envelope, e);
            throw e;
        }
    }

    private void dispatch(IntegrationEventEnvelope envelope) {
        List<SqsIntegrationEventHandler> routeHandlers = handlers.get(new SqsIntegrationEventRoute(
                envelope.destination(),
                envelope.eventType()
        ));
        if (routeHandlers == null || routeHandlers.isEmpty()) {
            log.debug("[sqs] ignored eventId={} type={} destination={}",
                    envelope.eventId(), envelope.eventType(), envelope.destination());
            return;
        }
        for (SqsIntegrationEventHandler handler : routeHandlers) {
            handler.handle(envelope);
        }
    }
}
