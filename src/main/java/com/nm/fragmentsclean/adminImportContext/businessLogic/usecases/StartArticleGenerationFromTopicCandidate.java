package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationRequest;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.RetainedTopicCandidateBriefPort;
import java.util.UUID;

public final class StartArticleGenerationFromTopicCandidate {
    private final RetainedTopicCandidateBriefPort briefs;
    private final StartStudioArticleGeneration articles;

    public StartArticleGenerationFromTopicCandidate(RetainedTopicCandidateBriefPort briefs,
                                                     StartStudioArticleGeneration articles) {
        this.briefs = briefs;
        this.articles = articles;
    }

    public StudioArticleGenerationResult execute(UUID candidateId, String locale,
                                                  UUID operatorId, String operatorName) {
        var brief = briefs.load(candidateId, locale);
        String sources = brief.provenance().stream()
                .map(source -> "- " + source.sourceName() + " (" + source.authorityLevel() + "): " + source.sourceUrl())
                .collect(java.util.stream.Collectors.joining("\n"));
        String groundedSubject = brief.subject() + " — " + brief.editorialAngle()
                + "\nCandidat éditorial: " + brief.candidateId()
                + "\nSources à citer et vérifier:\n" + sources;
        return articles.execute(new StudioArticleGenerationRequest(
                groundedSubject, brief.locale(), operatorId, operatorName));
    }
}
