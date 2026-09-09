package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration;

import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

@Component
public class BusHandlerRegistrationListener implements SmartInitializingSingleton {
	private final CommandBus commandBus;
	private final QueryBus querryBus;
	private final EventBus eventBus;
	private final List<CommandHandler<?>> commandHandlers;
	private final List<CommandHandlerWithResult<?, ?>> commandHandlersWithResult;
	private final List<QueryHandler<?, ?>> queryHandlers;
	private final List<EventHandler<?>> eventHandlers;

	public BusHandlerRegistrationListener(CommandBus commandBus,
			QueryBus querryBus,
			EventBus eventBus,
			List<CommandHandler<?>> commandHandlers,
			List<CommandHandlerWithResult<?, ?>> commandHandlersWithResult,
			List<QueryHandler<?, ?>> queryHandlers,
			List<EventHandler<?>> eventHandlers) {
		this.commandBus = commandBus;
		this.querryBus = querryBus;
		this.eventBus = eventBus;
		this.commandHandlers = commandHandlers;
		this.commandHandlersWithResult = commandHandlersWithResult;
		this.queryHandlers = queryHandlers;
		this.eventHandlers = eventHandlers;
	}

	/**
	 * Registers handlers before the application context is refreshed.
	 *
	 * Scheduled jobs may run as soon as Spring activates scheduling; waiting for
	 * {@code ApplicationReadyEvent} left a small startup window where a job could
	 * dispatch a valid command before its handler was known by the bus.
	 */
	@Override
	public void afterSingletonsInstantiated() {
		commandBus.registerCommandHandlers(commandHandlers);
		commandBus.registerCommandHandlersWithResult(commandHandlersWithResult);
		querryBus.registerQueryHandlers(queryHandlers);
		eventBus.registerEventHandlers(eventHandlers);
	}
}
