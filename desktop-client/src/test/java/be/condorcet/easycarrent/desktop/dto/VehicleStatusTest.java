package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class VehicleStatusTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void hasExactlyTheBackendValuesInOrder() {
		assertArrayEquals(
				new VehicleStatus[] {
						VehicleStatus.AVAILABLE, VehicleStatus.RENTED,
						VehicleStatus.MAINTENANCE, VehicleStatus.INACTIVE},
				VehicleStatus.values());
		assertEquals(4, VehicleStatus.values().length);
	}

	@Test
	void serializesByName() throws Exception {
		assertEquals("\"AVAILABLE\"", objectMapper.writeValueAsString(VehicleStatus.AVAILABLE));
		assertEquals("\"MAINTENANCE\"", objectMapper.writeValueAsString(VehicleStatus.MAINTENANCE));
	}

	@Test
	void deserializesEveryValueByName() throws Exception {
		for (VehicleStatus status : VehicleStatus.values()) {
			VehicleStatus parsed = objectMapper.readValue("\"" + status.name() + "\"", VehicleStatus.class);
			assertEquals(status, parsed);
		}
	}

	@Test
	void unsupportedValueFailsSafely() {
		assertThrows(IOException.class,
				() -> objectMapper.readValue("\"FLYING\"", VehicleStatus.class));
	}

	@Test
	void displayLabelsAreNonBlankAndUnique() {
		long distinct = Arrays.stream(VehicleStatus.values())
				.map(VehicleStatus::displayLabel)
				.distinct()
				.count();
		assertEquals(VehicleStatus.values().length, distinct);
		for (VehicleStatus status : VehicleStatus.values()) {
			assertFalse(status.displayLabel() == null || status.displayLabel().isBlank(),
					status + " must have a non-blank display label");
		}
	}
}
