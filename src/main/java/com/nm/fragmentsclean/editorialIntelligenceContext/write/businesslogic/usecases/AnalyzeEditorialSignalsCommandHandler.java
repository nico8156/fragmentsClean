package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import org.springframework.stereotype.Component;

/** Keeps both scheduled and operator-triggered analysis behind the command bus. */
@Component
public final class AnalyzeEditorialSignalsCommandHandler implements CommandHandler<AnalyzeEditorialSignalsCommand> {
    private final AnalyzeNewEditorialSignals analysis;

    public AnalyzeEditorialSignalsCommandHandler(AnalyzeNewEditorialSignals analysis) {
        this.analysis = analysis;
    }

    @Override
    public void execute(AnalyzeEditorialSignalsCommand command) {
        analysis.execute(command.limit());
    }
}
