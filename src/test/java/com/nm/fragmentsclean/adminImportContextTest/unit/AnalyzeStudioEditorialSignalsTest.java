package com.nm.fragmentsclean.adminImportContextTest.unit;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSignalAnalysisAdministrationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.AnalyzeStudioEditorialSignals;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyzeStudioEditorialSignalsTest {
    @Test
    void asks_editorial_intelligence_to_analyze_one_bounded_batch() {
        var port = new RecordingAnalysisPort();

        new AnalyzeStudioEditorialSignals(port).execute();

        assertThat(port.limit).isEqualTo(50);
    }

    private static final class RecordingAnalysisPort implements EditorialSignalAnalysisAdministrationPort {
        private int limit;
        @Override public void analyzePendingSignals(int limit) { this.limit = limit; }
    }
}
