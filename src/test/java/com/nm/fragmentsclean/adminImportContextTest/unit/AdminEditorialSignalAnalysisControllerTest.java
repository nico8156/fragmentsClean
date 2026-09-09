package com.nm.fragmentsclean.adminImportContextTest.unit;

import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminEditorialSourcesController;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSignalAnalysisAdministrationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceAdministrationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.AnalyzeStudioEditorialSignals;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ManageEditorialSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminEditorialSignalAnalysisControllerTest {
    @Test
    void exposes_a_deliberate_analysis_trigger_through_the_admin_acl() {
        var analysis = new RecordingAnalysisPort();
        var controller = new AdminEditorialSourcesController(
                new ManageEditorialSource(new NoopSources(), UUID::randomUUID),
                new EmptyCatalog(),
                new AnalyzeStudioEditorialSignals(analysis));

        var response = controller.analyzePendingSignals();

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(analysis.limit).isEqualTo(50);
    }

    private static final class RecordingAnalysisPort implements EditorialSignalAnalysisAdministrationPort {
        private int limit;
        @Override public void analyzePendingSignals(int limit) { this.limit = limit; }
    }

    private static final class NoopSources implements EditorialSourceAdministrationPort {
        @Override public void register(UUID id, String name, String mode, String level, String endpoint, Duration frequency) { }
        @Override public void revise(UUID id, String name, String mode, String level, String endpoint, Duration frequency, boolean enabled) { }
    }

    private static final class EmptyCatalog implements EditorialSourceStudioCatalog {
        @Override public List<Source> listSources() { return List.of(); }
        @Override public List<Signal> listSignals(UUID sourceId, int limit) { return List.of(); }
    }
}
