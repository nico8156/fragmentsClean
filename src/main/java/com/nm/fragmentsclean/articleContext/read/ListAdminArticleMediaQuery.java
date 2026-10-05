package com.nm.fragmentsclean.articleContext.read;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record ListAdminArticleMediaQuery(UUID articleId,String cursor,int limit) implements Query<java.util.Optional<ArticleMediaUsagePage>> {
    public ListAdminArticleMediaQuery {
        if(articleId==null || limit<1 || limit>100)throw new IllegalArgumentException("Invalid article media query");
        if(cursor!=null)parseCursor(cursor);
    }
    public static String[] parseCursor(String cursor){
        try {
            var parts=cursor.split(":",-1);
            if(parts.length!=5 || !UUID.fromString(parts[0]).toString().equals(parts[0]) || !UUID.fromString(parts[4]).toString().equals(parts[4]))throw new IllegalArgumentException();
            int role=Integer.parseInt(parts[1]),section=Integer.parseInt(parts[2]),position=Integer.parseInt(parts[3]);
            if((role!=0 && role!=1) || section< -1 || position<0 || !Integer.toString(role).equals(parts[1]) || !Integer.toString(section).equals(parts[2]) || !Integer.toString(position).equals(parts[3]))throw new IllegalArgumentException();
            return parts;
        }catch(RuntimeException e){throw new IllegalArgumentException("Invalid article media cursor");}
    }
}
