package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.*;
public record ListMediaCatalogArticleUsagesQuery(String id,String cursor,int limit){
    public ListMediaCatalogArticleUsagesQuery{if(!"ARTICLE".equals(SearchMediaCatalogQuery.parseId(id)[0]) || limit<1 || limit>100)throw new IllegalArgumentException("Invalid article usage query");if(cursor!=null)parseCursor(cursor);}
    public static String[] parseCursor(String cursor){
        try{var c=cursor.split(":",-1);if(c.length!=3 || !UUID.fromString(c[0]).toString().equals(c[0]) || !Set.of("COVER","SECTION").contains(c[1]) || !UUID.fromString(c[2]).toString().equals(c[2]))throw new IllegalArgumentException();return c;}
        catch(RuntimeException e){throw new IllegalArgumentException("Invalid usage cursor");}
    }
}
