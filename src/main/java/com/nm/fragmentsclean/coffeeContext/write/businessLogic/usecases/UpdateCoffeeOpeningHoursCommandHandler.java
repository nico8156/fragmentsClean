package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursUpdatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.OpeningHours;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.DayOfWeekShort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.TimeWindowMinutes;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class UpdateCoffeeOpeningHoursCommandHandler implements CommandHandler<UpdateCoffeeOpeningHoursCommand> {
    private final CoffeeRepository coffees;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;

    public UpdateCoffeeOpeningHoursCommandHandler(CoffeeRepository coffees, DomainEventPublisher events, DateTimeProvider clock) {
        this.coffees = coffees; this.events = events; this.clock = clock;
    }

    @Override @Transactional
    public void execute(UpdateCoffeeOpeningHoursCommand command) {
        var coffee = coffees.findById(new CoffeeId(command.coffeeId()))
                .orElseThrow(() -> new IllegalArgumentException("Coffee not found: " + command.coffeeId()));
        if (coffee.isArchived()) throw new IllegalStateException("Archived coffee cannot be edited");
        var byDay = new EnumMap<DayOfWeekShort, List<TimeWindowMinutes>>(DayOfWeekShort.class);
        for (var schedule : command.schedules()) {
            var day = DayOfWeekShort.fromCode(schedule.dayCode());
            if (byDay.containsKey(day)) throw new IllegalArgumentException("A day may only be supplied once: " + day);
            byDay.put(day, schedule.windows().stream()
                    .map(window -> new TimeWindowMinutes(window.startMinute(), window.endMinute())).toList());
        }
        var openingHours = new OpeningHours(byDay);
        var now = clock.now();
        coffee.setOpeningHours(openingHours, now);
        coffees.save(coffee);
        var periods = openingHours.asMap().entrySet().stream().flatMap(entry -> entry.getValue().stream()
                .map(window -> new CoffeeOpeningHoursUpdatedEvent.OpeningPeriod(entry.getKey().code(), window.start(), window.end())))
                .toList();
        events.publish(new CoffeeOpeningHoursUpdatedEvent(UUID.randomUUID(), command.commandId(), coffee.coffeeId(),
                periods, coffee.version(), now, command.clientAt()));
    }
}
