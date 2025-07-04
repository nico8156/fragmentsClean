package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.util.UUID;

public class ContentBlock {
    private final UUID id;
    private final UUID articleId;
    private final BlockType type;
    private final ContentValue content;
    private final OrderBlock order;

    public ContentBlock(UUID id, UUID articleId, BlockType type, ContentValue content, OrderBlock order) {
        this.id = id;
        this.articleId = articleId;
        this.type = type;
        this.content = content;
        this.order = order;
    }
    public ContentBlockSnapshot toSnapshot() {
        return new ContentBlockSnapshot(
                this.id,
                this.articleId,
                this.type,
                this.content,
                this.order
        );
    }

    public record ContentBlockSnapshot(UUID id, UUID articleId, BlockType type, ContentValue content, OrderBlock order) {}

}
