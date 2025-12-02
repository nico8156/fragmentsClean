package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuerryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new HashMap<>();

    public void registerQuerryHandlers(List<QueryHandler<?, ?>> handlerList) {
        for (var handler : handlerList) {
            Class<?> commandType = extractGenericCommandType(handler);
            handlers.put(commandType, handler);
        }
    }

    public <Q extends Query<R>, R> R dispatch(Q querry) {
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(querry.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for command: " + querry.getClass());
        }
        return handler.handle(querry);
    }

    private Class<?> extractGenericCommandType(QueryHandler<?, ?> handler) {
        Class<?> userType = AopUtils.getTargetClass(handler); // Unwrap proxy
        for (var iFace : userType.getGenericInterfaces()) {
            if (iFace instanceof ParameterizedType type &&
                    type.getRawType() == QueryHandler.class) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot infer command type from handler: " + handler);
    }
}
