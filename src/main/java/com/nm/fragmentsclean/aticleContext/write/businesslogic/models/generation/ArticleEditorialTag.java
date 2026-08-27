package com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleDomainException;

import java.text.Normalizer;
import java.util.Locale;

public enum ArticleEditorialTag {
    CULTURE_CAFE("culture cafe"), MATERIEL("materiel"), DIY("diy"), TUTO("tuto"),
    APPROFONDIR("approfondir"), FUN("fun"), DECOUVERTE("decouverte"), VOYAGE("voyage");

    private final String label;
    ArticleEditorialTag(String label) { this.label = label; }
    public String label() { return label; }

    public static ArticleEditorialTag fromProvider(String raw) {
        String normalized = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        for (var tag : values()) if (tag.label.equals(normalized)) return tag;
        throw new ArticleDomainException("Tag éditorial non autorisé: " + raw);
    }
}
