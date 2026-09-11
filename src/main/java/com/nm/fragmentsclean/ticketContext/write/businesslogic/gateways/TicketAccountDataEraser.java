package com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways;

import java.util.UUID;

public interface TicketAccountDataEraser {
  void erase(UUID userId);
}
