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
		var request = context.getRequest();
		String path = request.getServletPath();
		// MockMvc can leave servletPath empty; URI excludes the context prefix.
		if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
		boolean accessMutation = (path.equals("/api/admin/access/users") || path.startsWith("/api/admin/access/users/"))
				&& !(request.getMethod().equals("GET") || request.getMethod().equals("HEAD") || request.getMethod().equals("OPTIONS"));
		return new AuthorizationDecision(!(policy.isExclusiveOwnerMode() && accessMutation)
				&& policy.isAllowed(authentication.get()));
	}
}
