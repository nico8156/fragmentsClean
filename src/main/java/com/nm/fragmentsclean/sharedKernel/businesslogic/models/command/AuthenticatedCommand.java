package com.nm.fragmentsclean.sharedKernel.businesslogic.models.command;

import java.util.UUID;

/**
 * Command issued by an authenticated mobile user and reconciled through
 * {@code GET /commands/{commandId}}.
 */
public interface AuthenticatedCommand extends Command {
    UUID receiptCommandId();

    UUID requesterId();

    /** Stable receipt contract name; changing it requires an explicit version bump. */
    String receiptType();
}
