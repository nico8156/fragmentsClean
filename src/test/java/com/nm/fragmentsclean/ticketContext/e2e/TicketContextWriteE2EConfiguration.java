package com.nm.fragmentsclean.ticketContext.e2e;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake.FakeTicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TicketContextWriteE2EConfiguration {

    @Primary
    @Bean
    public DateTimeProvider deterministicClockProvider() {
        return new DeterministicDateTimeProvider();
    }

    @Primary
    @Bean
    public TicketVerificationProvider fakeTicketVerificationProvider() {
        // APPROVE by default (tu feras un autre test en REJECT)
        return new FakeTicketVerificationProvider(FakeTicketVerificationProvider.Mode.APPROVE);
    }
}
