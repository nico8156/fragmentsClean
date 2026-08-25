package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AdminAuditRecorder;
import com.nm.fragmentsclean.ticketContext.read.TicketStatusReadRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.*;

@RestController
@RequestMapping("/api/admin/tickets")
public class AdminTicketController {
    private final TicketStatusReadRepository reads;
    private final AdminUpdateTicketCommandHandler update;
    private final AdminDeleteTicketCommandHandler delete;
    private final VerifyTicketCommandHandler verify;
    private final AdminAuditRecorder audit;

    public AdminTicketController(TicketStatusReadRepository reads, AdminUpdateTicketCommandHandler update,
            AdminDeleteTicketCommandHandler delete, VerifyTicketCommandHandler verify, AdminAuditRecorder audit) {
        this.reads = reads; this.update = update; this.delete = delete; this.verify = verify; this.audit = audit;
    }

    @GetMapping public List<TicketStatusView> list() { return reads.list(); }
    @GetMapping("/{ticketId}") public ResponseEntity<TicketStatusView> get(@PathVariable UUID ticketId) {
        var value = reads.findById(ticketId); return value == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(value);
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<Void> update(@PathVariable UUID ticketId, @RequestBody AdminTicketUpdateRequest body,
            Authentication authentication) {
        var actor = actor(authentication); var commandId = UUID.randomUUID(); var now = Instant.now();
        update.execute(new AdminUpdateTicketCommand(commandId, ticketId, actor, body.toDomain(), now));
        audit.record(actor, "TICKET_UPDATED", "TICKET", ticketId, commandId, "APPLIED", now);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId, Authentication authentication) {
        var actor = actor(authentication); var commandId = UUID.randomUUID(); var now = Instant.now();
        delete.execute(new AdminDeleteTicketCommand(commandId, ticketId, actor, now));
        audit.record(actor, "TICKET_DELETED", "TICKET", ticketId, commandId, "APPLIED", now);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{ticketId}/verify")
    public ResponseEntity<Void> verify(@PathVariable UUID ticketId, @RequestBody AdminTicketVerifyRequest body,
            Authentication authentication) {
        var actor = actor(authentication); var commandId = UUID.randomUUID(); var now = Instant.now();
        verify.execute(new VerifyTicketCommand(commandId, ticketId, body.userId(), body.imageRef(), body.ocrText(), now));
        audit.record(actor, "TICKET_VERIFICATION_RETRIGGERED", "TICKET", ticketId, commandId, "ACCEPTED", now);
        return ResponseEntity.accepted().build();
    }

    private UUID actor(Authentication authentication) { return UUID.fromString(authentication.getName()); }

    public record AdminTicketVerifyRequest(UUID userId, String imageRef, String ocrText) { }
    public record AdminTicketUpdateRequest(String ocrText, String imageRef, Integer amountCents, String currency,
            Instant ticketDate, String merchantName, String merchantAddress, String paymentMethod,
            List<Ticket.TicketLineItem> lineItems, String rejectionReason, Ticket.TicketStatus status) {
        Ticket.AdminUpdate toDomain() { return new Ticket.AdminUpdate(ocrText, imageRef, amountCents, currency, ticketDate,
                merchantName, merchantAddress, paymentMethod, lineItems, rejectionReason, status); }
    }
}
