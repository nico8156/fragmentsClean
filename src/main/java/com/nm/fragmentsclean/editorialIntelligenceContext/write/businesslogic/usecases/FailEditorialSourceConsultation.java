package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Persists one source's isolated retry decision after an external failure. */
@Component
public class FailEditorialSourceConsultation {
    private final EditorialSourceRepository sources;
    private final DateTimeProvider clock;

    public FailEditorialSourceConsultation(EditorialSourceRepository sources, DateTimeProvider clock) {
        this.sources = sources;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID sourceId, String workerId, String failureCategory) {
        var source = sources.byId(sourceId)
                .orElseThrow(() -> new IllegalStateException("Editorial source not found: " + sourceId));
        source.failConsultation(workerId, failureCategory, clock.now());
        sources.save(source);
    }
}
