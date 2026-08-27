package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.UUID;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusView;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

@RestController
@RequestMapping("/api/admin/commands")
public class AdminCommandStatusController {
	private final CommandStatusRepository commandStatusRepository;
	private final RecordAdminAudit recordAdminAudit;
	private final DateTimeProvider dateTimeProvider;

	@Autowired
	public AdminCommandStatusController(CommandStatusRepository commandStatusRepository, RecordAdminAudit recordAdminAudit,
			DateTimeProvider dateTimeProvider) {
		this.commandStatusRepository = commandStatusRepository;
		this.recordAdminAudit = recordAdminAudit; this.dateTimeProvider = dateTimeProvider;
	}

	public AdminCommandStatusController(CommandStatusRepository commandStatusRepository) {
		this(commandStatusRepository, new RecordAdminAudit(entry -> {}), Instant::now);
	}

	@GetMapping("/{commandId}")
	public ResponseEntity<CommandStatusView> getStatus(@PathVariable UUID commandId, Authentication authentication) {
		var status = commandStatusRepository.find(commandId);
		if (authentication != null) recordAdminAudit.execute(UUID.fromString(authentication.getName()),
				"ADMIN_COMMAND_STATUS_READ", "COMMAND", commandId, commandId,
				status == null ? "PENDING" : status.status(), dateTimeProvider.now());
		return ResponseEntity.ok(status);
	}
}
