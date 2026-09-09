package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class ClaimEditorialSourceConsultationCommandHandler implements CommandHandler<ClaimEditorialSourceConsultationCommand> {
    private final EditorialSourceRepository sources;
    private final DateTimeProvider clock;
    public ClaimEditorialSourceConsultationCommandHandler(EditorialSourceRepository sources, DateTimeProvider clock) { this.sources = sources; this.clock = clock; }

    @Override @Transactional public void execute(ClaimEditorialSourceConsultationCommand command) {
        var source = sources.byId(command.sourceId()).orElseThrow(() -> new IllegalStateException("Editorial source not found: " + command.sourceId()));
        source.claimConsultation(command.workerId(), clock.now(), command.leaseUntil());
        sources.save(source);
    }
}
