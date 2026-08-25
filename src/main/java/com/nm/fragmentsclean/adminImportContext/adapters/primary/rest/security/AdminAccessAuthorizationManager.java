package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

public class AdminAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
	private final AdminAccessPolicy policy;

	public AdminAccessAuthorizationManager(AdminAccessPolicy policy) {
		this.policy = policy;
	}

	@Override
	public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
		return new AuthorizationDecision(policy.isAllowed(authentication.get()));
	}
}
