package com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities.UserJpaEntity;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userContext.businesslogic.models.UserPreferences;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class JpaUserRepository implements UserRepository {

    private final SpringUserRepository springUserRepository;

    public JpaUserRepository(SpringUserRepository springUserRepository) {
        this.springUserRepository = springUserRepository;
    }

    @Override
    public Optional<AppUser> findById(UUID userId) {
        return springUserRepository.findById(userId)
                .map(this::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        var entity = toEntity(user);
        var saved = springUserRepository.save(entity);
        return toDomain(saved);
    }

    private AppUser toDomain(UserJpaEntity e) {
        // Pour l’instant : on reconstruit un AppUser minimal.
        // On garde la logique de roles / flags / likedCoffeeIds très simple.
        var prefs = UserPreferences.defaultForLocale(e.getLocale());

        return new AppUser(
                e.getId(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getDisplayName(),
                e.getAvatarUrl(),
                e.getBio(),
                List.of("user"),   // rôle par défaut
                Map.of(),          // aucun flag
                prefs,
                List.of(),         // likedCoffeeIds vide pour l’instant
                e.getVersion()
        );
    }

    private UserJpaEntity toEntity(AppUser u) {
        var prefs = u.preferences();
        var locale = (prefs != null && prefs.locale() != null)
                ? prefs.locale()
                : "fr-FR";

        return new UserJpaEntity(
                u.id(),
                u.createdAt(),
                u.updatedAt(),
                u.displayName(),
                u.avatarUrl(),
                u.bio(),
                locale,
                u.version()
        );
    }
}
