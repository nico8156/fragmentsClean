package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.util.*;

@Component
public class EventBus {

    private final Map<Class<?>, List<EventHandler<?>>> handlersByType = new HashMap<>();

    /**
     * Appelée depuis la config (SharedKernelDependenciesConfiguration)
     * avec tous les EventHandler<?> du contexte Spring.
     */
    public void registerEventHandlers(List<EventHandler<?>> handlerList) {
        for (var handler : handlerList) {
            Class<?> eventType = extractGenericEventType(handler);
            handlersByType
                    .computeIfAbsent(eventType, k -> new ArrayList<>())
                    .add(handler);
        }
    }

    /**
     * Publie un événement : tous les handlers enregistrés
     * pour ce type seront invoqués.
     */
    @SuppressWarnings("unchecked")
    public <E extends DomainEvent> void publish(E event) {
        if (event == null) return;

        var handlers = handlersByType.getOrDefault(event.getClass(), List.of());
        for (EventHandler<?> raw : handlers) {
            ((EventHandler<E>) raw).handle(event);
        }
    }

    // ─── Reflection pour retrouver le type générique ────────────────────────

    private Class<?> extractGenericEventType(EventHandler<?> handler) {
        Class<?> userType = AopUtils.getTargetClass(handler); // unwrap proxy Spring

        for (var iFace : userType.getGenericInterfaces()) {
            if (iFace instanceof ParameterizedType type &&
                    type.getRawType() == EventHandler.class) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot infer event type from handler: " + handler);
    }
}
