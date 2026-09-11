package com.nm.fragmentsclean.experienceContextTest.endtoend;

import com.nm.fragmentsclean.FragmentsCleanApplication;
import com.nm.fragmentsclean.TestContainers;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FragmentsCleanApplication.class,
    properties = {
      "spring.task.scheduling.enabled=false",
      "app.outbox.dispatcher.scheduling-enabled=false"
    })
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ExperienceContextE2EConfiguration.class)
public abstract class AbstractExperienceE2E extends TestContainers {}
