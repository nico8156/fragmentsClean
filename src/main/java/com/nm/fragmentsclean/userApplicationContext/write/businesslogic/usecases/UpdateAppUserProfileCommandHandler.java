package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.InactiveAppUserException;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.InvalidDisplayNameException;
import jakarta.transaction.Transactional;

@Transactional
public class UpdateAppUserProfileCommandHandler
    implements CommandHandler<UpdateAppUserProfileCommand> {
  private final AppUserRepository users;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;

  public UpdateAppUserProfileCommandHandler(
      AppUserRepository users, DomainEventPublisher events, DateTimeProvider clock) {
    this.users = users;
    this.events = events;
    this.clock = clock;
  }

  @Override
  public void execute(UpdateAppUserProfileCommand command) {
    var user =
        users
            .findById(command.userId())
            .orElseThrow(
                () -> new BusinessCommandRejectedException("APP_USER_NOT_FOUND", "User not found"));

    try {
      if (!user.updateDisplayName(command.displayName(), clock.now())) {
        return;
      }
    } catch (InvalidDisplayNameException invalid) {
      throw new BusinessCommandRejectedException("INVALID_DISPLAY_NAME", invalid.getMessage());
    } catch (InactiveAppUserException inactive) {
      throw new BusinessCommandRejectedException("ACCOUNT_NOT_ACTIVE", inactive.getMessage());
    }

    users.save(user);
    user.domainEvents().forEach(events::publish);
    user.clearDomainEvents();
  }
}
