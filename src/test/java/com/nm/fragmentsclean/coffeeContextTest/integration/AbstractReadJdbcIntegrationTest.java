package com.nm.fragmentsclean.coffeeContextTest.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.nm.fragmentsclean.TestContainers;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties", properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
})
public abstract class AbstractReadJdbcIntegrationTest extends TestContainers {
}
