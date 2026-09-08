package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component public class ReviseEditorialSourceCommandHandler implements CommandHandler<ReviseEditorialSourceCommand> {
    private final EditorialSourceRepository sources; private final DateTimeProvider clock;
    public ReviseEditorialSourceCommandHandler(EditorialSourceRepository sources, DateTimeProvider clock) { this.sources=sources; this.clock=clock; }
    @Override @Transactional public void execute(ReviseEditorialSourceCommand command) {
        var source=sources.byId(command.sourceId()).orElseThrow(() -> new IllegalStateException("Editorial source not found: " + command.sourceId()));
        source.revise(command.name(), command.accessMode(), command.authorityLevel(), command.endpoint(), command.pollingFrequency(), clock.now());
        if (command.enabled()) source.enable(clock.now()); else source.disable();
        sources.save(source);
    }
}
