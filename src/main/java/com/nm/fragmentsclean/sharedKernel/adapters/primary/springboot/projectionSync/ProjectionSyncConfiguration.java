package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(ProjectionSyncProperties.class)
public class ProjectionSyncConfiguration {
	@Bean
	public TaskScheduler projectionSyncTaskScheduler() {
		var scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("projection-sync-");
		scheduler.setDaemon(true);
		return scheduler;
	}
}
