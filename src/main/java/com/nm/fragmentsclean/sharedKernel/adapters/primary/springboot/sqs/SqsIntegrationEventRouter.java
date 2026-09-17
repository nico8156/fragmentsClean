package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.InboxClaim;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.InboxMessageStore;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SqsIntegrationEventRouter implements SqsIntegrationEventRouting {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegrationEventRouter.class);

    private final InboxMessageStore inbox;
    private final Map<SqsIntegrationEventRoute, SqsIntegrationEventHandler> handlers;

    public SqsIntegrationEventRouter(
            InboxMessageStore inbox,
            List<SqsIntegrationEventHandler> handlers
    ) {
        this.inbox = inbox;
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SqsIntegrationEventHandler::route,
                        Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalStateException("Duplicate SQS integration event route " + first.route());
                        }
                ));
    }

    @Override
    public Result route(IntegrationEventEnvelope envelope) {
        var claim = inbox.claim(envelope);
        if (claim.status() == InboxClaim.Status.ALREADY_PROCESSED) {
            log.info("[sqs] processed duplicate suppressed eventId={} destination={}",
                    envelope.eventId(), envelope.destination());
            return Result.alreadyProcessed();
        }
        if (claim.status() == InboxClaim.Status.BUSY) {
            log.info("[sqs] active claim retained eventId={} destination={} leaseUntil={}",
                    envelope.eventId(), envelope.destination(), claim.leaseUntil());
            return Result.busyUntil(claim.leaseUntil());
        }

        try {
            dispatch(envelope);
        } catch (Exception e) {
            inbox.markFailed(envelope, claim.ownerToken(), e);
            throw e;
        }
        if (!inbox.markProcessed(envelope, claim.ownerToken())) {
            throw new IllegalStateException("Inbox lease lost before completion for event " + envelope.eventId());
        }
        return Result.processed();
    }

    private void dispatch(IntegrationEventEnvelope envelope) {
        SqsIntegrationEventHandler handler = handlers.get(new SqsIntegrationEventRoute(
                envelope.destination(),
                envelope.eventType()
        ));
        if (handler == null) {
            log.debug("[sqs] ignored eventId={} type={} destination={}",
                    envelope.eventId(), envelope.eventType(), envelope.destination());
            return;
        }
        handler.handle(envelope);
    }
}
