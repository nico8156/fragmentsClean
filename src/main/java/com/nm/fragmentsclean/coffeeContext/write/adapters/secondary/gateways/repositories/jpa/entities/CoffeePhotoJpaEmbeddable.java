package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CoffeePhotoJpaEmbeddable {
    @Column(name = "photo_id", nullable = false)
    private UUID photoId;
    @Column(name = "photo_uri", nullable = false, length = 2000)
    private String photoUri;
    @Column(name = "is_cover", nullable = false)
    private boolean cover;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CoffeePhotoJpaEmbeddable() { }

    public CoffeePhotoJpaEmbeddable(UUID photoId, String photoUri, boolean cover, int sortOrder) {
        this.photoId = photoId;
        this.photoUri = photoUri;
        this.cover = cover;
        this.sortOrder = sortOrder;
    }

    public UUID photoId() { return photoId; }
    public String photoUri() { return photoUri; }
    public boolean cover() { return cover; }
    public int sortOrder() { return sortOrder; }
}
