package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.UUID;

/** Primitive ACL. Implementations translate an editorial intent into an articleContext command. */
public interface ScheduledArticleOperationPort {
    void publish(UUID commandId, Instant requestedAt, UUID articleId, UUID revisionId);
    void archive(UUID commandId, Instant requestedAt, UUID articleId);
}
