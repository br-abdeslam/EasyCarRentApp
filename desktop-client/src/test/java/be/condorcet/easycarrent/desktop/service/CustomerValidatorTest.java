package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;

import java.time.LocalDate;
import java.util.List;

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

	// --- aggregation regression ------------------------------------------------

	@Test
	void doesNotStopAtTheFirstInvalidField() {
		List<String> errors = validate("", "", "bad", "12ab", "", "", TODAY.minusDays(1)).errors();
		assertTrue(errors.size() > 1, "validation must not stop after the first error");
	}

	@Test
	void reportsFirstNameAndEmailErrorsTogether() {
		List<String> errors = validate("", "Last", "bad-email", "000000", "addr", "L", FUTURE).errors();
		assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("first name")));
		assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("email")));
	}

	@Test
	void reportsPhoneAndExpiryErrorsTogether() {
		List<String> errors =
				validate("First", "Last", "a@b.invalid", "12ab", "addr", "L", TODAY.minusDays(1))
						.errors();
		assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("phone")));
		assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("expiry")));
	}

	@Test
	void reportsEverySevenFieldsInOneDeterministicPass() {
		List<String> errors = validate("", "", "bad-email", "12ab", "", "", TODAY.minusDays(1))
				.errors();

		assertEquals(7, errors.size());
		// Deterministic field order: first, last, email, phone, address, licence, expiry.
		assertTrue(errors.get(0).toLowerCase().contains("first name"));
		assertTrue(errors.get(1).toLowerCase().contains("last name"));
		assertTrue(errors.get(2).toLowerCase().contains("email"));
		assertTrue(errors.get(3).toLowerCase().contains("phone"));
		assertTrue(errors.get(4).toLowerCase().contains("address"));
		assertTrue(errors.get(5).toLowerCase().contains("licence"));
		assertTrue(errors.get(6).toLowerCase().contains("expiry"));
	}

	@Test
	void returnsNoDuplicateMessages() {
		List<String> errors = validate("", "", "bad-email", "12ab", "", "", TODAY.minusDays(1))
				.errors();
		assertEquals(errors.size(), errors.stream().distinct().count());
	}

	@Test
	void correctingOneFieldKeepsTheRemainingErrors() {
		List<String> allInvalid = validate("", "", "bad-email", "12ab", "", "", TODAY.minusDays(1))
				.errors();
		List<String> firstNameFixed =
				validate("First", "", "bad-email", "12ab", "", "", TODAY.minusDays(1)).errors();

		assertEquals(allInvalid.size() - 1, firstNameFixed.size());
		assertFalse(firstNameFixed.stream().anyMatch(e -> e.toLowerCase().contains("first name")));
		assertTrue(firstNameFixed.stream().anyMatch(e -> e.toLowerCase().contains("email")));
		assertTrue(firstNameFixed.stream().anyMatch(e -> e.toLowerCase().contains("expiry")));
	}

	@Test
	void reportedEmailAndPhoneScenarioReturnsBothErrorsWithEmailFirst() {
		// Exact reproduction: only email and phone are invalid; all other fields valid.
		CustomerValidator.Result result = validate("Test", "Customer", "gmail.com",
				"+32 455 5A55 55", "Fictional test address", "TEST-LICENCE-001", FUTURE);

		List<String> errors = result.errors();
		assertEquals(2, errors.size(), "both the email and phone errors must be reported");
		int emailIndex = indexOfContaining(errors, "email");
		int phoneIndex = indexOfContaining(errors, "phone");
		assertTrue(emailIndex >= 0, "an email error must be present");
		assertTrue(phoneIndex >= 0, "a phone error must be present");
		assertTrue(emailIndex < phoneIndex, "the email error must appear before the phone error");

		String joined = be.condorcet.easycarrent.desktop.service.CustomerMessages
				.localValidation(errors);
		assertTrue(joined.contains("\n"), "the two messages must be separated by a newline");
	}

	private static int indexOfContaining(List<String> errors, String needle) {
		for (int i = 0; i < errors.size(); i++) {
			if (errors.get(i).toLowerCase().contains(needle)) {
				return i;
			}
		}
		return -1;
	}

	@Test
	void messagesDoNotIncludeSubmittedValues() {
		List<String> errors = validate("First", "Last", "not-an-email",
				"+3212345678901234567890", "1 Example Street", "TEST-LICENCE-001", TODAY.minusDays(1))
				.errors();
		for (String message : errors) {
			assertFalse(message.contains("not-an-email"), "must not echo the submitted email");
			assertFalse(message.contains("1 Example Street"), "must not echo the submitted address");
			assertFalse(message.contains("TEST-LICENCE-001"), "must not echo the submitted licence");
		}
	}
}
