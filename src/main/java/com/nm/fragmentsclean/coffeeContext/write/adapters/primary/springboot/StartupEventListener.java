package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot;

import com.nm.fragmentsclean.coffeeContext.read.GetCoffeesQueryHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QuerryBus;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupEventListener {
    private final CommandBus commandBus;
    private final QuerryBus querryBus;
    private final CreateCoffeeCommandHandler createCoffeeCommandHandler;
    private final GetCoffeesQueryHandler getCoffeesQueryHandler;

    public StartupEventListener(CommandBus commandBus, CreateCoffeeCommandHandler createCoffeeCommandHandler, QuerryBus querryBus, GetCoffeesQueryHandler getCoffeesQueryHandler) {
        this.commandBus = commandBus;
        this.createCoffeeCommandHandler = createCoffeeCommandHandler;
        this.querryBus = querryBus;
        this.getCoffeesQueryHandler = getCoffeesQueryHandler;
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("Application is ready");
        List<CommandHandler<?>> handlerList = List.of(
                createCoffeeCommandHandler
        );
        commandBus.registerCommandHandlers(handlerList);
        List<QueryHandler<?, ?>> queryHandlerList = List.of(
                getCoffeesQueryHandler
        );
        querryBus.registerQuerryHandlers(queryHandlerList);
    }
}
