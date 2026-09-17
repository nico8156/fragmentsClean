package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

public interface InboxMessageStore {
    InboxClaim claim(IntegrationEventEnvelope envelope);

    boolean markProcessed(IntegrationEventEnvelope envelope, String ownerToken);

    boolean markFailed(IntegrationEventEnvelope envelope, String ownerToken, Exception error);
}
