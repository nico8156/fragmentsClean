package com.nm.fragmentsclean.ticketContext.integration;

import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.ticketContext.integration.TicketOutboxJpaIntegrationTestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;

@DataJpaTest
@EnableAutoConfiguration
@EntityScan({
        "com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities",
        "com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities"
})
@EnableJpaRepositories({
        "com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa",
        "com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {TicketOutboxJpaIntegrationTestConfiguration.class})
@TestPropertySources(
        @TestPropertySource(locations = {"classpath:application.properties"})
)
public abstract class AbstractTicketOutboxJpaIntegrationTest extends TestContainers {
}
