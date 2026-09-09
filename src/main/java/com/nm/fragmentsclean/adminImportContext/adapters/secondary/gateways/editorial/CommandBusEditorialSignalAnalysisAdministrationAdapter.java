package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSignalAnalysisAdministrationPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.AnalyzeEditorialSignalsCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

/** ACL adapter: Studio invokes an intention; editorial intelligence owns its execution. */
public final class CommandBusEditorialSignalAnalysisAdministrationAdapter implements EditorialSignalAnalysisAdministrationPort {
    private final CommandBus commands;

    public CommandBusEditorialSignalAnalysisAdministrationAdapter(CommandBus commands) {
        this.commands = commands;
    }

    @Override
    public void analyzePendingSignals(int limit) {
        commands.dispatch(new AnalyzeEditorialSignalsCommand(limit));
    }
}
