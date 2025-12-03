package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

public record TimeWindowMinutes(int start, int end) {
    public TimeWindowMinutes {
        if (start < 0 || end > 24 * 60 || start >= end) {
            throw new IllegalArgumentException("invalid time window: " + start + "–" + end);
        }
    }
}
