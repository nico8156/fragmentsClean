package com.nm.fragmentsclean.articleContext.read;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
/** Exact trimmed reference identity, never a grant to read or delete a file. */
public final class ArticleMediaReferenceIdentity {
    private ArticleMediaReferenceIdentity(){}
    public static UUID of(String reference){
        if(reference==null || reference.isBlank())throw new IllegalArgumentException("Missing article media reference");
        return UUID.nameUUIDFromBytes(("article-media-v1:"+reference.trim()).getBytes(StandardCharsets.UTF_8));
    }
}
