package com.nm.fragmentsclean.socialContext.write.businesslogic.gateways;

import java.util.UUID;

public interface SocialAccountDataEraser {
  void erase(UUID userId);
}
