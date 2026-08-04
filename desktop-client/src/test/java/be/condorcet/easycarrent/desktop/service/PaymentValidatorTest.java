package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentRequestDto;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaymentValidatorTest {

	@Test
	void acceptsValidSelectionAndBuildsRequest() {
		PaymentValidator.Result result = PaymentValidator.validate(4L, PaymentMethod.CARD);

		assertTrue(result.isValid());
		PaymentRequestDto request = result.request();
		assertEquals(4L, request.rentalId());
		assertEquals(PaymentMethod.CARD, request.paymentMethod());
	}

	@Test
	void rejectsMissingRentalAndMethod() {
		assertFalse(PaymentValidator.validate(null, PaymentMethod.CASH).isValid());
		assertFalse(PaymentValidator.validate(4L, null).isValid());
	}

	@Test
	void reportsBothErrorsInOneDeterministicPass() {
		List<String> errors = PaymentValidator.validate(null, null).errors();

		assertEquals(2, errors.size());
		assertTrue(errors.get(0).toLowerCase().contains("rental"));
		assertTrue(errors.get(1).toLowerCase().contains("payment method"));
	}

	@Test
	void returnsNoDuplicateMessages() {
		List<String> errors = PaymentValidator.validate(null, null).errors();
		assertEquals(errors.size(), errors.stream().distinct().count());
	}

	@Test
	void messagesDoNotEchoSubmittedIdentifiers() {
		List<String> errors = PaymentValidator.validate(null, null).errors();
		for (String message : errors) {
			assertFalse(message.matches(".*\\d.*"), "messages must not echo submitted identifiers");
		}
	}

	@Test
	void requestIsUnavailableForAnInvalidResult() {
		PaymentValidator.Result result = PaymentValidator.validate(null, null);
		assertThrows(IllegalStateException.class, result::request);
	}
}
