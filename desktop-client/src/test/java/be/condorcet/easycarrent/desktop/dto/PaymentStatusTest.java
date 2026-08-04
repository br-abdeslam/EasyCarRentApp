package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaymentStatusTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void containsExactlyTheBackendConstants() {
		assertEquals(List.of("PENDING", "PAID", "FAILED", "REFUNDED"),
				List.of(PaymentStatus.PENDING.name(), PaymentStatus.PAID.name(),
						PaymentStatus.FAILED.name(), PaymentStatus.REFUNDED.name()));
		assertEquals(4, PaymentStatus.values().length);
	}

	@Test
	void everyValueRoundTripsThroughJsonByName() throws Exception {
		for (PaymentStatus status : PaymentStatus.values()) {
			String json = objectMapper.writeValueAsString(status);
			assertEquals("\"" + status.name() + "\"", json);
			assertEquals(status, objectMapper.readValue(json, PaymentStatus.class));
		}
	}

	@Test
	void everyValueHasANonBlankDisplayLabel() {
		for (PaymentStatus status : PaymentStatus.values()) {
			assertFalse(status.displayLabel().isBlank(),
					status.name() + " must have a readable display label");
		}
		assertEquals("Pending", PaymentStatus.PENDING.displayLabel());
		assertEquals("Refunded", PaymentStatus.REFUNDED.displayLabel());
	}

	@Test
	void rejectsUnknownJsonValue() {
		assertThrows(Exception.class,
				() -> objectMapper.readValue("\"SETTLED\"", PaymentStatus.class));
	}

	@Test
	void noInventedStatusIsPresent() {
		for (PaymentStatus status : PaymentStatus.values()) {
			assertTrue(status == PaymentStatus.PENDING || status == PaymentStatus.PAID
					|| status == PaymentStatus.FAILED || status == PaymentStatus.REFUNDED,
					"unexpected status constant: " + status);
		}
	}
}
