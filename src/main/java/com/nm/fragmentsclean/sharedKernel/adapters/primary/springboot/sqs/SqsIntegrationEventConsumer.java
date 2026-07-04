package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport.SqsMessagingProperties;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Component
@ConditionalOnProperty(name = "app.messaging.sqs.enabled", havingValue = "true")
public class SqsIntegrationEventConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegrationEventConsumer.class);
    private static final AtomicInteger WORKER_SEQUENCE = new AtomicInteger();

    private final SqsClient sqsClient;
    private final SqsMessagingProperties properties;
    private final ObjectMapper objectMapper;
    private final SqsIntegrationEventRouting router;
    private final ExecutorServiceFactory executorServiceFactory;
    private final Object lifecycleMonitor = new Object();

    private volatile ExecutorService executorService;
    private volatile boolean running;

    public SqsIntegrationEventConsumer(SqsClient sqsClient,
                                       SqsMessagingProperties properties,
                                       ObjectMapper objectMapper,
                                       SqsIntegrationEventRouter router) {
        this(
                sqsClient,
                properties,
                objectMapper,
                router,
                workerCount -> Executors.newFixedThreadPool(workerCount, runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "fragments-sqs-consumer-" + WORKER_SEQUENCE.incrementAndGet()
                    );
                    thread.setDaemon(true);
                    return thread;
                })
        );
    }

    SqsIntegrationEventConsumer(SqsClient sqsClient,
                                SqsMessagingProperties properties,
                                ObjectMapper objectMapper,
                                SqsIntegrationEventRouting router,
                                ExecutorServiceFactory executorServiceFactory) {
        this.sqsClient = Objects.requireNonNull(sqsClient, "sqsClient is required");
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.router = Objects.requireNonNull(router, "router is required");
        this.executorServiceFactory = Objects.requireNonNull(executorServiceFactory, "executorServiceFactory is required");
    }

    @Override
    public void start() {
        synchronized (lifecycleMonitor) {
            if (running) {
                return;
            }
            List<String> queueUrls = uniqueQueueUrls();
            running = true;
            if (queueUrls.isEmpty()) {
                log.info("[sqs] consumer started queueCount=0");
                return;
            }
            executorService = executorServiceFactory.create(queueUrls.size());
            queueUrls.forEach(queueUrl -> executorService.submit(() -> runWorkerLoop(queueUrl)));
            log.info("[sqs] consumer started queueCount={} waitTime={} maxMessages={}",
                    queueUrls.size(), properties.getWaitTime(), properties.getMaxMessages());
        }
    }

    @Override
    public void stop() {
        synchronized (lifecycleMonitor) {
            if (!running) {
                return;
            }
            running = false;
            ExecutorService currentExecutor = executorService;
            executorService = null;
            if (currentExecutor != null) {
                currentExecutor.shutdownNow();
                awaitTermination(currentExecutor);
            }
            log.info("[sqs] consumer stopped");
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    public void pollDestination(String destination, String queueUrl) {
        if (queueUrl == null || queueUrl.isBlank()) {
            log.debug("[sqs] skipped blank queueUrl destination={}", destination);
            return;
        }
        pollQueue(queueUrl);
    }

    void runWorkerLoop(String queueUrl) {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                pollQueue(queueUrl);
            } catch (RuntimeException exception) {
                log.error("[sqs] worker iteration failed queueUrl={} error={}",
                        queueUrl, exception.getMessage(), exception);
            }
        }
    }

    List<String> uniqueQueueUrls() {
        return new ArrayList<>(new LinkedHashSet<>(properties.getQueues().values().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList()));
    }

    private void pollQueue(String queueUrl) {
        ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(properties.getMaxMessages())
                .waitTimeSeconds((int) properties.getWaitTime().toSeconds())
                .visibilityTimeout((int) properties.getVisibilityTimeout().toSeconds())
                .build());

        if (response.messages().isEmpty()) {
            log.debug("[sqs] receive empty queueUrl={} waitTime={} maxMessages={}",
                    queueUrl, properties.getWaitTime(), properties.getMaxMessages());
            return;
        }

        log.info("[sqs] receive messages queueUrl={} count={}", queueUrl, response.messages().size());
        for (Message message : response.messages()) {
            handleMessage(queueUrl, message);
        }
    }

    private void handleMessage(String queueUrl, Message message) {
        try {
            IntegrationEventEnvelope envelope = objectMapper.readValue(message.body(), IntegrationEventEnvelope.class);
            router.route(envelope);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
            log.info("[sqs] processed and deleted messageId={} queueUrl={}", message.messageId(), queueUrl);
        } catch (Exception e) {
            log.error("[sqs] failed to process messageId={}", message.messageId(), e);
        }
    }

    private void awaitTermination(ExecutorService currentExecutor) {
        try {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (!currentExecutor.awaitTermination(properties.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("[sqs] consumer stop timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    interface ExecutorServiceFactory {
        ExecutorService create(int workerCount);
    }
}
