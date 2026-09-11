package com.nm.fragmentsclean.sharedKernelTest.integration;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandReceiptStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusReader;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandReceiptTransactionIT extends AbstractBaseE2E {
    private static final UUID COMMAND_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");
    private static final UUID USER_ID = UUID.fromString("a2222222-2222-4222-8222-222222222222");

    @Autowired DurableCommandExecutor executor;
    @Autowired CommandStatusReader statuses;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS command_receipt_test_effects (effect_id UUID PRIMARY KEY)");
        jdbc.update("DELETE FROM command_receipt_test_effects");
        jdbc.update("DELETE FROM command_status");
    }

    @Test
    void technical_failure_rolls_back_business_effect_and_keeps_pending_receipt() {
        var command = new TestAuthenticatedCommand(COMMAND_ID, USER_ID, "technical-failure");

        assertThatThrownBy(() -> executor.execute(command, () -> {
            jdbc.update("INSERT INTO command_receipt_test_effects(effect_id) VALUES (?)", UUID.randomUUID());
            throw new IllegalStateException("database unavailable");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM command_receipt_test_effects", Integer.class)).isZero();
        assertThat(statuses.findForRequester(COMMAND_ID, USER_ID).status()).isEqualTo("PENDING");
    }

    @Test
    void business_rejection_rolls_back_business_effect_but_commits_rejected_receipt() {
        var command = new TestAuthenticatedCommand(COMMAND_ID, USER_ID, "business-rejection");

        assertThatThrownBy(() -> executor.execute(command, () -> {
            jdbc.update("INSERT INTO command_receipt_test_effects(effect_id) VALUES (?)", UUID.randomUUID());
            throw new BusinessCommandRejectedException("RULE_FAILED", "Rule failed");
        })).isInstanceOf(BusinessCommandRejectedException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM command_receipt_test_effects", Integer.class)).isZero();
        var status = statuses.findForRequester(COMMAND_ID, USER_ID);
        assertThat(status.status()).isEqualTo(CommandReceiptStatus.REJECTED.name());
        assertThat(status.rejectionCode()).isEqualTo("RULE_FAILED");
    }

    @Test
    void concurrent_retry_executes_business_effect_once() throws Exception {
        var command = new TestAuthenticatedCommand(COMMAND_ID, USER_ID, "concurrent");
        var executions = new AtomicInteger();
        Callable<Void> attempt = () -> {
            executor.execute(command, () -> {
                executions.incrementAndGet();
                jdbc.update("INSERT INTO command_receipt_test_effects(effect_id) VALUES (?)", UUID.randomUUID());
            });
            return null;
        };

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(attempt);
            var second = pool.submit(attempt);
            first.get();
            second.get();
        }

        assertThat(executions).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM command_receipt_test_effects", Integer.class)).isOne();
        assertThat(statuses.findForRequester(COMMAND_ID, USER_ID).status()).isEqualTo("APPLIED");
    }

    private record TestAuthenticatedCommand(UUID commandId, UUID requesterId, String intent)
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
}
