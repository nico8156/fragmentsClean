package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** HTTP boundary with one deadline, no redirects and a hard response-size limit. */
public final class BoundedEditorialHttpFetcher {
  private final HttpClient httpClient;
  private final PublicEditorialEndpointPolicy endpointPolicy;
  private final Duration requestTimeout;
  private final int maxBodyBytes;

  public BoundedEditorialHttpFetcher(
      HttpClient httpClient,
      PublicEditorialEndpointPolicy endpointPolicy,
      Duration requestTimeout,
      int maxBodyBytes) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    this.endpointPolicy = Objects.requireNonNull(endpointPolicy, "endpointPolicy");
    this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
    if (maxBodyBytes < 1) {
      throw new IllegalArgumentException("maxBodyBytes must be positive");
    }
    this.maxBodyBytes = maxBodyBytes;
  }

  public Response get(String endpoint, String accept, String etag, String lastModified) {
    URI requestedUri = endpointPolicy.validate(endpoint);
    var builder =
        HttpRequest.newBuilder(requestedUri)
            .timeout(requestTimeout)
            .header("Accept", accept)
            .GET();
    if (etag != null && !etag.isBlank()) {
      builder.header("If-None-Match", etag);
    }
    if (lastModified != null && !lastModified.isBlank()) {
      builder.header("If-Modified-Since", lastModified);
    }

    try {
      var pending = httpClient.sendAsync(builder.build(), limitedBodyHandler(maxBodyBytes));
      final HttpResponse<byte[]> response;
      try {
        response = pending.get(requestTimeout.toNanos(), TimeUnit.NANOSECONDS);
      } catch (TimeoutException timeout) {
        pending.cancel(true);
        throw EditorialSourceDiscoveryException.remoteFailure(
            "Editorial source request exceeded its deadline", timeout);
      } catch (ExecutionException failedRequest) {
        throw EditorialSourceDiscoveryException.remoteFailure(
            "Editorial source request failed", failedRequest.getCause());
      }
      if (!requestedUri.equals(response.uri())) {
        throw EditorialSourceDiscoveryException.remoteFailure(
            "Editorial source redirects are forbidden");
      }
      endpointPolicy.validate(response.uri());
      return new Response(
          response.statusCode(),
          response.headers().firstValue("ETag").orElse(null),
          response.headers().firstValue("Last-Modified").orElse(null),
          response.body());
    } catch (EditorialSourceDiscoveryException failure) {
      throw failure;
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw EditorialSourceDiscoveryException.remoteFailure(
          "Editorial source request interrupted", interrupted);
    } catch (Exception failure) {
      throw EditorialSourceDiscoveryException.remoteFailure(
          "Editorial source request failed", failure);
    }
  }

  private static HttpResponse.BodyHandler<byte[]> limitedBodyHandler(int maxBodyBytes) {
    return responseInfo -> new LimitedBodySubscriber(maxBodyBytes, responseInfo.headers().firstValueAsLong("Content-Length").orElse(-1));
  }

  private static Duration requirePositive(Duration value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  public record Response(int statusCode, String etag, String lastModified, byte[] body) {
    public Response {
      body = body.clone();
    }

    @Override
    public byte[] body() {
      return body.clone();
    }
  }

  private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final int maxBodyBytes;
    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final boolean declaredTooLarge;
    private Flow.Subscription subscription;
    private int received;

    private LimitedBodySubscriber(int maxBodyBytes, long contentLength) {
      this.maxBodyBytes = maxBodyBytes;
      this.declaredTooLarge = contentLength > maxBodyBytes;
    }

    @Override
    public CompletionStage<byte[]> getBody() {
      return body;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;
      if (declaredTooLarge) {
        subscription.cancel();
        body.completeExceptionally(new IOException("Editorial response exceeds byte limit"));
      } else {
        subscription.request(Long.MAX_VALUE);
      }
    }

    @Override
    public void onNext(List<ByteBuffer> chunks) {
      if (body.isDone()) {
        return;
      }
      for (var chunk : chunks) {
        int size = chunk.remaining();
        if (size > maxBodyBytes - received) {
          subscription.cancel();
          body.completeExceptionally(new IOException("Editorial response exceeds byte limit"));
          return;
        }
        byte[] bytes = new byte[size];
        chunk.get(bytes);
        buffer.writeBytes(bytes);
        received += size;
      }
    }

    @Override
    public void onError(Throwable failure) {
      body.completeExceptionally(failure);
    }

    @Override
    public void onComplete() {
      body.complete(buffer.toByteArray());
    }
  }
}
