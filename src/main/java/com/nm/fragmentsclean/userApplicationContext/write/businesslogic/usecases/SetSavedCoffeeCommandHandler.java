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

		var savedCoffeeById = repository.byId(cmd.savedCoffeeId());
		if (savedCoffeeById.isPresent() && !belongsTo(savedCoffeeById.get(), cmd)) {
			throw new IllegalStateException("SavedCoffeeId mismatch with user/coffee");
		}

		SavedCoffee savedCoffee = repository.byUserIdAndCoffeeId(cmd.userId(), cmd.coffeeId())
				.or(() -> savedCoffeeById)
				.orElseGet(() -> SavedCoffee.createNew(
						cmd.savedCoffeeId(),
						cmd.userId(),
						cmd.coffeeId(),
						now));

		var snapshot = savedCoffee.toSnapshot();
		if (savedCoffee.applyState(cmd.value(), now)) {
			repository.save(savedCoffee);
			savedCoffee.registerSavedCoffeeSetEvent(commandId, cmd.clientAt(), now);

			savedCoffee.domainEvents().forEach(eventPublisher::publish);
			savedCoffee.clearDomainEvents();
		}

		commandStatusRecorder.markApplied(
				commandId,
				"SavedCoffee",
				snapshot.savedCoffeeId().toString(),
				"user.saved_coffee.set",
				now);
	}

	private boolean belongsTo(SavedCoffee savedCoffee, SetSavedCoffeeCommand cmd) {
		var snapshot = savedCoffee.toSnapshot();
		return snapshot.userId().equals(cmd.userId()) && snapshot.coffeeId().equals(cmd.coffeeId());
	}
}
