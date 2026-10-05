package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;
public interface ArticleImageMetadataReader {
    Dimensions read(byte[] bytes);record Dimensions(Integer width,Integer height){}
}
