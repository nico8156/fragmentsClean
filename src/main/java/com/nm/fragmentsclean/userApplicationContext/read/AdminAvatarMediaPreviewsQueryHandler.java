package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.*;
import org.springframework.stereotype.Service;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Service
public final class AdminAvatarMediaPreviewsQueryHandler implements QueryHandler<AdminAvatarMediaPreviewsQuery,Map<UUID,String>> {
    private final AdminAvatarMediaPreviewRepository repository;
    public AdminAvatarMediaPreviewsQueryHandler(AdminAvatarMediaPreviewRepository repository){this.repository=repository;}
    public Map<UUID,String> handle(AdminAvatarMediaPreviewsQuery query){return repository.currentPreviews(query.mediaProfiles());}
}
