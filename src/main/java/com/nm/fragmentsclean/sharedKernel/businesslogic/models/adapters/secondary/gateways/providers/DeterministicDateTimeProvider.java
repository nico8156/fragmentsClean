package com.nm.fragmentsclean.sharedKernel.businesslogic.models.adapters.secondary.gateways.providers;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import java.time.Instant;

public class DeterministicDateTimeProvider implements DateTimeProvider {
    public Instant instantOfNow;

    @Override
    public Instant now() {
        return instantOfNow;
    }
}
