package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CustomerValidatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 7, 26);
	private static final LocalDate FUTURE = LocalDate.of(2030, 1, 15);

	private CustomerValidator.Result validate(String first, String last, String email, String phone,
			String address, String license, LocalDate expiry) {
		return CustomerValidator.validate(first, last, email, phone, address, license, expiry, TODAY);
	}

	private CustomerValidator.Result valid() {
		return validate("Test", "Customer", "test.customer@example.invalid", "+0000000000",
				"1 Example Street", "TEST-LICENCE-001", FUTURE);
	}

	@Test
	void acceptsValidInputAndBuildsRequestWithTrimmedValues() {
		CustomerValidator.Result result = validate("  Test ", " Customer ",
				" test.customer@example.invalid ", " +0000000000 ", " 1 Example Street ",
				" TEST-LICENCE-001 ", FUTURE);

		assertTrue(result.isValid());
		CustomerRequestDto request = result.request();
		assertEquals("Test", request.firstName());
		assertEquals("Customer", request.lastName());
		assertEquals("test.customer@example.invalid", request.email());
		assertEquals("1 Example Street", request.address());
		assertEquals(FUTURE, request.drivingLicenseExpiryDate());
	}

	@Test
	void rejectsBlankRequiredText() {
		assertFalse(validate("", "L", "a@b.invalid", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "", "a@b.invalid", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "a@b.invalid", "000000", "", "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "a@b.invalid", "000000", "addr", "", FUTURE).isValid());
	}

	@Test
	void rejectsOversizedText() {
		assertFalse(validate("x".repeat(61), "L", "a@b.invalid", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "x".repeat(61), "a@b.invalid", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "x".repeat(115) + "@b.invalid", "000000", "addr", "L", FUTURE)
				.isValid());
		assertFalse(validate("F", "L", "a@b.invalid", "000000", "x".repeat(256), "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "a@b.invalid", "000000", "addr", "x".repeat(41), FUTURE).isValid());
	}

	@Test
	void rejectsMalformedEmail() {
		assertFalse(validate("F", "L", "not-an-email", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "a@b@c.invalid", "000000", "addr", "L", FUTURE).isValid());
		assertFalse(validate("F", "L", "a b@c.invalid", "000000", "addr", "L", FUTURE).isValid());
	}

	@Test
	void enforcesBackendPhonePattern() {
		assertTrue(valid().isValid());
		assertTrue(validate("F", "L", "a@b.invalid", "+32 470 12 34 56", "addr", "L", FUTURE).isValid());
		assertTrue(validate("F", "L", "a@b.invalid", "(012) 345-678", "addr", "L", FUTURE).isValid());
		// Must contain a digit.
		assertFalse(validate("F", "L", "a@b.invalid", "------", "addr", "L", FUTURE).isValid());
		// Too short.
		assertFalse(validate("F", "L", "a@b.invalid", "123", "addr", "L", FUTURE).isValid());
		// Letters not allowed.
		assertFalse(validate("F", "L", "a@b.invalid", "12ab34", "addr", "L", FUTURE).isValid());
	}

	@Test
	void rejectsMissingOrPastExpiryDate() {
		assertFalse(validate("F", "L", "a@b.invalid", "000000", "addr", "L", null).isValid());
		assertFalse(validate("F", "L", "a@b.invalid", "000000", "addr", "L", TODAY.minusDays(1))
				.isValid());
	}

	@Test
	void acceptsTodayAsExpiryDate() {
		assertTrue(validate("F", "L", "a@b.invalid", "000000", "addr", "L", TODAY).isValid());
	}
}
