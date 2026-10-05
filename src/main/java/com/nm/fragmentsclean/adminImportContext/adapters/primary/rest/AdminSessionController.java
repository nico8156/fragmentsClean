package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;

/** Authorization is performed by the existing admin security chain before this probe. */
@RestController
public class AdminSessionController {
 private final AdminSecurityProperties properties;
 public AdminSessionController(AdminSecurityProperties properties) { this.properties = properties; }
 @GetMapping("/api/admin/access/me")
 public AdminSessionResponse current(Authentication authentication) {
  return new AdminSessionResponse(UUID.fromString(authentication.getName()), properties.isExclusiveOwnerMode());
 }
 public record AdminSessionResponse(UUID userId, boolean exclusiveOwner) {}
}
