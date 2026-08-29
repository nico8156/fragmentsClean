package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeArchivedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePublishedEventHandler;

class CoffeeProjectionTransactionBoundaryTest {

	@Test
	void every_summary_lifecycle_handler_owns_an_explicit_transaction_boundary() {
		Stream.of(
				CoffeeCreatedEventHandler.class,
				CoffeePublishedEventHandler.class,
				CoffeeArchivedEventHandler.class,
				CoffeeDeletedEventHandler.class)
				.map(this::handleMethod)
				.forEach(method -> assertThat(method.isAnnotationPresent(Transactional.class))
						.as("%s.handle must be transactional", method.getDeclaringClass().getSimpleName())
						.isTrue());
	}

	private Method handleMethod(Class<?> handlerType) {
		return Stream.of(handlerType.getDeclaredMethods())
				.filter(method -> method.getName().equals("handle"))
				.findFirst()
				.orElseThrow();
	}
}
