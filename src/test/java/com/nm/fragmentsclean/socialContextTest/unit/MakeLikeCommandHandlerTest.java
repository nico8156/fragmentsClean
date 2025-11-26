package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class MakeLikeCommandHandlerTest {

    private final UUID LIKE_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID CMD_ID    = UUID.fromString("44444444-4444-4444-4444-444444444444");

    FakeLikeRepository likeRepository = new FakeLikeRepository();
    FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
    DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

    MakeLikeCommandHandler handler;


    @BeforeEach
    void setup() {
        dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T10:00:00Z");
        handler = new MakeLikeCommandHandler(likeRepository, domainEventPublisher, dateTimeProvider);
    }

    @Test
    void should_create_new_like_and_publish_event() {
        // WHEN
        handler.execute(new MakeLikeCommand(
                CMD_ID,
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,
                Instant.parse("2023-10-01T09:59:00Z")
        ));

        // THEN : état
        var snaps = likeRepository.allSnapshots();
        assertThat(snaps).hasSize(1);
        var snap = snaps.getFirst();
        assertThat(snap.likeId()).isEqualTo(LIKE_ID);
        assertThat(snap.userId()).isEqualTo(USER_ID);
        assertThat(snap.targetId()).isEqualTo(TARGET_ID);
        assertThat(snap.active()).isTrue();
        assertThat(snap.version()).isEqualTo(1L);
        assertThat(snap.updatedAt()).isEqualTo(dateTimeProvider.instantOfNow);

        // THEN : event
        assertThat(domainEventPublisher.published).hasSize(1);
        var evt = (LikeSetEvent) domainEventPublisher.published.getFirst();
        assertThat(evt.commandId()).isEqualTo(CMD_ID);
        assertThat(evt.likeId()).isEqualTo(LIKE_ID);
        assertThat(evt.userId()).isEqualTo(USER_ID);
        assertThat(evt.targetId()).isEqualTo(TARGET_ID);
        assertThat(evt.active()).isTrue();
        assertThat(evt.count()).isEqualTo(1L);
        assertThat(evt.version()).isEqualTo(1L);
        assertThat(evt.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(evt.clientAt()).isEqualTo(Instant.parse("2023-10-01T09:59:00Z"));
    }

    @Test
    void should_toggle_like_off_and_decrement_count() {
        // GIVEN : déjà liké
        handler.execute(new MakeLikeCommand(
                CMD_ID,
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,
                Instant.parse("2023-10-01T09:59:00Z")
        ));

        domainEventPublisher.published.clear();
        dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T10:05:00Z");

        // WHEN : unlike
        handler.execute(new MakeLikeCommand(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                false,
                Instant.parse("2023-10-01T10:04:00Z")
        ));

        // THEN
        var snap = likeRepository.allSnapshots().getFirst();
        assertThat(snap.active()).isFalse();
        assertThat(snap.version()).isEqualTo(2L);

        var evt = (LikeSetEvent) domainEventPublisher.published.getFirst();
        assertThat(evt.count()).isEqualTo(0L);
        assertThat(evt.active()).isFalse();
    }

    @Test
    void should_be_idempotent_when_state_does_not_change() {
        // GIVEN
        handler.execute(new MakeLikeCommand(
                CMD_ID,
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,
                Instant.parse("2023-10-01T09:59:00Z")
        ));
        domainEventPublisher.published.clear();

        // WHEN : même état (true -> true)
        handler.execute(new MakeLikeCommand(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,
                Instant.parse("2023-10-01T10:00:00Z")
        ));

        // THEN : pas de nouvel event
        assertThat(domainEventPublisher.published).isEmpty();
    }
}
