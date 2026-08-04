package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.MaintenanceRequestDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class MaintenanceValidatorTest {

	private static final LocalDate START = LocalDate.of(2027, 9, 1);
	private static final LocalDate END = LocalDate.of(2027, 9, 3);

	@Test
	void acceptsValidInputAndBuildsTrimmedRequest() {
		MaintenanceValidator.Result result =
				MaintenanceValidator.validate(4L, "  Brake inspection  ", START, END, " 180.00 ");

		assertTrue(result.isValid());
		MaintenanceRequestDto request = result.request();
		assertEquals(4L, request.vehicleId());
		assertEquals("Brake inspection", request.description());
		assertEquals(START, request.startDate());
		assertEquals(END, request.endDate());
		assertEquals(0, new BigDecimal("180.00").compareTo(request.cost()));
	}

	@Test
	void acceptsSameDayAndZeroCost() {
		assertTrue(MaintenanceValidator.validate(4L, "Quick check", START, START, "0").isValid(),
				"same-day maintenance is allowed");
		assertTrue(MaintenanceValidator.validate(4L, "Quick check", START, END, "0.00").isValid(),
				"a zero cost is allowed");
	}

	@Test
	void allowsPastDatesBecauseTheBackendDoes() {
		assertTrue(MaintenanceValidator.validate(4L, "Old work",
				LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 2), "50.00").isValid());
	}

	@Test
	void rejectsMissingVehicleDescriptionDatesAndCost() {
		assertFalse(MaintenanceValidator.validate(null, "d", START, END, "10").isValid());
		assertFalse(MaintenanceValidator.validate(4L, " ", START, END, "10").isValid());
		assertFalse(MaintenanceValidator.validate(4L, "d", null, END, "10").isValid());
		assertFalse(MaintenanceValidator.validate(4L, "d", START, null, "10").isValid());
		assertFalse(MaintenanceValidator.validate(4L, "d", START, END, " ").isValid());
	}

	@Test
	void rejectsDescriptionLongerThan500() {
		assertTrue(MaintenanceValidator.validate(4L, "x".repeat(500), START, END, "10").isValid());
		assertFalse(MaintenanceValidator.validate(4L, "x".repeat(501), START, END, "10").isValid());
	}

	@Test
	void rejectsEndBeforeStart() {
		assertFalse(MaintenanceValidator.validate(4L, "d", END, START, "10").isValid(),
				"end before start must be rejected");
	}

	@Test
	void rejectsInvalidNegativeOversizedAndOverScaledCost() {
		assertFalse(MaintenanceValidator.validate(4L, "d", START, END, "abc").isValid(),
				"non-numeric cost");
		assertFalse(MaintenanceValidator.validate(4L, "d", START, END, "-1.00").isValid(),
				"negative cost");
		assertFalse(MaintenanceValidator.validate(4L, "d", START, END, "1.005").isValid(),
				"more than two decimals");
		assertFalse(MaintenanceValidator.validate(4L, "d", START, END, "12345678901").isValid(),
				"more than ten integer digits");
		assertTrue(MaintenanceValidator.validate(4L, "d", START, END, "1234567890.99").isValid(),
				"exactly ten integer digits and two decimals is allowed");
	}

	@Test
	void reportsEveryLocalErrorInOneDeterministicPass() {
		List<String> errors = MaintenanceValidator.validate(null, "", null, null, "").errors();

		assertEquals(5, errors.size());
		assertTrue(errors.get(0).toLowerCase().contains("vehicle"));
		assertTrue(errors.get(1).toLowerCase().contains("description"));
		assertTrue(errors.get(2).toLowerCase().contains("start date"));
		assertTrue(errors.get(3).toLowerCase().contains("end date"));
		assertTrue(errors.get(4).toLowerCase().contains("cost"));
	}

	@Test
	void returnsNoDuplicateMessages() {
		List<String> errors = MaintenanceValidator.validate(null, "", null, null, "").errors();
		assertEquals(errors.size(), errors.stream().distinct().count());
	}

	@Test
	void messagesDoNotEchoSubmittedValues() {
		List<String> errors =
				MaintenanceValidator.validate(4L, "d", END, START, "-1234.56").errors();
		for (String message : errors) {
			assertFalse(message.contains("1234.56"), "must not echo the submitted cost");
		}
	}

	@Test
	void requestIsUnavailableForAnInvalidResult() {
		MaintenanceValidator.Result result = MaintenanceValidator.validate(null, "", null, null, "");
		assertThrows(IllegalStateException.class, result::request);
	}
}
