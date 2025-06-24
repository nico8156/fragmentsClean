package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary;

import com.nm.fragmentsclean.FragmentsCleanApplication;
import com.nm.fragmentsclean.TestContainers;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = FragmentsCleanApplication.class)
@AutoConfigureMockMvc
public abstract class abstractBaseE2E extends TestContainers {
}
