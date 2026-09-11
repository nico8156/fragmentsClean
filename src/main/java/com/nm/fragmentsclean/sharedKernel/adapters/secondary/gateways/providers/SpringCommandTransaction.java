package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class SpringCommandTransaction implements CommandTransaction {
    private final PlatformTransactionManager transactionManager;

    public SpringCommandTransaction(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void requiresNew(Runnable work) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(ignored -> work.run());
    }
}
