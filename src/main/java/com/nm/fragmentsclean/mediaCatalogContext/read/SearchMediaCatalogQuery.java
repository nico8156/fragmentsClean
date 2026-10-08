package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record SearchMediaCatalogQuery(String q, String origin, String status, UUID ownerId, String cursor, int limit, String usage)
    implements Query<MediaCatalogPage> {
    public SearchMediaCatalogQuery(String q,String origin,String status,UUID ownerId,String cursor,int limit){this(q,origin,status,ownerId,cursor,limit,null);}
    public SearchMediaCatalogQuery {
        q=q==null?"":q.strip();
        if(q.length()>200 || limit<1 || limit>100) throw new IllegalArgumentException("Invalid search");
        if(origin!=null && !Set.of("EXPERIENCE","COFFEE","AVATAR","ARTICLE").contains(origin)) throw new IllegalArgumentException("Invalid origin");
        if(status!=null && !Set.of("PENDING","REVIEW_REQUIRED","REJECTED","AVAILABLE","DELETION_PENDING","DELETED").contains(status)) throw new IllegalArgumentException("Invalid status");
        if(usage!=null && !Set.of("USED","UNUSED","UNKNOWN").contains(usage))throw new IllegalArgumentException("Invalid usage filter");
        if(cursor!=null) parseId(cursor);
    }
    public static String[] parseId(String id) {
        var parts=id.split(":",-1);
        if(parts.length!=2 || !Set.of("EXPERIENCE","COFFEE","AVATAR","ARTICLE").contains(parts[0])) throw new IllegalArgumentException("Invalid media id");
        if(!UUID.fromString(parts[1]).toString().equals(parts[1])) throw new IllegalArgumentException("Invalid media id");
        return parts;
    }
}
