package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin.security")
public class AdminSecurityProperties {
	private java.util.UUID exclusiveOwnerId;

	public void setExclusiveOwnerId(String ownerId) {
		exclusiveOwnerId = ownerId == null || ownerId.isBlank() ? null : java.util.UUID.fromString(ownerId.trim());
	}
	public boolean isExclusiveOwnerMode() { return exclusiveOwnerId != null; }
	public boolean isExclusiveOwner(String userId) {
		try { return exclusiveOwnerId != null && exclusiveOwnerId.equals(java.util.UUID.fromString(userId)); }
		catch (IllegalArgumentException exception) { return false; }
	}

	private String bootstrapUserIds = "";
	private String bootstrapEmails = "";

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

	public Set<String> bootstrapUserIds() { return csv(bootstrapUserIds); }
	public Set<String> bootstrapEmails() { return csv(bootstrapEmails); }
	public boolean hasBootstrapAdmin() { return !bootstrapUserIds().isEmpty() || !bootstrapEmails().isEmpty(); }

	private Set<String> csv(String value) {
		return value == null ? Set.of() : Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(item -> !item.isBlank())
				.collect(Collectors.toSet());
	}
}
