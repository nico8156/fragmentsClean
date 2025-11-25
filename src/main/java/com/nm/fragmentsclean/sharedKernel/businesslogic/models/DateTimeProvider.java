package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import java.time.Instant;

public interface DateTimeProvider {
    Instant now();
}