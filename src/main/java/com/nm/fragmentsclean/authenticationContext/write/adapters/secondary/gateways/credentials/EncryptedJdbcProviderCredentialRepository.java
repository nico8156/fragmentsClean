package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.credentials;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.ProviderCredentialRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public final class EncryptedJdbcProviderCredentialRepository
    implements ProviderCredentialRepository {
  private final JdbcTemplate jdbc;
  private final ProviderCredentialCipher cipher;

  public EncryptedJdbcProviderCredentialRepository(
      JdbcTemplate jdbc, ProviderCredentialCipher cipher) {
    this.jdbc = jdbc;
    this.cipher = cipher;
  }

  @Override
  public void save(UUID userId, AuthProvider provider, String refreshToken) {
    jdbc.update(
        """
        INSERT INTO auth_provider_credentials(user_id,provider,encrypted_refresh_token,updated_at)
        VALUES (?,?,?,now()) ON CONFLICT(user_id,provider) DO UPDATE
        SET encrypted_refresh_token=EXCLUDED.encrypted_refresh_token,updated_at=EXCLUDED.updated_at
        """,
        userId,
        provider.name(),
        cipher.encrypt(refreshToken));
  }

  @Override
  public Optional<String> findRefreshToken(UUID userId, AuthProvider provider) {
    try {
      String value =
          jdbc.queryForObject(
              "SELECT encrypted_refresh_token FROM auth_provider_credentials WHERE user_id=? AND"
                  + " provider=?",
              String.class,
              userId,
              provider.name());
      return Optional.ofNullable(value).map(cipher::decrypt);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(UUID userId, AuthProvider provider) {
    jdbc.update(
        "DELETE FROM auth_provider_credentials WHERE user_id=? AND provider=?",
        userId,
        provider.name());
  }
}
