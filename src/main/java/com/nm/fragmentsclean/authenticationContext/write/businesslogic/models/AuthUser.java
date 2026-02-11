package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class AuthUser extends AggregateRoot {

	private final AuthProvider provider;
	private final String providerUserId; // Google sub
	private final String email;
	private final boolean emailVerified;

	// ✅ NOUVEAUX CHAMPS
	private String displayName;
	private String avatarUrl;

	private Instant lastLoginAt;

	public AuthUser(UUID id,
			AuthProvider provider,
			String providerUserId,
			String email,
			boolean emailVerified,

			// ✅ nouveaux paramètres
			String displayName,
			String avatarUrl,

			Instant lastLoginAt) {

		super(id);
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.email = email;
		this.emailVerified = emailVerified;

		this.displayName = displayName;
		this.avatarUrl = avatarUrl;

		this.lastLoginAt = lastLoginAt;
	}

	// ----------------------------------------------------------------------
	// FACTORY ENRICHIE
	// ----------------------------------------------------------------------

	public static AuthUser createNew(AuthProvider provider,
			String providerUserId,
			String email,
			boolean emailVerified,

			// ✅ nouveaux arguments
			String displayName,
			String avatarUrl,

			Instant now) {

		UUID id = UUID.randomUUID();

		var authUser = new AuthUser(
				id,
				provider,
				providerUserId,
				email,
				emailVerified,

				displayName,
				avatarUrl,

				now);

		// 🔥 Event enrichi
		authUser.registerEvent(AuthUserCreatedEvent.of(authUser, now));

		return authUser;
	}

	// ----------------------------------------------------------------------
	// EXISTANT CONSERVÉ
	// ----------------------------------------------------------------------

	public void markLogin(Instant now) {
		this.lastLoginAt = now;
		registerEvent(AuthUserLoggedInEvent.of(this, now));
	}

	// ----------------------------------------------------------------------
	// GETTERS
	// ----------------------------------------------------------------------

	public AuthProvider provider() {
		return provider;
	}

	public String providerUserId() {
		return providerUserId;
	}

	public String email() {
		return email;
	}

	public boolean emailVerified() {
		return emailVerified;
	}

	public Instant lastLoginAt() {
		return lastLoginAt;
	}

	// ✅ NOUVEAUX GETTERS

	public String displayName() {
		return displayName;
	}

	public String avatarUrl() {
		return avatarUrl;
	}

	// ----------------------------------------------------------------------
	// FUTUR : possibilité d’update profil
	// ----------------------------------------------------------------------

	public void updateProfile(String displayName, String avatarUrl) {
		this.displayName = displayName;
		this.avatarUrl = avatarUrl;
	}
}
