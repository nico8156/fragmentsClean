package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport.SqsMessagingProperties;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@EnabledIf("dockerAvailable")
class SqsIntegrationEventConsumerLocalStackTest {

	private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.4.0");

	@Test
	void successful_processing_deletes_the_sqs_message() throws Exception {
		try (var localStack = localStack();
			 var sqsClient = sqsClient(localStack)) {
			var queueUrl = createQueue(sqsClient, "success-queue");
			var router = new RecordingRouter();
			var consumer = consumer(sqsClient, router, queueUrl);

			sqsClient.sendMessage(SendMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageBody(json(envelope("event-success")))
					.build());

			consumer.pollDestination("coffees-events", queueUrl);

			assertThat(receiveCount(sqsClient, queueUrl)).isZero();
			assertThat(router.routed).containsExactly(envelope("event-success"));
		}
	}

	@Test
	void failed_processing_keeps_the_sqs_message_for_redelivery() throws Exception {
		try (var localStack = localStack();
			 var sqsClient = sqsClient(localStack)) {
			var queueUrl = createQueue(sqsClient, "failure-queue");
			var router = new RecordingRouter();
			router.failure = new IllegalStateException("projection failed");
			var consumer = consumer(sqsClient, router, queueUrl);

			sqsClient.sendMessage(SendMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageBody(json(envelope("event-failure")))
					.build());

			consumer.pollDestination("coffees-events", queueUrl);
			Thread.sleep(1200);

			assertThat(receiveCount(sqsClient, queueUrl)).isEqualTo(1);
		}
	}

	static boolean dockerAvailable() {
		try {
			return DockerClientFactory.instance().isDockerAvailable();
		} catch (RuntimeException exception) {
			return false;
		}
	}

	private static LocalStackContainer localStack() {
		var localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
				.withServices(LocalStackContainer.Service.SQS);
		localStack.start();
		return localStack;
	}

	private static SqsClient sqsClient(LocalStackContainer localStack) {
		return SqsClient.builder()
				.endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.SQS))
				.region(Region.of(localStack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						localStack.getAccessKey(),
						localStack.getSecretKey())))
				.build();
	}

	private static String createQueue(SqsClient sqsClient, String queueName) {
		var response = sqsClient.createQueue(CreateQueueRequest.builder()
				.queueName(queueName)
				.attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT, "1"))
				.build());
		return response.queueUrl();
	}

	private static int receiveCount(SqsClient sqsClient, String queueUrl) {
		return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl(queueUrl)
				.maxNumberOfMessages(10)
				.waitTimeSeconds(1)
				.visibilityTimeout(1)
				.build()).messages().size();
	}

	private static SqsIntegrationEventConsumer consumer(
			SqsClient sqsClient,
			SqsIntegrationEventRouting router,
			String queueUrl) {
		var properties = new SqsMessagingProperties();
		properties.setQueues(new LinkedHashMap<>(Map.of("coffees-events", queueUrl)));
		properties.setMaxMessages(5);
		properties.setWaitTime(Duration.ZERO);
		properties.setVisibilityTimeout(Duration.ofSeconds(1));
		properties.setShutdownTimeout(Duration.ofMillis(10));
		return new SqsIntegrationEventConsumer(
				sqsClient,
				properties,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				router,
				workerCount -> {
					throw new UnsupportedOperationException("LocalStack tests poll explicitly");
				});
	}

	private static class RecordingRouter implements SqsIntegrationEventRouting {
		private final List<IntegrationEventEnvelope> routed = new ArrayList<>();
		private RuntimeException failure;

		@Override
		public void route(IntegrationEventEnvelope envelope) {
			routed.add(envelope);
			if (failure != null) {
				throw failure;
			}
		}
	}

	private static IntegrationEventEnvelope envelope(String eventId) {
		return new IntegrationEventEnvelope(
				eventId,
				"coffee.created",
				1,
				"com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent",
				"Coffee",
				"coffee-1",
				"coffee:coffee-1",
				"coffees-events",
				"{}",
				Instant.parse("2026-07-04T08:00:00Z"));
	}

	private static String json(IntegrationEventEnvelope envelope) throws Exception {
		return JsonMapper.builder()
				.addModule(new JavaTimeModule())
				.build()
				.writeValueAsString(envelope);
	}
}
