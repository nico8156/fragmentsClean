package com.nm.fragmentsclean.experienceContextTest.endtoend;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;import org.springframework.boot.test.context.TestConfiguration;import org.springframework.context.annotation.*;
@TestConfiguration public class ExperienceContextE2EConfiguration{@Primary @Bean DateTimeProvider deterministicExperienceClock(){return new DeterministicDateTimeProvider();}}
