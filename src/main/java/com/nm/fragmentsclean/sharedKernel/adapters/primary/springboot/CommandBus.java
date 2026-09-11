package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandBus {

    private final DurableCommandExecutor durableCommandExecutor;

    // Handlers "classiques" sans retour
    private final Map<Class<?>, CommandHandler<?>> voidHandlers = new HashMap<>();

    // Handlers avec résultat
    private final Map<Class<?>, CommandHandlerWithResult<?, ?>> resultHandlers = new HashMap<>();

    public CommandBus() {
        this(null);
    }

    public CommandBus(DurableCommandExecutor durableCommandExecutor) {
        this.durableCommandExecutor = durableCommandExecutor;
    }

    // ------- Registration -------

    public void registerCommandHandlers(List<CommandHandler<?>> handlerList) {
        for (var handler : handlerList) {
            Class<?> commandType = extractGenericCommandType(handler);
            voidHandlers.put(commandType, handler);
        }
    }

    public void registerCommandHandlersWithResult(List<CommandHandlerWithResult<?, ?>> handlerList) {
        for (var handler : handlerList) {
            Class<?> commandType = extractGenericCommandTypeForResult(handler);
            resultHandlers.put(commandType, handler);
        }
    }

    // ------- Dispatch -------

    @SuppressWarnings("unchecked")
    public <C extends Command> void dispatch(C command) {
        CommandHandler<C> handler = (CommandHandler<C>) voidHandlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No void handler for command: " + command.getClass());
        }
        if (command instanceof AuthenticatedCommand authenticatedCommand) {
            if (durableCommandExecutor == null) {
                throw new IllegalStateException("Authenticated commands require durable command execution");
            }
            durableCommandExecutor.execute(authenticatedCommand, () -> handler.execute(command));
            return;
        }
        handler.execute(command);
    }

    @SuppressWarnings("unchecked")
    public <R> R dispatchWithResult(Command command) {
        CommandHandlerWithResult<Command, R> handler =
                (CommandHandlerWithResult<Command, R>) resultHandlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler with result for command: " + command.getClass());
        }
        return handler.execute(command);
    }

    // ------- Helpers -------

    private Class<?> extractGenericCommandType(CommandHandler<?> handler) {
        Class<?> userType = AopUtils.getTargetClass(handler);
        for (var iFace : userType.getGenericInterfaces()) {
            if (iFace instanceof ParameterizedType type &&
                    type.getRawType() == CommandHandler.class) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot infer command type from void handler: " + handler);
    }

    private Class<?> extractGenericCommandTypeForResult(CommandHandlerWithResult<?, ?> handler) {
        Class<?> userType = AopUtils.getTargetClass(handler);
        for (var iFace : userType.getGenericInterfaces()) {
            if (iFace instanceof ParameterizedType type &&
                    type.getRawType() == CommandHandlerWithResult.class) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot infer command type from result handler: " + handler);
    }
}
