package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.time.Duration;
import java.util.UUID;

/** Primitive ACL: Studio has no dependency on editorial domain types. */
public interface EditorialSourceAdministrationPort {
    void register(UUID sourceId, String name, String accessMode, String authorityLevel, String endpoint, Duration pollingFrequency);
    void revise(UUID sourceId, String name, String accessMode, String authorityLevel, String endpoint, Duration pollingFrequency, boolean enabled);
}
