package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;

import java.util.Objects;


public final class Photo {

    private final PhotoId id;
    private final CoffeeId coffeeId;
    private final String uri;
    private final boolean isCover;
    private final int sortOrder;

    public Photo(PhotoId id,
                 CoffeeId coffeeId,
                 String uri,
                 boolean isCover,
                 int sortOrder) {

        this.id = Objects.requireNonNull(id, "photo id required");
        this.coffeeId = Objects.requireNonNull(coffeeId, "coffee id required");

        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("photo uri is required");
        }
        this.uri = uri;

        this.isCover = isCover;
        this.sortOrder = sortOrder;
    }

    public PhotoId id() {
        return id;
    }

    public CoffeeId coffeeId() {
        return coffeeId;
    }

    public String uri() {
        return uri;
    }

    public boolean isCover() {
        return isCover;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
