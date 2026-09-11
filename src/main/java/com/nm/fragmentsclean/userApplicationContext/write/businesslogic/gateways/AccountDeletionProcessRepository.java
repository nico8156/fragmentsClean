package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcess;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionProcessRepository {
  Optional<AccountDeletionProcess> findByUserId(UUID userId);

  Optional<AccountDeletionProcess> findByRequestId(UUID requestId);

  AccountDeletionProcess save(AccountDeletionProcess process);
}
