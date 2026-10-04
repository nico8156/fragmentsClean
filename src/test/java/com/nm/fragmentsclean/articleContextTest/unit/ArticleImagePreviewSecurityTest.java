package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.storage.DefaultArticleImageUriResolver;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class ArticleImagePreviewSecurityTest {
    private final ArticleImageStorageProperties properties = new ArticleImageStorageProperties();
    private final String key = "fragments/test/articles/11111111-1111-4111-8111-111111111111/images/22222222-2222-4222-8222-222222222222.png";

    ArticleImagePreviewSecurityTest() {
        properties.setS3Bucket("articles-test");
        properties.setS3Prefix("fragments/test/articles");
        properties.setS3PresignTtl(Duration.ofMinutes(2));
    }

    @Test void signs_only_a_reference_inside_the_configured_article_scope() {
        try (var signer = signer()) {
            String preview = new DefaultArticleImageUriResolver(properties, signer).resolve("s3://articles-test/" + key);
            var uri = URI.create(preview);
            assertThat(uri.getHost()).isEqualTo("articles-test.s3.eu-west-3.amazonaws.com");
            assertThat(uri.getPath()).isEqualTo("/" + key);
            assertThat(uri.getQuery()).contains("X-Amz-Signature=", "X-Amz-Expires=120");
        }
    }

    @Test void refuses_a_foreign_bucket_before_signing() {
        try (var signer = signer()) {
            assertThatThrownBy(() -> new DefaultArticleImageUriResolver(properties, signer).resolve("s3://private-user-data/" + key))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void refuses_other_domains_and_prefix_lookalikes() {
        try (var signer = signer()) {
            var resolver = new DefaultArticleImageUriResolver(properties, signer);
            for (String outside : new String[]{"avatars/private.png", "fragments/test/articles-other/private.png", "fragments/test/articles"}) {
                assertThatThrownBy(() -> resolver.resolve("s3://articles-test/" + outside)).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test void refuses_ambiguous_or_traversing_references() {
        try (var signer = signer()) {
            var resolver = new DefaultArticleImageUriResolver(properties, signer);
            for (String ref : new String[]{
                "s3://articles-test/fragments/test/articles/../private.png",
                "s3://articles-test/fragments/test/articles/%2e%2e/private.png",
                "s3://articles-test/" + key + "?otherKey=secret",
                "s3://articles-test/" + key + "#fragment",
                "s3://operator@articles-test/" + key,
                "s3://articles-test:443/" + key}) {
                assertThatThrownBy(() -> resolver.resolve(ref)).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test void refuses_signing_when_the_bucket_or_prefix_scope_is_unconfigured() {
        try (var signer = signer()) {
            var resolver = new DefaultArticleImageUriResolver(properties, signer);
            properties.setS3Bucket("");
            assertThatThrownBy(() -> resolver.resolve("s3://articles-test/" + key)).isInstanceOf(IllegalStateException.class);
            properties.setS3Bucket("articles-test");
            properties.setS3Prefix("////");
            assertThatThrownBy(() -> resolver.resolve("s3://articles-test/" + key)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test void malformed_reference_errors_do_not_include_private_paths() {
        try (var signer = signer()) {
            var resolver = new DefaultArticleImageUriResolver(properties, signer);
            assertThatThrownBy(() -> resolver.resolve("s3://articles-test/fragments/test/articles/private customer.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("private customer")
                .hasNoCause();
        }
    }

    @Test void keeps_external_and_local_references_without_signing() {
        var resolver = new DefaultArticleImageUriResolver(properties, null);
        assertThat(resolver.resolve(" https://images.test/cover.png ")).isEqualTo("https://images.test/cover.png");
        assertThat(resolver.resolve("/api/articles/image-assets/cover.png")).isEqualTo("/api/articles/image-assets/cover.png");
        assertThat(resolver.resolve(null)).isNull();
    }

    private S3Presigner signer() {
        return S3Presigner.builder().region(Region.EU_WEST_3)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))).build();
    }
}
