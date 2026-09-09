package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.TopicCandidateRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ArticleBriefEvidenceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.TopicCandidate;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CreateArticleBriefFromRetainedCandidate;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CreateArticleBriefFromRetainedCandidateTest {
    @Test void only_retained_candidates_can_cross_the_article_boundary() {
        var repository = new CandidateRepository();
        var candidate = TopicCandidate.detect(UUID.randomUUID(), "Sujet", "Angle",
                List.of(UUID.randomUUID()), Instant.now());
        repository.save(candidate);
        var useCase = new CreateArticleBriefFromRetainedCandidate(repository, ids -> List.of(
                new ArticleBriefEvidenceRepository.Evidence("SCA", "https://sca.coffee/news/1",
                        Instant.parse("2026-09-08T08:00:00Z"), "AUTHORITATIVE")));
        assertThatThrownBy(() -> useCase.execute(candidate.snapshot().id(), "fr-FR"))
                .hasMessageContaining("retained");
        candidate.retain();
        var brief = useCase.execute(candidate.snapshot().id(), "fr-FR");
        assertThat(brief.topicCandidateId()).isEqualTo(candidate.snapshot().id());
        assertThat(brief.provenance()).singleElement().satisfies(source -> {
            assertThat(source.sourceName()).isEqualTo("SCA");
            assertThat(source.authorityLevel()).isEqualTo("AUTHORITATIVE");
        });
    }
    private static final class CandidateRepository implements TopicCandidateRepository {
        private TopicCandidate candidate;
        public void save(TopicCandidate candidate) { this.candidate = candidate; }
        public Optional<TopicCandidate> byId(UUID id) { return Optional.ofNullable(candidate); }
        public List<TopicCandidate> list() { return candidate == null ? List.of() : List.of(candidate); }
    }
}
