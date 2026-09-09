package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

/** Primitive ACL for an operator-requested analysis of pending editorial signals. */
public interface EditorialSignalAnalysisAdministrationPort {
    void analyzePendingSignals(int limit);
}
