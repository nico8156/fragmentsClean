package com.nm.fragmentsclean.experienceContext.read;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
public record SearchAdminExperiencesQuery(String q,UUID authorId,String moderation,String publication,ExperienceCursor cursor,int limit) implements Query<ExperiencePage>{
 public SearchAdminExperiencesQuery {q=q==null?"":q.strip();if(q.length()>200||limit<1||limit>100)throw new IllegalArgumentException("Invalid search or page size");
 if(moderation!=null&&!Set.of("VISIBLE","HIDDEN").contains(moderation))throw new IllegalArgumentException("Invalid moderation status");
 if(publication!=null&&!Set.of("DRAFT","PUBLISHED","DELETED").contains(publication))throw new IllegalArgumentException("Invalid publication status");}
}
