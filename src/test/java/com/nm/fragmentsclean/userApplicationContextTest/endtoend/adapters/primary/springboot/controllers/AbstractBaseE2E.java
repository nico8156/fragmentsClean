package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.FragmentsCleanApplication;
import com.nm.fragmentsclean.TestContainers;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@ContextConfiguration(classes = UserApplicationContextE2EConfiguration.class)
public abstract class AbstractBaseE2E extends TestContainers {}
