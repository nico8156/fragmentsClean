package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.util.Objects;

public final class ArticleImageRef {

    private final String storageReference;
    private final int width;
    private final int height;
    private final String alt;

    private ArticleImageRef(String storageReference, int width, int height, String alt) {
        this.storageReference = storageReference;
        this.width = width;
        this.height = height;
        this.alt = alt;
    }

    public static ArticleImageRef from(String storageReference, int width, int height, String alt) {
        var reference = Objects.requireNonNull(storageReference, "La référence image est obligatoire.").trim();
        var alternative = Objects.requireNonNull(alt, "Le texte alternatif est obligatoire.").trim();
        if (reference.isEmpty()) {
            throw new ArticleDomainException("La référence image est obligatoire.");
        }
        if (width <= 0 || height <= 0) {
            throw new ArticleDomainException("Les dimensions de l'image doivent être positives.");
        }
        if (alternative.isEmpty()) {
            throw new ArticleDomainException("Le texte alternatif est obligatoire.");
        }
        return new ArticleImageRef(reference, width, height, alternative);
    }

    public String storageReference() {
        return storageReference;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String alt() {
        return alt;
    }
}
