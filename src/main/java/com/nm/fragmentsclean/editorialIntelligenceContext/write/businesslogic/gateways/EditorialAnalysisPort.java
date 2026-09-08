package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import java.util.List;
import java.util.UUID;

/** ACL for batch analysis; implementation may be deterministic or LLM-backed. */
public interface EditorialAnalysisPort {
    AnalysisResult analyze(List<Signal> signals);
    record Signal(UUID id,String title,String summary,String url) { }
    record ProposedTopic(String subject,String suggestedAngle,List<UUID> signalIds) { }
    record AnalysisResult(List<ProposedTopic> topics,String model,int inputTokens,int outputTokens,java.math.BigDecimal estimatedCost) { }
}
