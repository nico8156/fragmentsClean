package com.nm.fragmentsclean.coffeeContextTest.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.nm.fragmentsclean.TestContainers;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties", properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration,org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration"
})
public abstract class AbstractReadJdbcIntegrationTest extends TestContainers {
}
