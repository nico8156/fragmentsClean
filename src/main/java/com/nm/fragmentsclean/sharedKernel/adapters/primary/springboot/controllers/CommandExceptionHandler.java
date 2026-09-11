package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandIdentityConflictException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.LegacyCommandReceiptUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class CommandExceptionHandler {
    @ExceptionHandler(BusinessCommandRejectedException.class)
    ResponseEntity<CommandErrorResponse> businessRejection(BusinessCommandRejectedException rejection) {
        return ResponseEntity.unprocessableEntity().body(new CommandErrorResponse(
                "COMMAND_REJECTED", rejection.rejectionCode(), rejection.getMessage()));
    }

    @ExceptionHandler(CommandIdentityConflictException.class)
    ResponseEntity<CommandErrorResponse> identityConflict(CommandIdentityConflictException conflict) {
        return ResponseEntity.status(409).body(new CommandErrorResponse(
                "COMMAND_ID_CONFLICT", "COMMAND_ID_REUSED", conflict.getMessage()));
    }

    @ExceptionHandler(LegacyCommandReceiptUnavailableException.class)
    ResponseEntity<CommandErrorResponse> legacyReceipt(LegacyCommandReceiptUnavailableException unavailable) {
        return ResponseEntity.status(503).body(new CommandErrorResponse(
                "COMMAND_RECEIPT_UNAVAILABLE", "LEGACY_OWNER_UNVERIFIED", unavailable.getMessage()));
    }
}
