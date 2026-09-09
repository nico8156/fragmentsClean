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
        // The article command's theme is a bounded value (max 240 characters).
        // Provenance remains owned by editorialIntelligenceContext; it must not be
        // smuggled into the article subject as an unbounded prompt.
        String subject = brief.subject();
        return articles.execute(new StudioArticleGenerationRequest(
                subject, brief.locale(), operatorId, operatorName));
    }
}
