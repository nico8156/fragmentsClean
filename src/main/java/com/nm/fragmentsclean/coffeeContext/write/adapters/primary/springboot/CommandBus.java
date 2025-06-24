package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandBus {

    private final Map<Class<?>, CommandHandler<?>> handlers = new HashMap<>();

    public void registerCommandHandlers(List<CommandHandler<?>> handlerList) {
        for (var handler : handlerList) {
            Class<?> commandType = extractGenericCommandType(handler);
            handlers.put(commandType, handler);
        }
    }

    public <C extends Command> void dispatch(C command) {
        CommandHandler<C> handler = (CommandHandler<C>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for command: " + command.getClass());
        }
        handler.execute(command);
    }

    private Class<?> extractGenericCommandType(CommandHandler<?> handler) {
        Class<?> userType = AopUtils.getTargetClass(handler); // Unwrap proxy
        for (var iFace : userType.getGenericInterfaces()) {
            if (iFace instanceof ParameterizedType type &&
                    type.getRawType() == CommandHandler.class) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot infer command type from handler: " + handler);
    }
}


