package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

import java.util.Arrays;

public enum DayOfWeekShort {
    MONDAY(0),
    TUESDAY(1),
    WEDNESDAY(2),
    THURSDAY(3),
    FRIDAY(4),
    SATURDAY(5),
    SUNDAY(6);

    private final int code;

    DayOfWeekShort(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static DayOfWeekShort fromCode(int code) {
        return Arrays.stream(values())
                .filter(d -> d.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid day code: " + code));
    }
}
