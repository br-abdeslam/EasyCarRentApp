package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaymentMethodTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void containsExactlyTheBackendConstants() {
		assertEquals(List.of("CASH", "CARD", "BANK_TRANSFER"),
				List.of(PaymentMethod.CASH.name(), PaymentMethod.CARD.name(),
						PaymentMethod.BANK_TRANSFER.name()));
		assertEquals(3, PaymentMethod.values().length);
	}

	@Test
	void everyValueRoundTripsThroughJsonByName() throws Exception {
		for (PaymentMethod method : PaymentMethod.values()) {
			String json = objectMapper.writeValueAsString(method);
			assertEquals("\"" + method.name() + "\"", json);
			assertEquals(method, objectMapper.readValue(json, PaymentMethod.class));
		}
	}

	@Test
	void everyValueHasANonBlankDisplayLabel() {
		for (PaymentMethod method : PaymentMethod.values()) {
			assertFalse(method.displayLabel().isBlank(),
					method.name() + " must have a readable display label");
		}
		assertEquals("Cash", PaymentMethod.CASH.displayLabel());
		assertEquals("Bank transfer", PaymentMethod.BANK_TRANSFER.displayLabel());
	}

	@Test
	void rejectsUnknownJsonValue() {
		assertThrows(Exception.class,
				() -> objectMapper.readValue("\"CRYPTO\"", PaymentMethod.class));
	}

	@Test
	void noInventedMethodIsPresent() {
		for (PaymentMethod method : PaymentMethod.values()) {
			assertTrue(method == PaymentMethod.CASH || method == PaymentMethod.CARD
					|| method == PaymentMethod.BANK_TRANSFER, "unexpected method constant: " + method);
		}
	}
}
