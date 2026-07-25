package be.condorcet.easycarrent.desktop.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class JsonMapperFactoryTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void deserializesRepresentativeApiError() throws Exception {
		String json = """
				{
				  "timestamp": "2026-07-25T10:15:30Z",
				  "status": 404,
				  "error": "Not Found",
				  "message": "Vehicle 42 was not found",
				  "path": "/api/vehicles/42"
				}
				""";

		ApiErrorDto error = objectMapper.readValue(json, ApiErrorDto.class);

		assertEquals(404, error.status());
		assertEquals("Not Found", error.error());
		assertEquals("Vehicle 42 was not found", error.message());
		assertEquals("/api/vehicles/42", error.path());
		assertNull(error.validationErrors());
	}

	@Test
	void deserializesValidationErrors() throws Exception {
		String json = """
				{
				  "timestamp": "2026-07-25T10:15:30Z",
				  "status": 400,
				  "error": "Bad Request",
				  "message": "Validation failed for one or more fields",
				  "path": "/api/vehicles",
				  "validationErrors": {
				    "registrationNumber": "must not be blank",
				    "dailyPrice": "must be positive"
				  }
				}
				""";

		ApiErrorDto error = objectMapper.readValue(json, ApiErrorDto.class);

		assertNotNull(error.validationErrors());
		assertEquals(2, error.validationErrors().size());
		assertEquals("must not be blank",
				error.validationErrors().get("registrationNumber"));
	}

	@Test
	void toleratesUnknownAndAbsentFields() throws Exception {
		String json = """
				{
				  "status": 500,
				  "message": "An unexpected error occurred",
				  "unexpectedFuture": "ignored"
				}
				""";

		ApiErrorDto error = objectMapper.readValue(json, ApiErrorDto.class);

		assertEquals(500, error.status());
		assertNull(error.validationErrors());
	}

	@Test
	void serializesJavaTimeAsIsoNotTimestamp() throws Exception {
		String json = objectMapper.writeValueAsString(LocalDate.of(2026, 7, 25));

		assertEquals("\"2026-07-25\"", json);
		assertTrue(json.contains("-"), "date must be ISO-8601, not a numeric timestamp");
	}
}
