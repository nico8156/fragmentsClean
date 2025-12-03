package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;


public record GeoPoint(double lat, double lon) {
    public GeoPoint {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("lat out of range: " + lat);
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("lon out of range: " + lon);
        }
    }
}
