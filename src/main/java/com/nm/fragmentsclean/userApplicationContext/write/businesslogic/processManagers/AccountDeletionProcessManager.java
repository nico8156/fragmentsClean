package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.platform.eventing.contracts.AccountDataErasedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.UserAccountDataEraser;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.PersonalDataResidueStore;

public class AccountDeletionProcessManager {
  private final AccountDeletionProcessRepository processes;
  private final AppUserRepository users;
  private final UserAccountDataEraser eraser;
  private final DateTimeProvider clock;
  private final AccountErasureBarrier barrier;
  private final PersonalDataResidueStore technicalResidues;

  public AccountDeletionProcessManager(
      AccountDeletionProcessRepository processes,
      AppUserRepository users,
      UserAccountDataEraser eraser,
      DateTimeProvider clock,
      AccountErasureBarrier barrier,
      PersonalDataResidueStore technicalResidues) {
    this.processes = processes;
    this.users = users;
    this.eraser = eraser;
    this.clock = clock;
    this.barrier = barrier;
    this.technicalResidues = technicalResidues;
  }

  public void eraseLocalData(AppUserDeletionRequestedIntegrationEvent request) {
    var now = clock.now();
    barrier.erase(AccountErasureBarrier.Scope.USER_APPLICATION, request.userId(), request.requestId(), now, () -> {
      eraser.erase(request.userId());
      acknowledge(
          request.requestId(), request.userId(), AccountDeletionProcess.Context.USER_APPLICATION);
    });
  }

  @org.springframework.transaction.annotation.Transactional
  public void acknowledge(AccountDataErasedIntegrationEvent event) {
    acknowledge(
        event.requestId(), event.userId(), AccountDeletionProcess.Context.valueOf(event.context()));
  }

  private void acknowledge(
      java.util.UUID requestId, java.util.UUID userId, AccountDeletionProcess.Context context) {
    var process =
        processes
            .findByRequestId(requestId)
            .orElseThrow(() -> new IllegalStateException("Account deletion process not found"));
    if (!process.userId().equals(userId))
      throw new IllegalStateException("Account deletion owner mismatch");
    if (!process.acknowledge(context, clock.now())) return;
    processes.save(process);
    if (process.status() == AccountDeletionProcess.Status.COMPLETED) {
      var user =
          users.findById(userId).orElseThrow(() -> new IllegalStateException("App user not found"));
      if (user.completeAccountDeletion(clock.now())) users.save(user);
      technicalResidues.purge(userId, user.authUserId());
    }
  }
}
