package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.adapters.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class MakeLikeCommandHandlerTest {

    private FakeLikeRepository likeRepository = new FakeLikeRepository();
    private DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();
    private FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();

    private final UUID LIKE_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID COMMAND_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private MakeLikeCommandHandler handler;

    @BeforeEach
    void setup() {
        dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T10:00:00Z");
        handler = new MakeLikeCommandHandler(
                likeRepository,
                domainEventPublisher,
                dateTimeProvider
        );
    }

    @Test
    void should_like_when_not_liked_yet() {
        // GIVEN : aucun like en base

        // WHEN
        var command = new MakeLikeCommand(
                COMMAND_ID,
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,   // LIKE
                Instant.parse("2023-10-01T09:59:00Z") // clientAt (pour info)
        );
        handler.execute(command);

        // THEN : état persistant
        var snapshot = likeRepository.snapshots.get(LIKE_ID);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.likeId()).isEqualTo(LIKE_ID);
        assertThat(snapshot.userId()).isEqualTo(USER_ID);
        assertThat(snapshot.targetId()).isEqualTo(TARGET_ID);
        assertThat(snapshot.active()).isTrue();
        assertThat(snapshot.updatedAt()).isEqualTo(dateTimeProvider.instantOfNow);

        // THEN : event publié
        assertThat(domainEventPublisher.published).hasSize(1);
        var event = (LikeSetEvent) domainEventPublisher.published.get(0);
        assertThat(event.commandId()).isEqualTo(COMMAND_ID);
        assertThat(event.likeId()).isEqualTo(LIKE_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.targetId()).isEqualTo(TARGET_ID);
        assertThat(event.active()).isTrue();
        assertThat(event.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
    }

    @Test
    void should_unlike_when_already_liked() {
        // GIVEN : un like déjà actif
        likeRepository.snapshots.put(
                LIKE_ID,
                new Like.LikeSnapshot(
                        LIKE_ID,
                        USER_ID,
                        TARGET_ID,
                        true,   // actif
                        Instant.parse("2023-09-30T10:00:00Z")
                )
        );

        // WHEN
        var command = new MakeLikeCommand(
                COMMAND_ID,
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                false,  // UNLIKE
                Instant.parse("2023-10-01T09:59:00Z")
        );
        handler.execute(command);

        // THEN : état persistant
        var snapshot = likeRepository.snapshots.get(LIKE_ID);
        assertThat(snapshot.active()).isFalse();
        assertThat(snapshot.updatedAt()).isEqualTo(dateTimeProvider.instantOfNow);

        // THEN : event publié
        assertThat(domainEventPublisher.published).hasSize(1);
        var event = (LikeSetEvent) domainEventPublisher.published.get(0);
        assertThat(event.active()).isFalse();
    }

    @Test
    void should_not_emit_event_when_state_does_not_change() {
        // GIVEN : like déjà actif
        likeRepository.snapshots.put(
                LIKE_ID,
                new Like.LikeSnapshot(
                        LIKE_ID,
                        USER_ID,
                        TARGET_ID,
                        true,
                        Instant.parse("2023-09-30T10:00:00Z")
                )
        );

        // WHEN : on redemande LIKE alors que c'est déjà liked
        var command = new MakeLikeCommand(
                COMMAND_ID,
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,   // LIKE encore
                Instant.parse("2023-10-01T09:59:00Z")
        );
        handler.execute(command);

        // THEN : l'état est toujours actif (normal)
        var snapshot = likeRepository.snapshots.get(LIKE_ID);
        assertThat(snapshot.active()).isTrue();
        // on pourrait aussi vérifier qu'on n'a pas écrasé updatedAt si tu veux

        // THEN : aucun nouvel event (important pour l'idempotence soft)
        assertThat(domainEventPublisher.published).isEmpty();
    }
}
