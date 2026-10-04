package com.nm.fragmentsclean.experienceContext.read;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.AdminExperienceMediaPreviewRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public final class AdminExperienceMediaPreviewsQueryHandler implements QueryHandler<AdminExperienceMediaPreviewsQuery,Map<UUID,String>> {
    private final AdminExperienceMediaPreviewRepository repository;
    public AdminExperienceMediaPreviewsQueryHandler(AdminExperienceMediaPreviewRepository repository){this.repository=repository;}
    public Map<UUID,String> handle(AdminExperienceMediaPreviewsQuery query){return repository.availablePreviews(query.mediaIds());}
}
