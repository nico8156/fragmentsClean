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
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class SqsIntegrationEventConsumerTest {
	@Test
	void unique_queue_urls_are_deduplicated_and_blank_values_are_ignored() {
		var consumer = consumerWith(properties(MapBuilder.queues()
				.queue("articles-events", "https://sqs.example/articles")
				.queue("articles-read-events", "https://sqs.example/articles")
				.queue("blank", " ")
				.queue("coffees-events", "https://sqs.example/coffees")
				.build()));

		assertThat(consumer.uniqueQueueUrls())
				.containsExactly("https://sqs.example/articles", "https://sqs.example/coffees");
	}

	@Test
	void start_creates_one_worker_per_unique_queue_url() {
		var executor = new RecordingExecutorService();
		var consumer = consumerWith(
				properties(MapBuilder.queues()
						.queue("articles-events", "https://sqs.example/articles")
						.queue("articles-read-events", "https://sqs.example/articles")
						.queue("coffees-events", "https://sqs.example/coffees")
						.build()),
				executor);

		consumer.start();
		consumer.stop();

		assertThat(executor.workerCount).isEqualTo(2);
		assertThat(executor.tasks).hasSize(2);
		assertThat(consumer.isRunning()).isFalse();
	}

	@Test
	void successful_message_is_routed_and_deleted() throws Exception {
		var sqsClient = new FakeSqsClient();
		var router = new RecordingRouter();
		var message = Message.builder()
				.messageId("message-1")
				.receiptHandle("receipt-1")
				.body(json(envelope("event-1")))
				.build();
		sqsClient.messages.add(message);
		var consumer = consumerWith(sqsClient, router, properties(MapBuilder.queues()
				.queue("coffees-events", "https://sqs.example/coffees")
				.build()));

		consumer.pollDestination("coffees-events", "https://sqs.example/coffees");

		assertThat(router.routed).containsExactly(envelope("event-1"));
		assertThat(sqsClient.deleted).singleElement().satisfies(request -> {
			assertThat(request.queueUrl()).isEqualTo("https://sqs.example/coffees");
			assertThat(request.receiptHandle()).isEqualTo("receipt-1");
		});
	}

	@Test
	void successful_message_records_end_to_end_projection_delivery_latency() throws Exception {
		var meters = new SimpleMeterRegistry();
		var sqsClient = new FakeSqsClient();
		var router = new RecordingRouter();
		sqsClient.messages.add(Message.builder()
				.messageId("message-1")
				.receiptHandle("receipt-1")
				.body(json(envelope("event-1")))
				.build());
		var consumer = new SqsIntegrationEventConsumer(
				sqsClient,
				properties(MapBuilder.queues().queue("coffees-events", "https://sqs.example/coffees").build()),
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				router,
				workerCount -> new RecordingExecutorService(),
				meters);

		consumer.pollDestination("coffees-events", "https://sqs.example/coffees");

		assertThat(meters.get("fragments.projection.delivery.latency")
				.tag("destination", "coffees-events").timer().count()).isEqualTo(1);
	}

	@Test
	void failed_message_processing_does_not_delete_message() throws Exception {
		var sqsClient = new FakeSqsClient();
		var router = new RecordingRouter();
		router.failure = new IllegalStateException("handler failed");
		var message = Message.builder()
				.messageId("message-1")
				.receiptHandle("receipt-1")
				.body(json(envelope("event-1")))
				.build();
		sqsClient.messages.add(message);
		var consumer = consumerWith(sqsClient, router, properties(MapBuilder.queues()
				.queue("coffees-events", "https://sqs.example/coffees")
				.build()));

		consumer.pollDestination("coffees-events", "https://sqs.example/coffees");

		assertThat(sqsClient.deleted).isEmpty();
	}

	private static SqsIntegrationEventConsumer consumerWith(SqsMessagingProperties properties) {
		return consumerWith(properties, new RecordingExecutorService());
	}

	private static SqsIntegrationEventConsumer consumerWith(
			SqsMessagingProperties properties,
			RecordingExecutorService executorService) {
		return new SqsIntegrationEventConsumer(
				new FakeSqsClient(),
				properties,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				new RecordingRouter(),
				workerCount -> {
					executorService.workerCount = workerCount;
					return executorService;
				});
	}

	private static SqsIntegrationEventConsumer consumerWith(
			SqsClient sqsClient,
			SqsIntegrationEventRouting router,
			SqsMessagingProperties properties) {
		return new SqsIntegrationEventConsumer(
				sqsClient,
				properties,
				JsonMapper.builder().addModule(new JavaTimeModule()).build(),
				router,
				workerCount -> new RecordingExecutorService());
	}

	private static SqsMessagingProperties properties(LinkedHashMap<String, String> queues) {
		var properties = new SqsMessagingProperties();
		properties.setQueues(queues);
		properties.setMaxMessages(5);
		properties.setWaitTime(Duration.ZERO);
		properties.setVisibilityTimeout(Duration.ofSeconds(1));
		properties.setShutdownTimeout(Duration.ofMillis(10));
		return properties;
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

	private static class RecordingExecutorService extends AbstractExecutorService {
		private final List<Runnable> tasks = new ArrayList<>();
		private int workerCount;
		private boolean shutdown;

		@Override
		public void shutdown() {
			shutdown = true;
		}

		@Override
		public List<Runnable> shutdownNow() {
			shutdown = true;
			return List.of();
		}

		@Override
		public boolean isShutdown() {
			return shutdown;
		}

		@Override
		public boolean isTerminated() {
			return shutdown;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) {
			return true;
		}

		@Override
		public void execute(Runnable command) {
			tasks.add(command);
		}
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

	private static class FakeSqsClient implements SqsClient {
		private final Queue<Message> messages = new ConcurrentLinkedQueue<>();
		private final List<DeleteMessageRequest> deleted = new ArrayList<>();

		@Override
		public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
			var batch = new ArrayList<Message>();
			for (int i = 0; i < receiveMessageRequest.maxNumberOfMessages(); i++) {
				var message = messages.poll();
				if (message == null) {
					break;
				}
				batch.add(message);
			}
			return ReceiveMessageResponse.builder().messages(batch).build();
		}

		@Override
		public DeleteMessageResponse deleteMessage(DeleteMessageRequest deleteMessageRequest) {
			deleted.add(deleteMessageRequest);
			return DeleteMessageResponse.builder().build();
		}

		@Override
		public String serviceName() {
			return "sqs";
		}

		@Override
		public void close() {
		}
	}

	private static class MapBuilder {
		private final LinkedHashMap<String, String> queues = new LinkedHashMap<>();

		static MapBuilder queues() {
			return new MapBuilder();
		}

		MapBuilder queue(String destination, String queueUrl) {
			queues.put(destination, queueUrl);
			return this;
		}

		LinkedHashMap<String, String> build() {
			return queues;
		}
	}
}
