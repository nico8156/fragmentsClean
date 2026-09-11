package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcess;
import jakarta.transaction.Transactional;

@Transactional
public class RequestAccountDeletionCommandHandler
    implements CommandHandler<RequestAccountDeletionCommand> {
  private final AppUserRepository users;
  private final AccountDeletionProcessRepository processes;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;

  public RequestAccountDeletionCommandHandler(
      AppUserRepository users,
      AccountDeletionProcessRepository processes,
      DomainEventPublisher events,
      DateTimeProvider clock) {
    this.users = users;
    this.processes = processes;
    this.events = events;
    this.clock = clock;
  }

  @Override
  public void execute(RequestAccountDeletionCommand command) {
    if (processes.findByUserId(command.userId()).isPresent()) return;
    var user =
        users
            .findById(command.userId())
            .orElseThrow(
                () -> new BusinessCommandRejectedException("APP_USER_NOT_FOUND", "User not found"));
    var now = clock.now();
    if (!user.requestAccountDeletion(command.commandId(), now)) return;
    users.save(user);
    processes.save(AccountDeletionProcess.start(command.commandId(), command.userId(), now));
    user.domainEvents().forEach(events::publish);
    user.clearDomainEvents();
  }
}
