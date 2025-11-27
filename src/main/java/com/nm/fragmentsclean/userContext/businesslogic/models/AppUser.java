package com.nm.fragmentsclean.userContext.businesslogic.models;

import com.nm.fragmentsclean.authContext.businesslogic.models.Identity;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.BadgeProgressSnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.LinkedIdentitySnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.UserPreferencesSnapshot;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AppUser {

    private final UUID id;
    private Instant createdAt;
    private Instant updatedAt;

    private String displayName;
    private String avatarUrl;
    private String bio;

    private List<String> roles;
    private Map<String, Boolean> flags;

    private UserPreferences preferences;
    private List<UUID> likedCoffeeIds;

    private long version;

    public AppUser(
            UUID id,
            Instant createdAt,
            Instant updatedAt,
            String displayName,
            String avatarUrl,
            String bio,
            List<String> roles,
            Map<String, Boolean> flags,
            UserPreferences preferences,
            List<UUID> likedCoffeeIds,
            long version
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        this.flags = flags != null ? new HashMap<>(flags) : new HashMap<>();
        this.preferences = preferences;
        this.likedCoffeeIds = likedCoffeeIds != null ? new ArrayList<>(likedCoffeeIds) : new ArrayList<>();
        this.version = version;
    }

    public static AppUser createNewFromOAuthProfile(
            String displayName,
            String avatarUrl,
            String locale,
            Instant now
    ) {
        return new AppUser(
                UUID.randomUUID(),
                now,
                now,
                displayName,
                avatarUrl,
                null,
                List.of("user"),                              // rôle par défaut
                Map.of(),                                     // aucun flag au début
                UserPreferences.defaultForLocale(locale),
                List.of(),
                0L
        );
    }

    public void markAuthenticated(Instant now) {
        this.updatedAt = now;
    }

    // --------- MAPPING → SNAPSHOT READ MODEL ---------

    public AppUserSnapshot toSnapshot(List<Identity> identities) {
        var identitySnapshots = identities.stream()
                .map(this::toLinkedIdentitySnapshot)
                .collect(Collectors.toList());

        var badgeProgress = preferences != null && preferences.badgeProgress() != null
                ? new BadgeProgressSnapshot(
                preferences.badgeProgress().exploration(),
                preferences.badgeProgress().gout(),
                preferences.badgeProgress().social(),
                preferences.badgeProgress().unlockedBadges()
        )
                : new BadgeProgressSnapshot(0, 0, 0, List.of());

        var prefsSnapshot = preferences != null
                ? new UserPreferencesSnapshot(
                preferences.locale(),
                preferences.marketingOptIn(),
                preferences.pushOptIn(),
                preferences.theme(),
                badgeProgress
        )
                : new UserPreferencesSnapshot(
                "fr-FR",
                false,
                true,
                "system",
                badgeProgress
        );

        List<String> likedIds = likedCoffeeIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());

        return new AppUserSnapshot(
                id.toString(),
                createdAt.toString(),
                updatedAt.toString(),
                displayName,
                avatarUrl,
                bio,
                identitySnapshots,
                List.copyOf(roles),
                Map.copyOf(flags),
                prefsSnapshot,
                likedIds,
                version
        );
    }

    private LinkedIdentitySnapshot toLinkedIdentitySnapshot(Identity identity) {
        return new LinkedIdentitySnapshot(
                identity.id().toString(),
                identity.provider(),
                identity.providerUserId(),
                identity.email(),
                identity.createdAt() != null ? identity.createdAt().toString() : null,
                identity.lastAuthAt() != null ? identity.lastAuthAt().toString() : null
        );
    }

    // --------- GETTERS ---------

    public UUID id() { return id; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public String displayName() { return displayName; }
    public String avatarUrl() { return avatarUrl; }
    public String bio() { return bio; }
    public List<String> roles() { return Collections.unmodifiableList(roles); }
    public Map<String, Boolean> flags() { return Collections.unmodifiableMap(flags); }
    public UserPreferences preferences() { return preferences; }
    public List<UUID> likedCoffeeIds() { return Collections.unmodifiableList(likedCoffeeIds); }
    public long version() { return version; }
}
