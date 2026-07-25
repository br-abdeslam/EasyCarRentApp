package be.condorcet.easycarrent.desktop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;

class ApiConfigurationTest {

	@Test
	void defaultConfigurationResourceExists() {
		assertNotNull(
				ApiConfiguration.class.getResource(ApiConfiguration.DEFAULT_RESOURCE),
				"desktop.properties must exist on the classpath");
	}

	@Test
	void loadsAndParsesBaseUrlAsUri() {
		ApiConfiguration configuration = new ApiConfiguration();

		URI baseUri = configuration.baseUri();

		assertNotNull(baseUri);
		assertEquals("http", baseUri.getScheme());
		assertEquals("localhost", baseUri.getHost());
		assertEquals(8080, baseUri.getPort());
	}

	@Test
	void baseUrlIsNormalizedWithTrailingSlash() {
		ApiConfiguration configuration = new ApiConfiguration();

		assertTrue(configuration.baseUri().toString().endsWith("/"),
				"base URL must be normalized to end with a slash");
	}

	@Test
	void trailingSlashNormalizationPreservesContextPath() {
		ApiConfiguration configuration =
				new ApiConfiguration("/test-config/context-path-base-url.properties");

		assertEquals("http://localhost:8080/api-gateway/",
				configuration.baseUri().toString());
	}

	@Test
	void missingConfigurationResourceIsRejected() {
		assertThrows(IllegalStateException.class,
				() -> new ApiConfiguration("/test-config/does-not-exist.properties"));
	}

	@Test
	void blankBaseUrlIsRejected() {
		assertThrows(IllegalStateException.class,
				() -> new ApiConfiguration("/test-config/blank-base-url.properties"));
	}

	@Test
	void missingBaseUrlKeyIsRejected() {
		assertThrows(IllegalStateException.class,
				() -> new ApiConfiguration("/test-config/no-base-url.properties"));
	}
}
