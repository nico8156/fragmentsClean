package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceConsultationRequestPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ClaimEditorialSourceConsultationCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.stereotype.Component;

@Component
public final class CommandBusEditorialSourceConsultationRequestAdapter implements EditorialSourceConsultationRequestPort {
    private final CommandBus commands;
    public CommandBusEditorialSourceConsultationRequestAdapter(CommandBus commands) { this.commands = commands; }
    @Override public void request(ClaimEditorialSourceConsultationCommand command) { commands.dispatch(command); }
}
