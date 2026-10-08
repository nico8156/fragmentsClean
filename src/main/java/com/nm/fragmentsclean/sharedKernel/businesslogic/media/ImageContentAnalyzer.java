package com.nm.fragmentsclean.sharedKernel.businesslogic.media;

import java.net.URI;

/** Technical classification signal. Each producer owns publication and human review decisions. */
public interface ImageContentAnalyzer {
  boolean flagged(URI imageInput);
}
