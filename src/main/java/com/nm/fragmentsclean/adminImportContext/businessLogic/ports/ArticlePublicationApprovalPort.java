package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.util.UUID;

public interface ArticlePublicationApprovalPort {
    UUID approve(String token);
}
