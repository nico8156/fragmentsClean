package com.nm.fragmentsclean.socialContextTest.integration;

import com.nm.fragmentsclean.TestContainers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;

@DataJpaTest
@EnableAutoConfiguration
@EntityScan("com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {JpaIntegrationTestConfiguration.class})
@TestPropertySources(
        @TestPropertySource(locations = {"classpath:application.properties"})
)
public abstract class AbstractJpaIntegrationTest extends TestContainers {
}
