package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.nm.fragmentsclean.FragmentsCleanApplication;
import com.nm.fragmentsclean.TestContainers;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FragmentsCleanApplication.class,
    properties = {
      "app.outbox.dispatcher.scheduling-enabled=false",
      "auth.provider-credential-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AuthenticationContextWriteE2EConfiguration.class)
@ActiveProfiles("auth_test")
public abstract class AbstractBaseE2E extends TestContainers {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate authenticationFixtureJdbc;

    @org.junit.jupiter.api.BeforeEach
    void clearAccountFixtures() {
        // Testcontainers database only: child account rows from other verticals
        // must not leak into these tests. Never weaken production FK constraints.
        authenticationFixtureJdbc.execute("TRUNCATE TABLE app_users, auth_users CASCADE");
    }
}
