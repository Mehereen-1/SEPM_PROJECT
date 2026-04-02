package com.example.project;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		configureDatasourceFromRenderUrl();
		SpringApplication.run(ProjectApplication.class, args);
	}

	private static void configureDatasourceFromRenderUrl() {
		String currentDatasourceUrl = trimToNull(System.getProperty("spring.datasource.url"));
		if (currentDatasourceUrl != null && currentDatasourceUrl.startsWith("jdbc:")) {
			return;
		}

		String rawUrl = firstNonBlank(
			trimToNull(System.getenv("DB_URL")),
			trimToNull(System.getenv("DATABASE_URL"))
		);

		if (rawUrl == null) {
			return;
		}

		if (rawUrl.startsWith("jdbc:")) {
			System.setProperty("spring.datasource.url", rawUrl);
			return;
		}

		if (!(rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
			return;
		}

		String normalized = rawUrl.startsWith("postgres://")
			? "postgresql://" + rawUrl.substring("postgres://".length())
			: rawUrl;

		URI uri = URI.create(normalized);
		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String path = trimToNull(uri.getPath());

		if (host == null || path == null) {
			return;
		}

		StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
			.append(host)
			.append(":")
			.append(port)
			.append(path);

		String query = trimToNull(uri.getRawQuery());
		if (query != null) {
			jdbcUrl.append("?").append(query);
		}

		System.setProperty("spring.datasource.url", jdbcUrl.toString());

		String userInfo = trimToNull(uri.getRawUserInfo());
		if (userInfo != null) {
			String[] parts = userInfo.split(":", 2);
			if (trimToNull(System.getProperty("spring.datasource.username")) == null && parts.length >= 1) {
				System.setProperty("spring.datasource.username", decode(parts[0]));
			}
			if (trimToNull(System.getProperty("spring.datasource.password")) == null && parts.length == 2) {
				System.setProperty("spring.datasource.password", decode(parts[1]));
			}
		}
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

}
