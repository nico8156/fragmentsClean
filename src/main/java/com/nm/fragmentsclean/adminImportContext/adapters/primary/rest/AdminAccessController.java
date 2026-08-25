package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GrantAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminUsers;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RevokeAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

@RestController
@RequestMapping("/api/admin/access/users")
public class AdminAccessController {
	private final ListAdminUsers listAdminUsers;
	private final GrantAdminUser grantAdminUser;
	private final RevokeAdminUser revokeAdminUser;
	private final DateTimeProvider dateTimeProvider;
	private final RecordAdminAudit recordAdminAudit;

	public AdminAccessController(ListAdminUsers listAdminUsers, GrantAdminUser grantAdminUser,
			RevokeAdminUser revokeAdminUser, DateTimeProvider dateTimeProvider, RecordAdminAudit recordAdminAudit) {
		this.listAdminUsers = listAdminUsers;
		this.grantAdminUser = grantAdminUser;
		this.revokeAdminUser = revokeAdminUser;
		this.dateTimeProvider = dateTimeProvider;
		this.recordAdminAudit = recordAdminAudit;
	}

	@GetMapping
	public List<AdminUserResponse> list() {
		return listAdminUsers.execute().stream().map(AdminUserResponse::from).toList();
	}

	@PostMapping
	public ResponseEntity<AdminUserResponse> grant(@RequestBody GrantAdminUserRequest request,
			Authentication authentication) {
		var granted = grantAdminUser.execute(request.userId(), request.email(),
				UUID.fromString(authentication.getName()), dateTimeProvider.now());
		recordAdminAudit.execute(UUID.fromString(authentication.getName()), "ADMIN_ACCESS_GRANTED", "USER", granted.userId(), null, "APPLIED", dateTimeProvider.now());
		return ResponseEntity.status(HttpStatus.CREATED).body(AdminUserResponse.from(granted));
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> revoke(@PathVariable UUID userId, Authentication authentication) {
		revokeAdminUser.execute(userId);
		recordAdminAudit.execute(UUID.fromString(authentication.getName()), "ADMIN_ACCESS_REVOKED", "USER", userId, null, "APPLIED", dateTimeProvider.now());
		return ResponseEntity.noContent().build();
	}

	public record GrantAdminUserRequest(UUID userId, String email) { }

	public record AdminUserResponse(UUID userId, String email, Instant grantedAt, UUID grantedBy, boolean bootstrap) {
		static AdminUserResponse from(AdminUserAccess access) {
			return new AdminUserResponse(access.userId(), access.email(), access.grantedAt(), access.grantedBy(), access.bootstrap());
		}
	}
}
