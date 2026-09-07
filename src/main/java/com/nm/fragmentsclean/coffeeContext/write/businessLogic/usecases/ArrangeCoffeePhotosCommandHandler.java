package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosArrangedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class ArrangeCoffeePhotosCommandHandler implements CommandHandler<ArrangeCoffeePhotosCommand> {
    private final CoffeeRepository coffees; private final DomainEventPublisher events; private final DateTimeProvider clock;
    public ArrangeCoffeePhotosCommandHandler(CoffeeRepository coffees, DomainEventPublisher events, DateTimeProvider clock) {
        this.coffees=coffees; this.events=events; this.clock=clock;
    }
    @Override @Transactional public void execute(ArrangeCoffeePhotosCommand command) {
        var coffee=coffees.findById(new CoffeeId(command.coffeeId())).orElseThrow(() -> new IllegalArgumentException("Coffee not found"));
        var now=clock.now();
        coffee.arrangePhotos(command.orderedPhotoIds().stream().map(PhotoId::new).toList(),
                command.coverPhotoId()==null?null:new PhotoId(command.coverPhotoId()), now);
        coffees.save(coffee);
        events.publish(new CoffeePhotosArrangedEvent(UUID.randomUUID(), command.commandId(), coffee.coffeeId(),
                coffee.photos().stream().map(photo -> new CoffeePhotosArrangedEvent.ArrangedPhoto(photo.id().value(), photo.uri(), photo.isCover(), photo.sortOrder())).toList(),
                coffee.version(), now, command.clientAt()));
    }
}
