package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.RetainedTopicCandidateBriefPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CreateArticleBriefFromRetainedCandidate;
import java.util.UUID;

public final class RetainedTopicCandidateBriefAdapter implements RetainedTopicCandidateBriefPort {
    private final CreateArticleBriefFromRetainedCandidate briefs;

    public RetainedTopicCandidateBriefAdapter(CreateArticleBriefFromRetainedCandidate briefs) {
        this.briefs = briefs;
    }

    @Override
    public Brief load(UUID candidateId, String locale) {
        var brief = briefs.execute(candidateId, locale);
        return new Brief(brief.topicCandidateId(), brief.subject(), brief.editorialAngle(), brief.locale(),
                brief.provenance().stream().map(item -> new Provenance(item.sourceName(), item.sourceUrl(),
                        item.publishedAt(), item.authorityLevel())).toList());
    }
}
