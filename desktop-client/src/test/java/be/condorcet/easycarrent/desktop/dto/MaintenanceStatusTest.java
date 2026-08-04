package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

class MaintenanceStatusTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void containsExactlyTheBackendConstants() {
		assertEquals(List.of("PLANNED", "IN_PROGRESS", "COMPLETED"),
				List.of(MaintenanceStatus.PLANNED.name(), MaintenanceStatus.IN_PROGRESS.name(),
						MaintenanceStatus.COMPLETED.name()));
		assertEquals(3, MaintenanceStatus.values().length);
	}

	@Test
	void everyValueRoundTripsThroughJsonByName() throws Exception {
		for (MaintenanceStatus status : MaintenanceStatus.values()) {
			String json = objectMapper.writeValueAsString(status);
			assertEquals("\"" + status.name() + "\"", json);
			assertEquals(status, objectMapper.readValue(json, MaintenanceStatus.class));
		}
	}

	@Test
	void everyValueHasANonBlankDisplayLabel() {
		for (MaintenanceStatus status : MaintenanceStatus.values()) {
			assertFalse(status.displayLabel().isBlank(),
					status.name() + " must have a readable display label");
		}
		assertEquals("Planned", MaintenanceStatus.PLANNED.displayLabel());
		assertEquals("In progress", MaintenanceStatus.IN_PROGRESS.displayLabel());
	}

	@Test
	void rejectsUnknownJsonValue() {
		assertThrows(Exception.class,
				() -> objectMapper.readValue("\"CANCELLED\"", MaintenanceStatus.class));
	}

	@Test
	void noInventedStatusIsPresent() {
		for (MaintenanceStatus status : MaintenanceStatus.values()) {
			assertTrue(status == MaintenanceStatus.PLANNED || status == MaintenanceStatus.IN_PROGRESS
					|| status == MaintenanceStatus.COMPLETED, "unexpected status constant: " + status);
		}
	}
}
