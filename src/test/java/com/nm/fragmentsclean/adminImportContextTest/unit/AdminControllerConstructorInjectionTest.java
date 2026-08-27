package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminCommandStatusController;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminImportPlacesController;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminStudioArticlesController;
import com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin.AdminCoffeesReadController;

class AdminControllerConstructorInjectionTest {

	@Test
	void controllers_with_test_convenience_constructors_have_one_explicit_spring_constructor() {
		assertAutowiredConstructor(AdminCoffeesReadController.class);
		assertAutowiredConstructor(AdminCommandStatusController.class);
		assertAutowiredConstructor(AdminImportPlacesController.class);
		assertAutowiredConstructor(AdminStudioArticlesController.class);
	}

	private static void assertAutowiredConstructor(Class<?> controllerType) {
		assertThat(Arrays.stream(controllerType.getConstructors())
				.filter(constructor -> constructor.isAnnotationPresent(Autowired.class)))
				.as("%s must expose one constructor selected by Spring", controllerType.getSimpleName())
				.hasSize(1);
	}
}
