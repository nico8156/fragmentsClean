package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationRequest;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.RetainedTopicCandidateBriefPort;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class StartArticleGenerationFromTopicCandidateTest {
    @Test
    void copies_a_retained_candidate_brief_into_the_existing_article_authoring_flow() {
        var candidateId = UUID.randomUUID();
        RetainedTopicCandidateBriefPort briefs = (id, locale) ->
                new RetainedTopicCandidateBriefPort.Brief(id, "Le climat transforme le café",
                        "Comprendre son influence sur le goût", locale, List.of(
                        new RetainedTopicCandidateBriefPort.Provenance("SCA", "https://sca.coffee/climate", "2026-09-08T08:00:00Z", "AUTHORITATIVE")));
        var authoring = new RecordingAuthoringPort();
        var ids = new ArrayDeque<>(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        var studioGeneration = new StartStudioArticleGeneration(authoring, ids::removeFirst,
                () -> Instant.parse("2026-09-08T12:00:00Z"));
        var useCase = new StartArticleGenerationFromTopicCandidate(briefs, studioGeneration);

        useCase.execute(candidateId, "fr-FR", UUID.randomUUID(), "Nicolas");

        assertThat(authoring.command.subject()).contains(
                "Le climat transforme le café — Comprendre son influence sur le goût",
                "Candidat éditorial: " + candidateId,
                "SCA (AUTHORITATIVE): https://sca.coffee/climate");
        assertThat(authoring.command.locale()).isEqualTo("fr-FR");
    }

    private static final class RecordingAuthoringPort implements ArticleGenerationAuthoringPort {
        private StudioArticleGenerationCommand command;
        @Override public void requestGeneration(StudioArticleGenerationCommand command) { this.command = command; }
    }
}
