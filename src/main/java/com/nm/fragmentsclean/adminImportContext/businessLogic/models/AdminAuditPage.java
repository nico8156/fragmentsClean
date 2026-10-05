package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.List;

public record AdminAuditPage(List<AdminAuditEntry> items, String nextCursor) {}
