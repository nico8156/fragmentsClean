package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditPage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchAdminAuditQuery;

public interface AdminAuditReadRepository {
    AdminAuditPage search(SearchAdminAuditQuery query);
}
