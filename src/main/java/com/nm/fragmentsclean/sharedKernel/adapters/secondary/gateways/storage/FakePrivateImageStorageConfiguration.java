package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver;

@Configuration(proxyBeanMethods = false)
@Profile("fake")
@EnableConfigurationProperties(PrivateImageStorageProperties.class)
public class FakePrivateImageStorageConfiguration {
  @Bean
  PrivateImageStore privateImageStore() {
    return new PrivateImageStore() {
      @Override public UploadTarget presignUpload(String key, String type, Duration ttl, Instant at) {
        return new UploadTarget(URI.create("https://upload.test/" + key), "PUT", Map.of("Content-Type", type), at.plus(ttl));
      }
      @Override public ProcessedImage normalize(String pending, String target, String type, ImageRules rules) {
        return new ProcessedImage(target, "image/jpeg", 512, Math.min(1200, rules.maxWidth()), Math.min(800, rules.maxHeight()), "fake-sha256");
      }
      @Override public URI presignDownload(String key, Duration ttl) { return URI.create("https://media.test/" + key); }
      @Override public void delete(String key) {}
    };
  }

	@Bean PrivateMediaObjectKeys privateMediaObjectKeys(PrivateImageStorageProperties properties) {
		return new PrivateMediaObjectKeys(properties.normalizedPrefix());
	}

	@Bean PrivateMediaUrlResolver privateMediaUrlResolver(
			PrivateImageStore store, PrivateImageStorageProperties properties) {
		return new PrivateMediaUrlResolver(store, properties.getDownloadTtl());
	}
}
