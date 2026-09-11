package com.nm.fragmentsclean.socialContextTest.unit;

import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeUserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.UserBlockChangedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SetUserBlockCommandHandlerTest {
    @Test void block_then_unblock_is_persisted_and_evented() {
        var blocks=new FakeUserBlockRepository(); var events=new FakeDomainEventPublisher();
        var clock=new DeterministicDateTimeProvider(); clock.instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        var handler=new SetUserBlockCommandHandler(blocks,events,clock);
        UUID blocker=UUID.randomUUID(), blocked=UUID.randomUUID(), blockId=UUID.randomUUID();
        handler.execute(new SetUserBlockCommand(UUID.randomUUID(),blockId,blocker,blocked,true,clock.instantOfNow));
        handler.execute(new SetUserBlockCommand(UUID.randomUUID(),blockId,blocker,blocked,false,clock.instantOfNow));
        assertThat(blocks.allSnapshots()).singleElement().satisfies(block -> {
            assertThat(block.active()).isFalse(); assertThat(block.version()).isEqualTo(1);
        });
        assertThat(events.published).hasSize(2).allMatch(UserBlockChangedEvent.class::isInstance);
    }
}
