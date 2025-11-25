package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Entity {

    @Getter
    @EqualsAndHashCode.Include
    protected final UUID id;

    public Entity(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }

}
