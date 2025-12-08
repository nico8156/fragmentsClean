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
public class QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new HashMap<>();

    /**
     * À appeler quelque part au startup avec la liste des QueryHandler
     * (exactement comme tu le fais pour CommandBus).
     */
    public void registerQueryHandlers(List<QueryHandler<?, ?>> handlerList) {
        for (var handler : handlerList) {
            Class<?> queryType = extractGenericQueryType(handler);
            handlers.put(queryType, handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <R, Q extends Query<R>> R dispatch(Q query) {
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for query: " + query.getClass());
        }
        return handler.handle(query);
    }

    private Class<?> extractGenericQueryType(QueryHandler<?, ?> handler) {
        Class<?> userType = AopUtils.getTargetClass(handler); // unwrap proxy
        for (var iFace : userType.getGenericInterfaces()) {
            if (iFace instanceof ParameterizedType type &&
                    type.getRawType() == QueryHandler.class) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot infer query type from handler: " + handler);
    }
}
