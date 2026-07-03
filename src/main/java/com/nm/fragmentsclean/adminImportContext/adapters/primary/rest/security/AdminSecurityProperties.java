package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin.security")
public class AdminSecurityProperties {
	private String token = "";

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean hasToken() {
		return token != null && !token.isBlank();
	}
}
