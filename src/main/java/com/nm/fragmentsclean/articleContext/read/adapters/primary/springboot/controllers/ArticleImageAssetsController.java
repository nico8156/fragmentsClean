package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.controllers;

import java.nio.file.Files;
import java.util.Locale;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;

@RestController
public class ArticleImageAssetsController {
	private final ArticleImageStorageProperties properties;

	public ArticleImageAssetsController(ArticleImageStorageProperties properties) {
		this.properties = properties;
	}

	@GetMapping("/api/articles/image-assets/{fileName:.+}")
	public ResponseEntity<FileSystemResource> readImage(@PathVariable String fileName) {
		if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
			return ResponseEntity.notFound().build();
		}
		var file = properties.getDirectory().resolve(fileName).normalize();
		if (!file.startsWith(properties.getDirectory().normalize()) || !Files.isRegularFile(file)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
				.contentType(mediaType(fileName))
				.body(new FileSystemResource(file));
	}

	private MediaType mediaType(String fileName) {
		var lower = fileName.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".png")) {
			return MediaType.IMAGE_PNG;
		}
		if (lower.endsWith(".webp")) {
			return MediaType.parseMediaType("image/webp");
		}
		if (lower.endsWith(".gif")) {
			return MediaType.IMAGE_GIF;
		}
		return MediaType.IMAGE_JPEG;
	}
}
