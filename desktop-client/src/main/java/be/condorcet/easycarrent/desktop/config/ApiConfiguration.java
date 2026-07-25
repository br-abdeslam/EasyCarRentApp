package be.condorcet.easycarrent.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Loads immutable desktop-client configuration from a classpath properties file.
 *
 * <p>The only value required today is the backend base URL. The base URL is
 * normalized so it always ends with a trailing slash, which lets the HTTP layer
 * resolve relative API paths (for example {@code api/ping}) safely, including
 * when a servlet context path is present.</p>
 */
public final class ApiConfiguration {

	static final String DEFAULT_RESOURCE =
			"/be/condorcet/easycarrent/desktop/config/desktop.properties";
	static final String BASE_URL_KEY = "api.base-url";

	private final URI baseUri;

	/** Loads configuration from the default classpath resource. */
	public ApiConfiguration() {
		this(DEFAULT_RESOURCE);
	}

	/**
	 * Loads configuration from the given classpath resource. Exposed primarily so
	 * the loading behavior can be exercised with alternative resources in tests.
	 *
	 * @param resourcePath absolute classpath path to a properties resource
	 */
	public ApiConfiguration(String resourcePath) {
		this.baseUri = loadBaseUri(resourcePath);
	}

	/** @return the normalized backend base URI, always ending with a slash */
	public URI baseUri() {
		return baseUri;
	}

	private static URI loadBaseUri(String resourcePath) {
		Properties properties = new Properties();
		try (InputStream input = ApiConfiguration.class.getResourceAsStream(resourcePath)) {
			if (input == null) {
				throw new IllegalStateException(
						"Configuration resource not found on the classpath: " + resourcePath);
			}
			properties.load(input);
		} catch (IOException e) {
			throw new IllegalStateException(
					"Unable to read configuration resource: " + resourcePath, e);
		}

		String rawBaseUrl = properties.getProperty(BASE_URL_KEY);
		if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
			throw new IllegalStateException(
					"Missing or blank '" + BASE_URL_KEY + "' in " + resourcePath);
		}
		return normalize(rawBaseUrl.trim());
	}

	private static URI normalize(String rawBaseUrl) {
		String withTrailingSlash = rawBaseUrl.endsWith("/") ? rawBaseUrl : rawBaseUrl + "/";
		try {
			return new URI(withTrailingSlash);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(
					"Invalid '" + BASE_URL_KEY + "' value: " + rawBaseUrl, e);
		}
	}
}
