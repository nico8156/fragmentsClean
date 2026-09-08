package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.SourceSignal;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coordinates three separate boundaries: transactional claim, remote discovery
 * without a database transaction, then transactional completion or backoff.
 */
@Component
public final class ConsultEditorialSourceCommandHandler implements CommandHandler<ConsultEditorialSourceCommand> {
    private final ClaimEditorialSourceConsultationCommandHandler claim;
    private final EditorialSourceRepository sources;
    private final Map<com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceAccessMode, EditorialSourceDiscoveryPort> discoveries;
    private final CompleteEditorialSourceConsultation complete;
    private final FailEditorialSourceConsultation fail;
    private final DateTimeProvider clock;

    public ConsultEditorialSourceCommandHandler(ClaimEditorialSourceConsultationCommandHandler claim,
                                                EditorialSourceRepository sources,
                                                List<EditorialSourceDiscoveryPort> discoveries,
                                                CompleteEditorialSourceConsultation complete,
                                                FailEditorialSourceConsultation fail,
                                                DateTimeProvider clock) {
        this.claim = claim;
        this.sources = sources;
        this.discoveries = discoveries.stream().collect(Collectors.toUnmodifiableMap(EditorialSourceDiscoveryPort::accessMode, Function.identity()));
        this.complete = complete;
        this.fail = fail;
        this.clock = clock;
    }

    @Override
    public void execute(ConsultEditorialSourceCommand command) {
        claim.execute(new ClaimEditorialSourceConsultationCommand(command.sourceId(), command.workerId(), command.leaseUntil()));
        var source = sources.byId(command.sourceId())
                .orElseThrow(() -> new IllegalStateException("Editorial source not found: " + command.sourceId()));
        var snapshot = source.snapshot();
        var discovery = discoveries.get(snapshot.accessMode());
        if (discovery == null) {
            fail.execute(command.sourceId(), command.workerId(), "UNSUPPORTED_ACCESS_MODE");
            return;
        }

        try {
            var result = discovery.discover(snapshot.endpoint(), snapshot.checkpoint().etag(), snapshot.checkpoint().lastModified());
            var newItems = itemsAfterCheckpoint(result.items(), snapshot.checkpoint().lastExternalId());
            var discoveredAt = clock.now();
            var signals = newItems.stream().map(item -> new SourceSignal(UUID.randomUUID(), command.sourceId(), item.externalId(),
                    item.title(), item.summary(), item.url(), item.author(), item.publishedAt(), discoveredAt, item.fingerprint())).toList();
            complete.execute(command.sourceId(), command.workerId(), signals, result.etag(), result.lastModified(),
                    checkpointExternalId(result, snapshot.checkpoint().lastExternalId()), checkpointPublishedAt(result, snapshot.checkpoint().lastPublishedAt()));
        } catch (EditorialSourceDiscoveryException failure) {
            fail.execute(command.sourceId(), command.workerId(), failure.category().name());
        }
    }

    private static List<EditorialSourceDiscoveryPort.DiscoveredItem> itemsAfterCheckpoint(
            List<EditorialSourceDiscoveryPort.DiscoveredItem> items, String checkpointExternalId) {
        if (checkpointExternalId == null || checkpointExternalId.isBlank()) return items;
        var checkpointIndex = -1;
        for (int index = 0; index < items.size(); index++) {
            if (checkpointExternalId.equals(items.get(index).externalId())) {
                checkpointIndex = index;
                break;
            }
        }
        return checkpointIndex < 0 ? items : items.subList(0, checkpointIndex);
    }

    private static String checkpointExternalId(EditorialSourceDiscoveryPort.DiscoveryResult result, String current) {
        return result.items().isEmpty() ? current : result.items().getFirst().externalId();
    }

    private static Instant checkpointPublishedAt(EditorialSourceDiscoveryPort.DiscoveryResult result, Instant current) {
        return result.items().stream().map(EditorialSourceDiscoveryPort.DiscoveredItem::publishedAt)
                .filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(current);
    }
}
