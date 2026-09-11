package com.nm.fragmentsclean.sharedKernelTest.unit;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurableCommandExecutorTest {
    private static final Instant NOW = Instant.parse("2026-09-11T12:00:00Z");
    private static final UUID COMMAND_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private final FakeReceiptStore receipts = new FakeReceiptStore();
    private final DurableCommandExecutor executor = new DurableCommandExecutor(
            receipts,
            command -> "fingerprint:" + ((TestCommand) command).value(),
            Runnable::run,
            () -> NOW);

    @Test
    void successful_command_is_applied_even_when_it_emits_no_event() {
        executor.execute(new TestCommand(COMMAND_ID, USER_ID, "same-state"), () -> { });

        assertThat(receipts.get(COMMAND_ID).status()).isEqualTo(CommandReceiptStatus.APPLIED);
    }

    @Test
    void retry_of_an_applied_command_does_not_execute_business_logic_twice() {
        var executions = new AtomicInteger();
        var command = new TestCommand(COMMAND_ID, USER_ID, "like");

        executor.execute(command, executions::incrementAndGet);
        executor.execute(command, executions::incrementAndGet);

        assertThat(executions).hasValue(1);
    }

    @Test
    void retry_after_executor_restart_uses_the_durable_receipt_without_reexecution() {
        var executions = new AtomicInteger();
        var command = new TestCommand(COMMAND_ID, USER_ID, "after-restart");
        executor.execute(command, executions::incrementAndGet);

        var restartedExecutor = new DurableCommandExecutor(
                receipts,
                candidate -> "fingerprint:" + ((TestCommand) candidate).value(),
                Runnable::run,
                () -> NOW.plusSeconds(30));
        restartedExecutor.execute(command, executions::incrementAndGet);

        assertThat(executions).hasValue(1);
        assertThat(receipts.get(COMMAND_ID).status()).isEqualTo(CommandReceiptStatus.APPLIED);
    }

    @Test
    void explicit_business_rejection_is_durable_and_replayed_without_execution() {
        var command = new TestCommand(COMMAND_ID, USER_ID, "forbidden");

        assertThatThrownBy(() -> executor.execute(command, () -> {
            throw new BusinessCommandRejectedException("COMMENT_NOT_OWNED", "Only the author can edit it");
        })).isInstanceOf(BusinessCommandRejectedException.class);

        assertThat(receipts.get(COMMAND_ID).status()).isEqualTo(CommandReceiptStatus.REJECTED);
        assertThat(receipts.get(COMMAND_ID).rejectionCode()).isEqualTo("COMMENT_NOT_OWNED");

        assertThatThrownBy(() -> executor.execute(command, () -> {
            throw new AssertionError("must not run");
        })).isInstanceOf(BusinessCommandRejectedException.class)
                .hasMessage("Only the author can edit it");
    }

    @Test
    void technical_failure_leaves_command_pending_for_retry() {
        var failure = new IllegalStateException("database unavailable");

        assertThatThrownBy(() -> executor.execute(
                new TestCommand(COMMAND_ID, USER_ID, "retry"),
                () -> { throw failure; }))
                .isSameAs(failure);

        assertThat(receipts.get(COMMAND_ID).status()).isEqualTo(CommandReceiptStatus.PENDING);
    }

    @Test
    void command_id_cannot_be_reused_by_another_requester_or_intent() {
        executor.execute(new TestCommand(COMMAND_ID, USER_ID, "first"), () -> { });

        assertThatThrownBy(() -> executor.execute(
                new TestCommand(COMMAND_ID, UUID.randomUUID(), "first"), () -> { }))
                .isInstanceOf(CommandIdentityConflictException.class);
        assertThatThrownBy(() -> executor.execute(
                new TestCommand(COMMAND_ID, USER_ID, "different"), () -> { }))
                .isInstanceOf(CommandIdentityConflictException.class);
    }

    @Test
    void proven_legacy_applied_receipt_is_accepted_without_reexecution() {
        var command = new TestCommand(COMMAND_ID, USER_ID, "legacy");
        receipts.values.put(COMMAND_ID, new CommandReceipt(
                new CommandDescriptor(COMMAND_ID, USER_ID, null, null),
                CommandReceiptStatus.APPLIED, NOW, null, null, null));

        executor.execute(command, () -> { throw new AssertionError("must not execute"); });
    }

    @Test
    void ownerless_legacy_receipt_is_not_claimed_or_treated_as_business_rejection() {
        var command = new TestCommand(COMMAND_ID, USER_ID, "legacy");
        receipts.values.put(COMMAND_ID, new CommandReceipt(
                new CommandDescriptor(COMMAND_ID, null, null, null),
                CommandReceiptStatus.APPLIED, NOW, null, null, null));

        assertThatThrownBy(() -> executor.execute(command, () -> { }))
                .isInstanceOf(LegacyCommandReceiptUnavailableException.class);
        assertThat(receipts.get(COMMAND_ID).descriptor().requesterId()).isNull();
    }

    private record TestCommand(UUID commandId, UUID requesterId, String value)
            implements AuthenticatedCommand {
        @Override
        public UUID receiptCommandId() {
            return commandId;
        }

        @Override
        public String receiptType() {
            return "test.command.v1";
        }
    }

    private static final class FakeReceiptStore implements CommandReceiptStore {
        private final Map<UUID, CommandReceipt> values = new HashMap<>();

        @Override
        public CommandReceipt registerPending(CommandDescriptor descriptor, Instant now) {
            values.putIfAbsent(descriptor.commandId(), pending(descriptor));
            return values.get(descriptor.commandId());
        }

        @Override
        public CommandReceipt lock(CommandDescriptor descriptor) {
            return values.get(descriptor.commandId());
        }

        @Override
        public void markApplied(CommandDescriptor descriptor, Instant appliedAt) {
            values.put(descriptor.commandId(), new CommandReceipt(
                    descriptor, CommandReceiptStatus.APPLIED, appliedAt, null, null, null));
        }

        @Override
        public void markRejected(CommandDescriptor descriptor, String rejectionCode, String reason, Instant rejectedAt) {
            values.put(descriptor.commandId(), new CommandReceipt(
                    descriptor, CommandReceiptStatus.REJECTED, null, rejectedAt, rejectionCode, reason));
        }

        CommandReceipt get(UUID commandId) {
            return values.get(commandId);
        }

        private CommandReceipt pending(CommandDescriptor descriptor) {
            return new CommandReceipt(descriptor, CommandReceiptStatus.PENDING, null, null, null, null);
        }
    }
}
