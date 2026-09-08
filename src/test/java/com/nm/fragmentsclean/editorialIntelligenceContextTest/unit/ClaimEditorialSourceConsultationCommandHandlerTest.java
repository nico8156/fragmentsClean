package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class ClaimEditorialSourceConsultationCommandHandlerTest {
    @Test void claims_a_due_source_without_calling_any_provider() {
        var id = UUID.randomUUID(); var now = Instant.parse("2023-10-01T11:00:00Z");
        var source = EditorialSource.register(id, "SCA", EditorialSourceAccessMode.RSS, EditorialAuthorityLevel.AUTHORITATIVE, "https://sca.coffee/news", Duration.ofHours(6), now);
        var repository = new FakeRepository(source);
        new ClaimEditorialSourceConsultationCommandHandler(repository, new DeterministicDateTimeProvider()).execute(new ClaimEditorialSourceConsultationCommand(id, "scheduler-1", now.plusSeconds(120)));
        assertThat(repository.source.snapshot().leaseOwner()).isEqualTo("scheduler-1");
        assertThat(repository.saves).isEqualTo(1);
    }
    private static final class FakeRepository implements EditorialSourceRepository {
        EditorialSource source; int saves; FakeRepository(EditorialSource source) { this.source=source; }
        public Optional<EditorialSource> byId(UUID id) { return Optional.ofNullable(source); }
        public List<EditorialSource> dueAt(Instant now, int limit) { return List.of(source); }
        public void save(EditorialSource value) { source=value; saves++; }
    }
}
