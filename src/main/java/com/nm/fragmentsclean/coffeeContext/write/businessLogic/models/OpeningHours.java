package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.DayOfWeekShort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.TimeWindowMinutes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class OpeningHours {

    private final Map<DayOfWeekShort, List<TimeWindowMinutes>> windowsByDay;

    public OpeningHours(Map<DayOfWeekShort, List<TimeWindowMinutes>> windowsByDay) {
        // defensive copy + invariants simples
        Map<DayOfWeekShort, List<TimeWindowMinutes>> tmp = new EnumMap<>(DayOfWeekShort.class);
        if (windowsByDay != null) {
            for (var entry : windowsByDay.entrySet()) {
                var day = entry.getKey();
                var windows = entry.getValue();
                if (windows == null || windows.isEmpty()) {
                    tmp.put(day, List.of());
                    continue;
                }
                // tu peux plus tard vérifier l’absence de chevauchement
                tmp.put(day, List.copyOf(windows));
            }
        }
        this.windowsByDay = Collections.unmodifiableMap(tmp);
    }

    public List<TimeWindowMinutes> windowsFor(DayOfWeekShort day) {
        return windowsByDay.getOrDefault(day, List.of());
    }

    public boolean isClosed(DayOfWeekShort day) {
        return windowsFor(day).isEmpty();
    }

    public Map<DayOfWeekShort, List<TimeWindowMinutes>> asMap() {
        return windowsByDay;
    }

    public static OpeningHours empty() {
        return new OpeningHours(Map.of());
    }
}
