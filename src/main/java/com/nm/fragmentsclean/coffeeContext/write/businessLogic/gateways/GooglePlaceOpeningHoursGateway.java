package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

import java.util.List;

public interface GooglePlaceOpeningHoursGateway {
	List<String> findWeekdayDescriptions(GooglePlaceId googlePlaceId);
}
