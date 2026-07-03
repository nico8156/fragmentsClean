package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

public record AdminImportOpeningHoursPeriodPreviewResponse(
		Integer dayOfWeek,
		String openTime,
		String closeTime,
		boolean closed,
		String label
) {
}
