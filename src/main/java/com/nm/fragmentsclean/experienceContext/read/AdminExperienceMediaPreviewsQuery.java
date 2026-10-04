package com.nm.fragmentsclean.experienceContext.read;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record AdminExperienceMediaPreviewsQuery(List<UUID> mediaIds) implements Query<Map<UUID,String>> {
    public AdminExperienceMediaPreviewsQuery {
        mediaIds=List.copyOf(mediaIds);
        if(mediaIds.size()>100)throw new IllegalArgumentException("At most 100 media per preview batch");
    }
}
