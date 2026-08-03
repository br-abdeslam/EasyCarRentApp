package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

class RentalStatusTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void containsExactlyTheBackendConstants() {
		assertEquals(List.of("PLANNED", "ACTIVE", "COMPLETED", "CANCELLED"),
				List.of(RentalStatus.PLANNED.name(), RentalStatus.ACTIVE.name(),
						RentalStatus.COMPLETED.name(), RentalStatus.CANCELLED.name()));
		assertEquals(4, RentalStatus.values().length);
	}

	@Test
	void everyValueRoundTripsThroughJsonByName() throws Exception {
		for (RentalStatus status : RentalStatus.values()) {
			String json = objectMapper.writeValueAsString(status);
			assertEquals("\"" + status.name() + "\"", json);
			assertEquals(status, objectMapper.readValue(json, RentalStatus.class));
		}
	}

	@Test
	void everyValueHasANonBlankDisplayLabel() {
		for (RentalStatus status : RentalStatus.values()) {
			assertFalse(status.displayLabel().isBlank(),
					status.name() + " must have a readable display label");
		}
		assertEquals("Planned", RentalStatus.PLANNED.displayLabel());
		assertEquals("Cancelled", RentalStatus.CANCELLED.displayLabel());
	}

	@Test
	void rejectsUnknownJsonValue() {
		assertThrows(Exception.class,
				() -> objectMapper.readValue("\"SETTLED\"", RentalStatus.class));
	}

	@Test
	void noInventedStatusIsPresent() {
		for (RentalStatus status : RentalStatus.values()) {
			assertTrue(status == RentalStatus.PLANNED || status == RentalStatus.ACTIVE
					|| status == RentalStatus.COMPLETED || status == RentalStatus.CANCELLED,
					"unexpected status constant: " + status);
		}
	}
}
