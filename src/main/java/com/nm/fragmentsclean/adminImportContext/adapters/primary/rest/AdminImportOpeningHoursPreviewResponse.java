package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;

public record AdminImportOpeningHoursPreviewResponse(
		List<String> weekdayDescriptions,
		List<AdminImportOpeningHoursPeriodPreviewResponse> periods
) {
	public AdminImportOpeningHoursPreviewResponse {
		weekdayDescriptions = weekdayDescriptions == null ? List.of() : List.copyOf(weekdayDescriptions);
		periods = periods == null ? List.of() : List.copyOf(periods);
	}
}
