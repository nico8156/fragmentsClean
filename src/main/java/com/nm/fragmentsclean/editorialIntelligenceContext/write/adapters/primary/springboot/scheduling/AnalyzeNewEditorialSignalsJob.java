package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.primary.springboot.scheduling;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.AnalyzeNewEditorialSignals;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name = "fragments.editorial.analysis.schedule.enabled", havingValue = "true")
public final class AnalyzeNewEditorialSignalsJob {
    private final AnalyzeNewEditorialSignals analysis;

    public AnalyzeNewEditorialSignalsJob(AnalyzeNewEditorialSignals analysis) {
        this.analysis = analysis;
    }

    @Scheduled(
            initialDelayString = "${fragments.editorial.analysis.schedule.initial-delay-ms:300000}",
            fixedDelayString = "${fragments.editorial.analysis.schedule.delay-ms:86400000}")
    public void analyze() {
        analysis.execute(50);
    }
}
