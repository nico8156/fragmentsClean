package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

public record EventRouting( boolean sendToEventBus,
                            boolean sendToKafka,
                            boolean sendToWebSocket
) {
    public static EventRouting eventBusOnly() {
        return new EventRouting(true, false, false);
    }

    public static EventRouting kafkaOnly() {
        return new EventRouting(false, true, false);
    }

    public static EventRouting kafkaAndWebSocket() {
        return new EventRouting(false, true, true);
    }

    public static EventRouting none() {
        return new EventRouting(false, false, false);
    }
}
