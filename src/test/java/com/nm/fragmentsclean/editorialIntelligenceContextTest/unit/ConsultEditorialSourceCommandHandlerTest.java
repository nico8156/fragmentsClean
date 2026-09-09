package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.SourceSignalRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialAuthorityLevel;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSource;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceAccessMode;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.SourceSignal;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ClaimEditorialSourceConsultationCommandHandler;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CompleteEditorialSourceConsultation;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ConsultEditorialSourceCommand;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ConsultEditorialSourceCommandHandler;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.FailEditorialSourceConsultation;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultEditorialSourceCommandHandlerTest {
    private static final Instant NOW = Instant.parse("2023-10-01T11:00:00Z");

    @Test
    void claims_discovers_outside_the_completion_boundary_and_advances_the_checkpoint() {
        var sourceId = UUID.randomUUID();
        var sources = new SourceRepository(source(sourceId));
        var signals = new Signals();
        var clock = new DeterministicDateTimeProvider();
        var handler = handler(sources, signals, clock, new Discovery(DiscoveryResultFixture.newestFirst()));

        handler.execute(new ConsultEditorialSourceCommand(sourceId, "worker-1", NOW.plusSeconds(60)));

        assertThat(signals.saved).extracting(SourceSignal::externalId).containsExactly("new", "old");
        var snapshot = sources.source.snapshot();
        assertThat(snapshot.checkpoint().etag()).isEqualTo("etag-2");
        assertThat(snapshot.checkpoint().lastModified()).isEqualTo("Tue, 08 Sep 2026 10:00:00 GMT");
        assertThat(snapshot.checkpoint().lastExternalId()).isEqualTo("new");
        assertThat(snapshot.leaseOwner()).isNull();
    }

    @Test
    void records_an_isolated_backoff_when_the_provider_is_unavailable() {
        var sourceId = UUID.randomUUID();
        var sources = new SourceRepository(source(sourceId));
        var handler = handler(sources, new Signals(), new DeterministicDateTimeProvider(), new Discovery(null));

        handler.execute(new ConsultEditorialSourceCommand(sourceId, "worker-1", NOW.plusSeconds(60)));

        var snapshot = sources.source.snapshot();
        assertThat(snapshot.failureCount()).isEqualTo(1);
        assertThat(snapshot.leaseOwner()).isNull();
        assertThat(snapshot.nextCheckAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    private static ConsultEditorialSourceCommandHandler handler(SourceRepository sources, Signals signals,
                                                                 DeterministicDateTimeProvider clock, Discovery discovery) {
        var claim = new ClaimEditorialSourceConsultationCommandHandler(sources, clock);
        var complete = new CompleteEditorialSourceConsultation(sources, signals, clock);
        var fail = new FailEditorialSourceConsultation(sources, clock);
        return new ConsultEditorialSourceCommandHandler(claim, sources, List.of(discovery), complete, fail, clock);
    }

    private static EditorialSource source(UUID sourceId) {
        return EditorialSource.register(sourceId, "Daily Coffee News", EditorialSourceAccessMode.RSS,
                EditorialAuthorityLevel.SPECIALIZED_MEDIA, "https://example.test/feed", Duration.ofHours(6), NOW);
    }

    private static final class SourceRepository implements EditorialSourceRepository {
        private EditorialSource source;
        private SourceRepository(EditorialSource source) { this.source = source; }
        @Override public Optional<EditorialSource> byId(UUID id) { return Optional.of(source); }
        @Override public List<EditorialSource> dueAt(Instant now, int limit) { return List.of(); }
        @Override public void save(EditorialSource source) { this.source = source; }
    }

    private static final class Signals implements SourceSignalRepository {
        private final List<SourceSignal> saved = new ArrayList<>();
        @Override public int saveIgnoringDuplicate(List<SourceSignal> signals) { saved.addAll(signals); return signals.size(); }
    }

    private static final class Discovery implements EditorialSourceDiscoveryPort {
        private final DiscoveryResult result;
        private Discovery(DiscoveryResult result) { this.result = result; }
        @Override public EditorialSourceAccessMode accessMode() { return EditorialSourceAccessMode.RSS; }
        @Override public DiscoveryResult discover(String endpoint, String etag, String lastModified) {
            if (result == null) throw EditorialSourceDiscoveryException.remoteFailure("timeout");
            return result;
        }
    }

    private static final class DiscoveryResultFixture {
        private static EditorialSourceDiscoveryPort.DiscoveryResult newestFirst() {
            return EditorialSourceDiscoveryPort.DiscoveryResult.discovered("etag-2", "Tue, 08 Sep 2026 10:00:00 GMT", List.of(
                    new EditorialSourceDiscoveryPort.DiscoveredItem("new", "New", null, "https://example.test/new", null, NOW, "f-new"),
                    new EditorialSourceDiscoveryPort.DiscoveredItem("old", "Old", null, "https://example.test/old", null, NOW.minusSeconds(60), "f-old")
            ));
        }
    }
}
