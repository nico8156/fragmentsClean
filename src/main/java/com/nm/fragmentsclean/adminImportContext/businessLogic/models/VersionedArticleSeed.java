package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.Objects;

public record VersionedArticleSeed(
    String seedKey, String lifecycle, StudioArticleSubmission submission) {
  public VersionedArticleSeed {
    Objects.requireNonNull(seedKey, "seedKey");
    Objects.requireNonNull(lifecycle, "lifecycle");
    Objects.requireNonNull(submission, "submission");
  }
}
