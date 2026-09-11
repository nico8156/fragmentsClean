package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
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

	public SetSavedCoffeeCommandHandler(
			SavedCoffeeRepository repository,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void execute(SetSavedCoffeeCommand cmd) {
		var now = dateTimeProvider.now();
		var savedCoffeeById = repository.byId(cmd.savedCoffeeId());
		if (savedCoffeeById.isPresent() && !belongsTo(savedCoffeeById.get(), cmd)) {
			throw new BusinessCommandRejectedException(
					"SAVED_COFFEE_ID_CONFLICT", "Saved coffee id belongs to another user or coffee");
		}

		SavedCoffee savedCoffee = repository.byUserIdAndCoffeeId(cmd.userId(), cmd.coffeeId())
				.or(() -> savedCoffeeById)
				.orElseGet(() -> SavedCoffee.createNew(
						cmd.savedCoffeeId(),
						cmd.userId(),
						cmd.coffeeId(),
						now));

		if (savedCoffee.applyState(cmd.value(), now)) {
			repository.save(savedCoffee);
			savedCoffee.registerSavedCoffeeSetEvent(UUID.fromString(cmd.commandId()), cmd.clientAt(), now);

			savedCoffee.domainEvents().forEach(eventPublisher::publish);
			savedCoffee.clearDomainEvents();
		}
	}

	private boolean belongsTo(SavedCoffee savedCoffee, SetSavedCoffeeCommand cmd) {
		var snapshot = savedCoffee.toSnapshot();
		return snapshot.userId().equals(cmd.userId()) && snapshot.coffeeId().equals(cmd.coffeeId());
	}
}
