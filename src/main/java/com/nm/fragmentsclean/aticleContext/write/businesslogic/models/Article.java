package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.util.List;
import java.util.UUID;

public class Article {
    private UUID id;
    private Title title;
    private List<UUID> contentBlocksIds;

    public Article(UUID id, Title title, List<UUID> contentBlocksIds) {
        this.id = id;
        this.title = title;
        this.contentBlocksIds = contentBlocksIds;
    }
    public String title() {
        return title.value();
    }
    public List<UUID> contentBlocks() {
        return contentBlocksIds;
    }
    public ArticleSnapshot toSnapshot() {
        return new ArticleSnapshot(
                this.title.value(),
                this.contentBlocksIds
        );
    }

    public record ArticleSnapshot( String title, List<UUID> contentBlocksIds) {}

}
