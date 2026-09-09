package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSignalAnalysisAdministrationPort;

/** Studio application use case; it does not depend on editorial domain types. */
public final class AnalyzeStudioEditorialSignals {
    private static final int MAXIMUM_BATCH_SIZE = 50;
    private final EditorialSignalAnalysisAdministrationPort analysis;

    public AnalyzeStudioEditorialSignals(EditorialSignalAnalysisAdministrationPort analysis) {
        this.analysis = analysis;
    }

    public void execute() {
        analysis.analyzePendingSignals(MAXIMUM_BATCH_SIZE);
    }
}
