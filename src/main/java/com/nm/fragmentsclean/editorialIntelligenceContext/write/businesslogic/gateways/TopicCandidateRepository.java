package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.TopicCandidate;
public interface TopicCandidateRepository { void save(TopicCandidate candidate); java.util.Optional<TopicCandidate> byId(java.util.UUID candidateId); java.util.List<TopicCandidate> list(); }
