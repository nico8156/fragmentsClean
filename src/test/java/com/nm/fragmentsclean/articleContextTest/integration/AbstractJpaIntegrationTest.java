package com.nm.fragmentsclean.articleContextTest.integration;

import com.nm.fragmentsclean.TestContainers;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;

@DataJpaTest
@EnableAutoConfiguration
@EntityScan("com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { JpaIntegrationTestConfiguration.class })
@TestPropertySources(@TestPropertySource(locations = { "classpath:application.properties" }))
public abstract class AbstractJpaIntegrationTest extends TestContainers {
}
