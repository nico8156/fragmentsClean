package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureJournal;

/** Records recoverable erasure intent before the backend accepts the durable deletion command. */
public final class RequestAccountDeletion {
  private final AccountErasureJournal journal;
  private final DateTimeProvider clock;
  private final DurableCommandExecutor durable;
  private final RequestAccountDeletionCommandHandler handler;

  public RequestAccountDeletion(
      AccountErasureJournal journal,
      DateTimeProvider clock,
      DurableCommandExecutor durable,
      RequestAccountDeletionCommandHandler handler) {
    this.journal = journal;
    this.clock = clock;
    this.durable = durable;
    this.handler = handler;
  }

  public void execute(RequestAccountDeletionCommand command) {
    // AppUser ids intentionally equal their authentication subject in the current identity contract.
    journal.record(
        new AccountErasureJournal.Entry(
            command.commandId(), command.userId(), command.userId(), clock.now()));
    durable.execute(command, () -> handler.execute(command));
  }
}
