package com.nm.fragmentsclean.sharedKernelTest.unit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.BusHandlerRegistrationListener;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

import static org.assertj.core.api.Assertions.assertThat;

class BusHandlerRegistrationListenerTest {
	@Test
	void registers_all_injected_handlers_on_application_ready() {
		var commandBus = new CommandBus();
		var queryBus = new QueryBus();
		var eventBus = new EventBus();
		var commandHandler = new RecordingCommandHandler();
		var commandWithResultHandler = new RecordingCommandWithResultHandler();
		var queryHandler = new RecordingQueryHandler();
		var eventHandler = new RecordingEventHandler();

		new BusHandlerRegistrationListener(
				commandBus,
				queryBus,
				eventBus,
				List.of(commandHandler),
				List.of(commandWithResultHandler),
				List.of(queryHandler),
				List.of(eventHandler))
				.onApplicationReady(null);

		commandBus.dispatch(new TestCommand());
		var result = commandBus.<String>dispatchWithResult(new TestCommandWithResult());
		var queryResult = queryBus.dispatch(new TestQuery());
		eventBus.publish(new TestEvent(UUID.randomUUID(), Instant.parse("2026-07-05T08:00:00Z")));

		assertThat(commandHandler.calls.get()).isEqualTo(1);
		assertThat(commandWithResultHandler.calls.get()).isEqualTo(1);
		assertThat(result).isEqualTo("command-result");
		assertThat(queryHandler.calls.get()).isEqualTo(1);
		assertThat(queryResult).isEqualTo("query-result");
		assertThat(eventHandler.calls.get()).isEqualTo(1);
	}

	private record TestCommand() implements Command {
	}

	private record TestCommandWithResult() implements Command {
	}

	private record TestQuery() implements Query<String> {
	}

	private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
	}

	private static class RecordingCommandHandler implements CommandHandler<TestCommand> {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public void execute(TestCommand command) {
			calls.incrementAndGet();
		}
	}

	private static class RecordingCommandWithResultHandler implements CommandHandlerWithResult<TestCommandWithResult, String> {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public String execute(TestCommandWithResult command) {
			calls.incrementAndGet();
			return "command-result";
		}
	}

	private static class RecordingQueryHandler implements QueryHandler<TestQuery, String> {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public String handle(TestQuery query) {
			calls.incrementAndGet();
			return "query-result";
		}
	}

	private static class RecordingEventHandler implements EventHandler<TestEvent> {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public void handle(TestEvent event) {
			calls.incrementAndGet();
		}
	}
}
