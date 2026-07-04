package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport.SqsMessagingProperties;

import software.amazon.awssdk.services.sqs.SqsClient;

class SqsIntegrationEventConsumerWiringTest {

	@Test
	void production_constructor_is_explicitly_autowired_for_spring_component_scanning() throws Exception {
		Constructor<SqsIntegrationEventConsumer> constructor =
				SqsIntegrationEventConsumer.class.getConstructor(
						SqsClient.class,
						SqsMessagingProperties.class,
						ObjectMapper.class,
						SqsIntegrationEventRouter.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getAnnotation(Autowired.class)).isNotNull();
	}
}
