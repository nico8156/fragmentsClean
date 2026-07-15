package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.SavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffee;
import jakarta.transaction.Transactional;

import java.util.UUID;

@Transactional
public class SetSavedCoffeeCommandHandler implements CommandHandler<SetSavedCoffeeCommand> {
	private final SavedCoffeeRepository repository;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;
	private final CommandStatusRecorder commandStatusRecorder;

	public SetSavedCoffeeCommandHandler(
			SavedCoffeeRepository repository,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider,
			CommandStatusRecorder commandStatusRecorder) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
		this.commandStatusRecorder = commandStatusRecorder;
	}

	@Override
	public void execute(SetSavedCoffeeCommand cmd) {
		var now = dateTimeProvider.now();
		UUID commandId = UUID.fromString(cmd.commandId());

		if (commandStatusRecorder.isApplied(commandId)) {
			return;
		}

		SavedCoffee savedCoffee = repository.byId(cmd.savedCoffeeId())
				.orElseGet(() -> SavedCoffee.createNew(
						cmd.savedCoffeeId(),
						cmd.userId(),
						cmd.coffeeId(),
						now));

		var snapshot = savedCoffee.toSnapshot();
		if (!snapshot.userId().equals(cmd.userId()) || !snapshot.coffeeId().equals(cmd.coffeeId())) {
			throw new IllegalStateException("SavedCoffeeId mismatch with user/coffee");
		}

		savedCoffee.applyState(cmd.value(), now);
		repository.save(savedCoffee);
		savedCoffee.registerSavedCoffeeSetEvent(commandId, cmd.clientAt(), now);

		savedCoffee.domainEvents().forEach(eventPublisher::publish);
		savedCoffee.clearDomainEvents();

		commandStatusRecorder.markApplied(
				commandId,
				"SavedCoffee",
				cmd.savedCoffeeId().toString(),
				"user.saved_coffee.set",
				now);
	}
}
