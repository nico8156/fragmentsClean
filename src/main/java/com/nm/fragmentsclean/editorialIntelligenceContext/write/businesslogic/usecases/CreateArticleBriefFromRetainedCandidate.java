package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.TopicCandidateRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ArticleBriefEvidenceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.ArticleBriefV1;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.TopicCandidateStatus;
import java.util.List;
import java.util.UUID;

public final class CreateArticleBriefFromRetainedCandidate {
    private final TopicCandidateRepository candidates;
    private final ArticleBriefEvidenceRepository evidence;

    public CreateArticleBriefFromRetainedCandidate(TopicCandidateRepository candidates, ArticleBriefEvidenceRepository evidence) {
        this.candidates = candidates;
        this.evidence = evidence;
    }

    public ArticleBriefV1 execute(UUID candidateId, String locale) {
        var candidate = candidates.byId(candidateId)
                .orElseThrow(() -> new IllegalStateException("Topic candidate not found: " + candidateId));
        var snapshot = candidate.snapshot();
        if (snapshot.status() != TopicCandidateStatus.RETAINED) {
            throw new IllegalStateException("Only a retained topic candidate can start article authoring");
        }
        var provenance = evidence.bySignalIds(snapshot.signalIds()).stream()
                .map(item -> new ArticleBriefV1.Provenance(item.sourceName(), item.sourceUrl(),
                        item.publishedAt() == null ? null : item.publishedAt().toString(), item.authorityLevel()))
                .toList();
        if (provenance.size() != snapshot.signalIds().size()) {
            throw new IllegalStateException("Retained candidate evidence is incomplete");
        }
        return new ArticleBriefV1(snapshot.id(), snapshot.subject(), snapshot.suggestedAngle(), locale, List.of(), provenance);
    }
}
