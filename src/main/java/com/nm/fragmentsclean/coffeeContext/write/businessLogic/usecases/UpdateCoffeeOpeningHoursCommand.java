package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record UpdateCoffeeOpeningHoursCommand(UUID commandId, UUID coffeeId, List<DaySchedule> schedules,
        Instant clientAt) implements Command {
    public UpdateCoffeeOpeningHoursCommand { schedules = schedules == null ? List.of() : List.copyOf(schedules); }
    public record DaySchedule(int dayCode, List<TimeWindow> windows) {
        public DaySchedule { windows = windows == null ? List.of() : List.copyOf(windows); }
    }
    public record TimeWindow(int startMinute, int endMinute) { }
}
