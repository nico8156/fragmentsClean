package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import java.time.Instant;

public class DeterministicDateTimeProvider implements DateTimeProvider {

	public Instant instantOfNow;

	@Override
	public Instant now() {
		return Instant.parse("2023-10-01T11:00:00Z");
	}
}
