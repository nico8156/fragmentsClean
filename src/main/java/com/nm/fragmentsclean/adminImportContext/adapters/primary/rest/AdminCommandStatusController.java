package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusView;

@RestController
@RequestMapping("/api/admin/commands")
public class AdminCommandStatusController {
	private final CommandStatusRepository commandStatusRepository;

	public AdminCommandStatusController(CommandStatusRepository commandStatusRepository) {
		this.commandStatusRepository = commandStatusRepository;
	}

	@GetMapping("/{commandId}")
	public ResponseEntity<CommandStatusView> getStatus(@PathVariable UUID commandId) {
		return ResponseEntity.ok(commandStatusRepository.find(commandId));
	}
}
