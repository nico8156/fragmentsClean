package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

import java.util.List;

public interface GooglePlaceOpeningHoursGateway {
	ImportedOpeningHours findOpeningHours(GooglePlaceId googlePlaceId);

	record ImportedOpeningHours(List<OpeningPeriod> periods, List<String> weekdayDescriptions) {
		public ImportedOpeningHours {
			periods = periods == null ? List.of() : List.copyOf(periods);
			weekdayDescriptions = weekdayDescriptions == null ? List.of() : List.copyOf(weekdayDescriptions);
		}
	}
	record OpeningPeriod(int dayCode, int startMinute, int endMinute) { }
}
