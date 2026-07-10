package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

import com.nm.fragmentsclean.aticleContext.read.configuration.ArticleContextReadDependenciesConfiguration;
import com.nm.fragmentsclean.coffeeContext.read.configuration.CoffeeContextDependenciesConfiguration;

class StoragePresignerWiringTest {
	@Test
	void coffee_photo_resolver_uses_the_coffee_s3_presigner() throws Exception {
		assertQualifier(
				CoffeeContextDependenciesConfiguration.class,
				"coffeePhotoUriResolver",
				"coffeePhotoS3Presigner");
	}

	@Test
	void article_image_resolver_uses_the_article_s3_presigner() throws Exception {
		assertQualifier(
				ArticleContextReadDependenciesConfiguration.class,
				"articleImageUriResolver",
				"articleImageS3Presigner");
	}

	private void assertQualifier(Class<?> configurationClass, String beanMethodName, String expectedQualifier) throws Exception {
		Method method = findMethod(configurationClass, beanMethodName);
		var annotations = method.getParameterAnnotations()[1];
		assertThat(annotations)
				.filteredOn(Qualifier.class::isInstance)
				.extracting(annotation -> ((Qualifier) annotation).value())
				.containsExactly(expectedQualifier);
	}

	private Method findMethod(Class<?> type, String methodName) {
		for (Method method : type.getDeclaredMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		throw new IllegalArgumentException("Method not found: " + type.getName() + "#" + methodName);
	}
}
