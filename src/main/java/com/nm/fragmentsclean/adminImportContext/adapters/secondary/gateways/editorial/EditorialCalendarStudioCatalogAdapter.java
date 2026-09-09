package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialCalendarStudioCatalog;
import com.nm.fragmentsclean.editorialIntelligenceContext.read.EditorialCalendarCatalog;

import java.time.YearMonth;
import java.util.List;

public final class EditorialCalendarStudioCatalogAdapter implements EditorialCalendarStudioCatalog {
    private final EditorialCalendarCatalog catalog;
    public EditorialCalendarStudioCatalogAdapter(EditorialCalendarCatalog catalog) { this.catalog = catalog; }
    @Override public List<Item> month(YearMonth month) {
        return catalog.month(month).stream().map(item -> new Item(item.scheduleId(), item.articleId(), item.revisionId(),
                item.operation(), item.dueAt(), item.status(), item.rejectionReason())).toList();
    }
}
