package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;

public final class ArticleGenerationProviderException extends RuntimeException {
    private final boolean retryable;
    public ArticleGenerationProviderException(String message, boolean retryable, Throwable cause) { super(message, cause); this.retryable = retryable; }
    public ArticleGenerationProviderException(String message, boolean retryable) { super(message); this.retryable = retryable; }
    public boolean retryable() { return retryable; }
}
