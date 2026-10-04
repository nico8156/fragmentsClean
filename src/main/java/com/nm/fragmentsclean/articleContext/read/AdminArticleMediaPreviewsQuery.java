package com.nm.fragmentsclean.articleContext.read;
import java.util.*;
public record AdminArticleMediaPreviewsQuery(Map<UUID,String> references){
    public AdminArticleMediaPreviewsQuery {if(references==null || references.size()>100)throw new IllegalArgumentException("Invalid article preview batch");references=Map.copyOf(references);}
}
