package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DebugAuthUsersEventsKafkaListener {


    private static final Logger log = LoggerFactory.getLogger(DebugAuthUsersEventsKafkaListener.class);
    public DebugAuthUsersEventsKafkaListener() {
        log.info("[DEBUG-KAFKA] DebugAuthUsersEventsKafkaListener bean initialized");
    }

    @KafkaListener(
            topics = { "auth-users-events" },
            groupId = "debug-all-users-events",
            properties = {
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "auto.offset.reset=earliest"
            }
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info(
                "[DEBUG-KAFKA] topic={} partition={} offset={} key={} value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        );
    }
}
