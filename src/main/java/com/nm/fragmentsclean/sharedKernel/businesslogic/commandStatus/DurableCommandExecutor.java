package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;

import java.util.Objects;

/**
 * Application service that makes the command receipt lifecycle independent of
 * HTTP and of opportunistic event/socket delivery.
 */
public final class DurableCommandExecutor {
    private final CommandReceiptStore receipts;
    private final CommandFingerprint fingerprint;
    private final CommandTransaction transactions;
    private final DateTimeProvider clock;

    public DurableCommandExecutor(
            CommandReceiptStore receipts,
            CommandFingerprint fingerprint,
            CommandTransaction transactions,
            DateTimeProvider clock) {
        this.receipts = receipts;
        this.fingerprint = fingerprint;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void execute(AuthenticatedCommand command, Runnable businessExecution) {
        var descriptor = new CommandDescriptor(
                command.receiptCommandId(),
                command.requesterId(),
                command.receiptType(),
                fingerprint.fingerprint(command));

        CommandReceipt initial = receipts.registerPending(descriptor, clock.now());
        if (isLegacy(initial)) {
            if (initial.status() == CommandReceiptStatus.APPLIED
                    && Objects.equals(initial.descriptor().requesterId(), descriptor.requesterId())) {
                return;
            }
            throw new LegacyCommandReceiptUnavailableException();
        }
        verifyIdentity(initial, descriptor);

        try {
            transactions.requiresNew(() -> executeOnce(descriptor, businessExecution));
        } catch (BusinessCommandRejectedException rejection) {
            transactions.requiresNew(() -> receipts.markRejected(
                    descriptor,
                    rejection.rejectionCode(),
                    rejection.getMessage(),
                    clock.now()));
            throw rejection;
        }
    }

    private void executeOnce(CommandDescriptor descriptor, Runnable businessExecution) {
        CommandReceipt receipt = receipts.lock(descriptor);
        verifyIdentity(receipt, descriptor);

        if (receipt.status() == CommandReceiptStatus.APPLIED) {
            return;
        }
        if (receipt.status() == CommandReceiptStatus.REJECTED) {
            throw new BusinessCommandRejectedException(receipt.rejectionCode(), receipt.reason());
        }

        businessExecution.run();
        receipts.markApplied(descriptor, clock.now());
    }

    private void verifyIdentity(CommandReceipt receipt, CommandDescriptor expected) {
        if (!Objects.equals(receipt.descriptor(), expected)) {
            throw new CommandIdentityConflictException();
        }
    }

    private boolean isLegacy(CommandReceipt receipt) {
        return receipt.descriptor().commandType() == null || receipt.descriptor().fingerprint() == null;
    }
}
