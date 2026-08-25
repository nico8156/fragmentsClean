package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin.security")
public class AdminSecurityProperties {
	private String token = "";
	private String bootstrapUserIds = "";
	private String bootstrapEmails = "";

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean hasToken() {
		return token != null && !token.isBlank();
	}

	public void setBootstrapUserIds(String bootstrapUserIds) {
		this.bootstrapUserIds = bootstrapUserIds;
	}

	public void setBootstrapEmails(String bootstrapEmails) {
		this.bootstrapEmails = bootstrapEmails;
	}

	public boolean isBootstrapUser(String userId, String email) {
		return csv(bootstrapUserIds).contains(userId)
				|| (email != null && csv(bootstrapEmails).stream().anyMatch(email::equalsIgnoreCase));
	}

	private Set<String> csv(String value) {
		return value == null ? Set.of() : Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(item -> !item.isBlank())
				.collect(Collectors.toSet());
	}
}
