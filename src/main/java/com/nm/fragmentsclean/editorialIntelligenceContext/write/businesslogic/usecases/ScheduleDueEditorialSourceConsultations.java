package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceConsultationRequestPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/** Scheduler application use case: discovers due work only; it never consults a provider. */
@Component
public final class ScheduleDueEditorialSourceConsultations {
    private final EditorialSourceRepository sources; private final EditorialSourceConsultationRequestPort requests; private final DateTimeProvider clock;
    public ScheduleDueEditorialSourceConsultations(EditorialSourceRepository sources, EditorialSourceConsultationRequestPort requests, DateTimeProvider clock) { this.sources=sources; this.requests=requests; this.clock=clock; }
    public int execute(int limit, Duration leaseDuration, String workerId) {
        if (limit < 1 || leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative() || workerId == null || workerId.isBlank()) return 0;
        var now = clock.now(); var due = sources.dueAt(now, limit);
        due.forEach(source -> requests.request(new ClaimEditorialSourceConsultationCommand(source.snapshot().id(), workerId + "-" + UUID.randomUUID(), now.plus(leaseDuration))));
        return due.size();
    }
}
