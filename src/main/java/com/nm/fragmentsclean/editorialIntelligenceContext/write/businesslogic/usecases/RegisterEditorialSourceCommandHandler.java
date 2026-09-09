package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSource;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component public class RegisterEditorialSourceCommandHandler implements CommandHandler<RegisterEditorialSourceCommand> {
    private final EditorialSourceRepository sources; private final DateTimeProvider clock;
    public RegisterEditorialSourceCommandHandler(EditorialSourceRepository sources, DateTimeProvider clock) { this.sources=sources; this.clock=clock; }
    @Override @Transactional public void execute(RegisterEditorialSourceCommand command) {
        if (sources.byId(command.sourceId()).isPresent()) throw new IllegalStateException("Editorial source already exists: " + command.sourceId());
        sources.save(EditorialSource.register(command.sourceId(), command.name(), command.accessMode(), command.authorityLevel(), command.endpoint(), command.pollingFrequency(), clock.now()));
    }
}
