package com.nm.fragmentsclean.adminImportContextTest.unit;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.CommandBusEditorialSignalAnalysisAdministrationAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.AnalyzeEditorialSignalsCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBusEditorialSignalAnalysisAdministrationAdapterTest {
    @Test
    void maps_the_admin_intention_to_the_editorial_command() {
        var commandBus = new CommandBus();
        var handler = new RecordingHandler();
        commandBus.registerCommandHandlers(List.of(handler));

        new CommandBusEditorialSignalAnalysisAdministrationAdapter(commandBus).analyzePendingSignals(50);

        assertThat(handler.limit).isEqualTo(50);
    }

    private static final class RecordingHandler implements CommandHandler<AnalyzeEditorialSignalsCommand> {
        private int limit;
        @Override public void execute(AnalyzeEditorialSignalsCommand command) { limit = command.limit(); }
    }
}
