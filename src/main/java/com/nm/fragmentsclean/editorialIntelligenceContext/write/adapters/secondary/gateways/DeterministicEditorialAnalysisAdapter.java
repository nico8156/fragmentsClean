package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialAnalysisPort;
import java.math.BigDecimal;
import java.util.List;

/** Safe MVP analyzer: one attributable proposal per signal, no remote call or hidden cost. */
public final class DeterministicEditorialAnalysisAdapter implements EditorialAnalysisPort {
    @Override public AnalysisResult analyze(List<Signal> signals) {
        var topics=signals.stream().map(s -> new ProposedTopic(s.title(), angle(s), List.of(s.id()))).toList();
        return new AnalysisResult(topics,"deterministic-v1",0,0,BigDecimal.ZERO);
    }
    private String angle(Signal signal) { return signal.summary()==null||signal.summary().isBlank() ? "Comprendre ce sujet café et ses implications." : signal.summary(); }
}
