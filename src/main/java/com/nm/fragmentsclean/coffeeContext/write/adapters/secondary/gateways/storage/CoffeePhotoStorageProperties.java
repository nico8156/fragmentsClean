package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "coffee.photos.storage")
public class CoffeePhotoStorageProperties {
	private Path directory = Path.of("/tmp/fragments-coffee-photos");
	private String publicBaseUrl = "";

	public Path getDirectory() {
		return directory;
	}

	public void setDirectory(Path directory) {
		this.directory = directory;
	}

	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}
}
