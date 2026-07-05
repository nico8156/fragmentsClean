package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

@Component
public class SocialStartupEventListener {
	private final CommandBus commandBus;
	private final QueryBus querryBus;
	private final EventBus eventBus;
	private final List<CommandHandler<?>> commandHandlers;
	private final List<CommandHandlerWithResult<?, ?>> commandHandlersWithResult;
	private final List<QueryHandler<?, ?>> queryHandlers;
	private final List<EventHandler<?>> eventHandlers;

	public SocialStartupEventListener(CommandBus commandBus,
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

	@EventListener
	public void onApplicationReady(ApplicationReadyEvent event) {
		commandBus.registerCommandHandlers(commandHandlers);
		commandBus.registerCommandHandlersWithResult(commandHandlersWithResult);
		querryBus.registerQueryHandlers(queryHandlers);
		eventBus.registerEventHandlers(eventHandlers);
	}
}
