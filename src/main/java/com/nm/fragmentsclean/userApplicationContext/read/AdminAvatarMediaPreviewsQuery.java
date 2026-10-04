package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record AdminAvatarMediaPreviewsQuery(Map<UUID,UUID> mediaProfiles) implements Query<Map<UUID,String>> {
    public AdminAvatarMediaPreviewsQuery {mediaProfiles=Map.copyOf(mediaProfiles);if(mediaProfiles.size()>100)throw new IllegalArgumentException("At most 100 media per preview batch");}
}
