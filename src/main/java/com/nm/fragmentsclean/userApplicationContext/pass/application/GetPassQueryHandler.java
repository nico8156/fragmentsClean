package com.nm.fragmentsclean.userApplicationContext.pass.application;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassProgressPolicy;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassCounters;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class GetPassQueryHandler implements QueryHandler<GetPassQuery, PassSnapshot> {
    private final PassContributionStore store;

    public GetPassQueryHandler(PassContributionStore store) { this.store = store; }

    @Override
    public PassSnapshot handle(GetPassQuery query) {
        return store.find(query.userId()).orElseGet(() -> PassProgressPolicy.evaluate(
                query.userId(), new PassCounters(0, 0, 0), Set.of(), 0, Instant.EPOCH));
    }
}
