package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Entity(name = "articles")
@ToString
@EqualsAndHashCode
@Getter
public class ArticleJpaEntity {
    @Id
    private UUID id;

    private  String title;

    @ElementCollection
    private List<UUID> contentBlocksIds;
}
