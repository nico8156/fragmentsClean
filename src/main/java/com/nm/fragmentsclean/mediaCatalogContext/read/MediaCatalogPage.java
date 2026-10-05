package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.List;
public record MediaCatalogPage(List<MediaCatalogView> items, String nextCursor, List<String> coverage) { }
