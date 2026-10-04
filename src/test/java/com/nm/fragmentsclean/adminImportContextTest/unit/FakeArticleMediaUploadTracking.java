package com.nm.fragmentsclean.adminImportContextTest.unit;
import java.util.*;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleMediaUploadTracking;
public final class FakeArticleMediaUploadTracking implements ArticleMediaUploadTracking {
    public final List<String> references=new ArrayList<>();
    public void record(UUID articleId,String reference,String originalName,String contentType,byte[] bytes,Integer width,Integer height,UUID uploadedBy,String purpose){references.add(reference);}
}
