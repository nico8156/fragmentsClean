package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.util.UUID;

/** Read-side port. Owner filtering is mandatory for mobile-facing reads. */
public interface CommandStatusReader {
    CommandStatusView find(UUID commandId);

    CommandStatusView findForRequester(UUID commandId, UUID requesterId);
}
