package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApproval;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApprovalRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApprovalIssuer;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApprovalValidator;

@Component
public final class ArticleReviewApprovalTokenService implements ArticleReviewApprovalIssuer, ArticleReviewApprovalValidator {
	private static final String VERSION = "v1";
	private final ArticleReviewApprovalRepository approvals;
	private final ArticleReviewApprovalProperties properties;

	public ArticleReviewApprovalTokenService(
			ArticleReviewApprovalRepository approvals,
			ArticleReviewApprovalProperties properties) {
		this.approvals = approvals;
		this.properties = properties;
	}

	@Override
	public String issue(UUID sagaId, UUID articleId, UUID revisionId, Instant now) {
		if (properties.secret() == null || properties.secret().isBlank()) {
			throw new IllegalStateException("Editorial approval secret is not configured");
		}
		var existing = approvals.findBySagaAndRevision(sagaId, revisionId).orElse(null);
		if (existing != null && existing.isActiveAt(now)) {
			return tokenFor(existing.sagaId(), existing.articleId(), existing.revisionId(), existing.expiresAt());
		}
		Instant expiresAt = now.plus(properties.ttl());
		String token = tokenFor(sagaId, articleId, revisionId, expiresAt);
		approvals.save(new ArticleReviewApproval(
				UUID.randomUUID(), sagaId, articleId, revisionId, hash(token), now, expiresAt, null));
		return token;
	}

	@Override
	public ArticleReviewApproval validate(String token, Instant now) {
		if (properties.secret() == null || properties.secret().isBlank() || token == null || token.isBlank()) {
			throw new IllegalArgumentException("Approval token is invalid");
		}
		String[] parts = token.split("\\.", -1);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Approval token is invalid");
		}
		String payload = decode(parts[0]);
		byte[] providedSignature;
		try {
			providedSignature = Base64.getUrlDecoder().decode(parts[1]);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Approval token is invalid", exception);
		}
		if (!MessageDigest.isEqual(sign(payload), providedSignature)) {
			throw new IllegalArgumentException("Approval token is invalid");
		}
		String[] claims = payload.split("\\|", -1);
		if (claims.length != 5 || !VERSION.equals(claims[0])) {
			throw new IllegalArgumentException("Approval token is invalid");
		}
		try {
			UUID sagaId = UUID.fromString(claims[1]);
			UUID articleId = UUID.fromString(claims[2]);
			UUID revisionId = UUID.fromString(claims[3]);
			Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(claims[4]));
			if (!expiresAt.isAfter(now)) {
				throw new IllegalArgumentException("Approval token has expired");
			}
			var approval = approvals.findByTokenHash(hash(token))
					.filter(value -> value.sagaId().equals(sagaId))
					.filter(value -> value.articleId().equals(articleId))
					.filter(value -> value.revisionId().equals(revisionId))
					.filter(value -> value.expiresAt().equals(expiresAt))
					.orElseThrow(() -> new IllegalArgumentException("Approval token is invalid"));
			if (!approval.isActiveAt(now)) {
				throw new IllegalArgumentException("Approval token is no longer active");
			}
			return approval;
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("Approval token is invalid", exception);
		}
	}

	@Override
	public boolean consume(UUID approvalId, Instant consumedAt) {
		return approvals.consume(approvalId, consumedAt);
	}

	private String tokenFor(UUID sagaId, UUID articleId, UUID revisionId, Instant expiresAt) {
		String payload = String.join("|", VERSION, sagaId.toString(), articleId.toString(), revisionId.toString(), Long.toString(expiresAt.getEpochSecond()));
		String encodedPayload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
		return encodedPayload + "." + signature;
	}

	private byte[] sign(String payload) {
		try {
			var mac = javax.crypto.Mac.getInstance("HmacSHA256");
			mac.init(new javax.crypto.spec.SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		} catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("Cannot sign editorial approval", exception);
		}
	}

	private String decode(String value) {
		try {
			return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Approval token is invalid", exception);
		}
	}

	private String hash(String value) {
		try {
			return Base64.getUrlEncoder().withoutPadding().encodeToString(
					MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Cannot hash editorial approval", exception);
		}
	}
}
