package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationRequestPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationScheduleGuard;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationIdPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.ScheduleArticleGeneration;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleArticleGenerationTest {
    @Test
    void scheduled_request_reuses_the_generation_command_and_trigger() {
        var commands = new ArrayDeque<RequestArticleGenerationCommand>();
        ArticleGenerationRequestPort requests = commands::add;
        ArticleGenerationScheduleGuard guard = (subject, now, max, dedup) -> true;
        var ids = new ArrayDeque<UUID>();
        ids.add(UUID.randomUUID()); ids.add(UUID.randomUUID()); ids.add(UUID.randomUUID()); ids.add(UUID.randomUUID());
        ArticleGenerationIdPort idPort = ids::remove;
        var useCase = new ScheduleArticleGeneration(requests, guard, idPort, () -> Instant.parse("2026-08-27T10:00:00Z"));

        assertThat(useCase.execute("Origines du café", "fr-FR", 2, 168)).isTrue();
        assertThat(commands).singleElement().satisfies(command -> {
            assertThat(command.trigger().name()).isEqualTo("SCHEDULED");
            assertThat(command.theme()).isEqualTo("Origines du café");
            assertThat(command.slug()).startsWith("origines-du-cafe-");
        });
    }

    @Test
    void scheduled_request_is_skipped_when_the_guard_rejects_it() {
        var calls = new ArrayList<RequestArticleGenerationCommand>();
        var useCase = new ScheduleArticleGeneration(calls::add, (subject, now, max, dedup) -> false,
                UUID::randomUUID, () -> Instant.now());

        assertThat(useCase.execute("Sujet", "fr-FR", 2, 168)).isFalse();
        assertThat(calls).isEmpty();
    }
}
