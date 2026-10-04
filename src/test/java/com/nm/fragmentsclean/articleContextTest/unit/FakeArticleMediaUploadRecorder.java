package com.nm.fragmentsclean.articleContextTest.unit;
import java.util.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaUploadRecorder;
public final class FakeArticleMediaUploadRecorder implements ArticleMediaUploadRecorder {
    public final List<String> references=new ArrayList<>();
    public void record(UUID articleId,String reference,String originalName,String contentType,byte[] bytes,Integer width,Integer height,UUID uploadedBy,String purpose){references.add(reference);}
}
