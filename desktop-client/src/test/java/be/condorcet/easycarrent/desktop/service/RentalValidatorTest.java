package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.RentalRequestDto;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class RentalValidatorTest {

	private static final LocalDate START = LocalDate.of(2026, 9, 1);
	private static final LocalDate END = LocalDate.of(2026, 9, 4);

	@Test
	void acceptsValidSelectionAndBuildsRequest() {
		RentalValidator.Result result = RentalValidator.validate(3L, 4L, START, END);

		assertTrue(result.isValid());
		RentalRequestDto request = result.request();
		assertEquals(3L, request.customerId());
		assertEquals(4L, request.vehicleId());
		assertEquals(START, request.startDate());
		assertEquals(END, request.endDate());
	}

	@Test
	void rejectsMissingCustomerVehicleAndDates() {
		assertFalse(RentalValidator.validate(null, 4L, START, END).isValid());
		assertFalse(RentalValidator.validate(3L, null, START, END).isValid());
		assertFalse(RentalValidator.validate(3L, 4L, null, END).isValid());
		assertFalse(RentalValidator.validate(3L, 4L, START, null).isValid());
	}

	@Test
	void rejectsEndBeforeOrEqualToStart() {
		assertFalse(RentalValidator.validate(3L, 4L, END, START).isValid(),
				"end before start must be rejected");
		assertFalse(RentalValidator.validate(3L, 4L, START, START).isValid(),
				"equal start and end must be rejected (the backend requires end strictly after start)");
	}

	@Test
	void allowsPastDatesBecauseTheBackendDoes() {
		LocalDate pastStart = LocalDate.of(2000, 1, 1);
		LocalDate pastEnd = LocalDate.of(2000, 1, 3);
		assertTrue(RentalValidator.validate(3L, 4L, pastStart, pastEnd).isValid(),
				"the backend does not reject past dates, so the client must not either");
	}

	@Test
	void reportsEveryLocalErrorInOneDeterministicPass() {
		List<String> errors = RentalValidator.validate(null, null, null, null).errors();

		assertEquals(4, errors.size());
		assertTrue(errors.get(0).toLowerCase().contains("customer"));
		assertTrue(errors.get(1).toLowerCase().contains("vehicle"));
		assertTrue(errors.get(2).toLowerCase().contains("start date"));
		assertTrue(errors.get(3).toLowerCase().contains("end date"));
	}

	@Test
	void reportsCustomerAndDateOrderTogether() {
		List<String> errors = RentalValidator.validate(null, 4L, END, START).errors();
		assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("customer")));
		assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("after the start")));
	}

	@Test
	void returnsNoDuplicateMessages() {
		List<String> errors = RentalValidator.validate(null, null, null, null).errors();
		assertEquals(errors.size(), errors.stream().distinct().count());
	}

	@Test
	void messagesDoNotEchoSubmittedIdentifiers() {
		List<String> errors = RentalValidator.validate(3L, 4L, END, START).errors();
		for (String message : errors) {
			assertFalse(message.contains("3"), "must not echo the submitted customer id");
			assertFalse(message.contains("4"), "must not echo the submitted vehicle id");
		}
	}

	@Test
	void requestIsUnavailableForAnInvalidResult() {
		RentalValidator.Result result = RentalValidator.validate(null, null, null, null);
		assertThrows(IllegalStateException.class, result::request);
	}
}
