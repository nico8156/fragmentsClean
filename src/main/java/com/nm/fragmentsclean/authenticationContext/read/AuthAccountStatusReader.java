package com.nm.fragmentsclean.authenticationContext.read;

import java.util.UUID;

public interface AuthAccountStatusReader {
  boolean isActive(UUID userId);
}
