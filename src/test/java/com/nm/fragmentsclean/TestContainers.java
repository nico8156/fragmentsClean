package com.nm.fragmentsclean;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("database")
public abstract class TestContainers {
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13.1")
            .withUsername("fragments")
            .withPassword("fragments")
            .withUrlParam("stringtype", "unspecified")
            .withInitScript("schema.sql");

    static {
        postgreSQLContainer.start();
    }

    @DynamicPropertySource
    public static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }
}
