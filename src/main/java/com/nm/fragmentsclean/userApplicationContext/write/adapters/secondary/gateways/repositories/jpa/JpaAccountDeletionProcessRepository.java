package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AccountDeletionProcessJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcess;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class JpaAccountDeletionProcessRepository implements AccountDeletionProcessRepository {
  private final SpringAccountDeletionProcessRepository repository;

  public JpaAccountDeletionProcessRepository(SpringAccountDeletionProcessRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<AccountDeletionProcess> findByUserId(UUID userId) {
    return repository.findByUserId(userId).map(this::domain);
  }

  @Override
  public Optional<AccountDeletionProcess> findByRequestId(UUID requestId) {
    return repository.findByRequestIdForUpdate(requestId).map(this::domain);
  }

  @Override
  public AccountDeletionProcess save(AccountDeletionProcess process) {
    return domain(repository.save(entity(process)));
  }

  private AccountDeletionProcess domain(AccountDeletionProcessJpaEntity entity) {
    var acks =
        entity.getAcknowledgements().isBlank()
            ? EnumSet.noneOf(AccountDeletionProcess.Context.class)
            : Arrays.stream(entity.getAcknowledgements().split(","))
                .map(AccountDeletionProcess.Context::valueOf)
                .collect(
                    Collectors.toCollection(
                        () -> EnumSet.noneOf(AccountDeletionProcess.Context.class)));
    return AccountDeletionProcess.rehydrate(
        entity.getRequestId(),
        entity.getUserId(),
        entity.getRequestedAt(),
        acks,
        entity.getStatus(),
        entity.getCompletedAt(),
        entity.getVersion());
  }

  private AccountDeletionProcessJpaEntity entity(AccountDeletionProcess process) {
    String acks =
        process.acknowledgements().stream()
            .sorted()
            .map(Enum::name)
            .collect(Collectors.joining(","));
    return new AccountDeletionProcessJpaEntity(
        process.requestId(),
        process.userId(),
        process.requestedAt(),
        process.status(),
        acks,
        process.completedAt(),
        process.version());
  }
}
