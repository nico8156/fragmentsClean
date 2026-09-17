package com.nm.fragmentsclean.sharedKernel.businesslogic.privacy;

import java.util.UUID;

/** Purges technical payload copies after every business context has acknowledged erasure. */
public interface PersonalDataResidueStore {
  void purge(UUID userId, UUID authUserId);
}
