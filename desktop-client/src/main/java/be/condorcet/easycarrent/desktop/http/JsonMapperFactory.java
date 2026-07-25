package be.condorcet.easycarrent.desktop.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Central factory for the single JSON {@link ObjectMapper} configuration used
 * across the desktop client.
 *
 * <p>The mapper is configured for the backend contract: ISO-8601 date/time
 * serialization (never numeric timestamps) and tolerance of unknown JSON
 * properties so that additive backend changes do not break the client. No
 * polymorphic typing is enabled.</p>
 */
public final class JsonMapperFactory {

	private JsonMapperFactory() {
	}

	/** @return a new, fully configured {@link ObjectMapper} */
	public static ObjectMapper create() {
		return JsonMapper.builder()
				.addModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
	}
}
