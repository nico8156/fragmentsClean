package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ArticleGenerationRunTest {
    @Test void completesAnAttemptOnlyOnceAndKeepsProviderMetadata() {
        Instant started = Instant.parse("2026-08-27T10:00:00Z");
        ArticleGenerationRun run = ArticleGenerationRun.start(UUID.randomUUID(), UUID.randomUUID(), 1, "worker-1", started);
        run.succeed("openai", "resp-1", "gpt-4o-mini", "article-generation.v1", started.plusSeconds(4));
        assertEquals(ArticleGenerationRun.Status.SUCCEEDED, run.snapshot().status());
        assertEquals("article-generation.v1", run.snapshot().schemaVersion());
        assertThrows(IllegalStateException.class, () -> run.succeed("openai", "resp-2", "gpt-4o-mini", "article-generation.v1", started.plusSeconds(5)));
    }
}
