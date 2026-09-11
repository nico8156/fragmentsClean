package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class AccountDeletionProcess {
  public enum Context {
    USER_APPLICATION,
    AUTHENTICATION,
    SOCIAL,
    TICKET,
    EXPERIENCE
  }

  public enum Status {
    IN_PROGRESS,
    COMPLETED
  }

  private final UUID requestId;
  private final UUID userId;
  private final Instant requestedAt;
  private final EnumSet<Context> acknowledgements;
  private Status status;
  private Instant completedAt;
  private long version;

  private AccountDeletionProcess(
      UUID requestId,
      UUID userId,
      Instant requestedAt,
      Set<Context> acknowledgements,
      Status status,
      Instant completedAt,
      long version) {
    this.requestId = requestId;
    this.userId = userId;
    this.requestedAt = requestedAt;
    this.acknowledgements =
        acknowledgements.isEmpty()
            ? EnumSet.noneOf(Context.class)
            : EnumSet.copyOf(acknowledgements);
    this.status = status;
    this.completedAt = completedAt;
    this.version = version;
  }

  public static AccountDeletionProcess start(UUID requestId, UUID userId, Instant now) {
    return new AccountDeletionProcess(
        requestId, userId, now, Set.of(), Status.IN_PROGRESS, null, 0L);
  }

  public static AccountDeletionProcess rehydrate(
      UUID requestId,
      UUID userId,
      Instant requestedAt,
      Set<Context> acknowledgements,
      Status status,
      Instant completedAt,
      long version) {
    return new AccountDeletionProcess(
        requestId, userId, requestedAt, acknowledgements, status, completedAt, version);
  }

  public boolean acknowledge(Context context, Instant now) {
    if (status == Status.COMPLETED || !acknowledgements.add(context)) return false;
    version++;
    if (acknowledgements.containsAll(EnumSet.allOf(Context.class))) {
      status = Status.COMPLETED;
      completedAt = now;
    }
    return true;
  }

  public UUID requestId() {
    return requestId;
  }

  public UUID userId() {
    return userId;
  }

  public Instant requestedAt() {
    return requestedAt;
  }

  public Set<Context> acknowledgements() {
    return Set.copyOf(acknowledgements);
  }

  public Status status() {
    return status;
  }

  public Instant completedAt() {
    return completedAt;
  }

  public long version() {
    return version;
  }
}
