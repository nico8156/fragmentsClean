package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcess;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_deletion_processes")
public class AccountDeletionProcessJpaEntity {
  @Id private UUID requestId;

  @Column(nullable = false, unique = true)
  private UUID userId;

  @Column(nullable = false)
  private Instant requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountDeletionProcess.Status status;

  @Column(nullable = false)
  private String acknowledgements;

  private Instant completedAt;

  @Column(nullable = false)
  private long version;

  protected AccountDeletionProcessJpaEntity() {}

  public AccountDeletionProcessJpaEntity(
      UUID requestId,
      UUID userId,
      Instant requestedAt,
      AccountDeletionProcess.Status status,
      String acknowledgements,
      Instant completedAt,
      long version) {
    this.requestId = requestId;
    this.userId = userId;
    this.requestedAt = requestedAt;
    this.status = status;
    this.acknowledgements = acknowledgements;
    this.completedAt = completedAt;
    this.version = version;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public UUID getUserId() {
    return userId;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public AccountDeletionProcess.Status getStatus() {
    return status;
  }

  public String getAcknowledgements() {
    return acknowledgements;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public long getVersion() {
    return version;
  }
}
