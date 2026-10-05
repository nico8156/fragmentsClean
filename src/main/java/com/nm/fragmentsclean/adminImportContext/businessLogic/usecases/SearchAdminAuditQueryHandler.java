package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditPage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditReadRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

@Component
public final class SearchAdminAuditQueryHandler implements QueryHandler<SearchAdminAuditQuery, AdminAuditPage> {
    private final AdminAuditReadRepository repository;
    public SearchAdminAuditQueryHandler(AdminAuditReadRepository repository) { this.repository = repository; }
    @Override
    public AdminAuditPage handle(SearchAdminAuditQuery query) { return repository.search(query); }
}
