package com.nm.fragmentsclean.articleContext.read;
import java.net.URI;
public final class ArticleMediaPreviewUrls {
    private ArticleMediaPreviewUrls(){}
    public static String resolve(ArticleImageUriResolver previews,String reference){
        try {
            var value=previews.resolve(reference);if(value==null)return null;
            if(value.matches("/api/articles/image-assets/[A-Za-z0-9_.-]+") && !value.contains(".."))return value;
            var uri=URI.create(value);return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost()!=null && uri.getRawUserInfo()==null?value:null;
        }catch(IllegalArgumentException | IllegalStateException e){return null;}
    }
}
