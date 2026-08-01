package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CustomerDateFormatterTest {

	@Test
	void formatsRepresentativeDateAsIso() {
		assertEquals("2030-01-15",
				CustomerDateFormatter.formatExpiry(LocalDate.of(2030, 1, 15)));
	}

	@Test
	void outputIsStable() {
		LocalDate date = LocalDate.of(2027, 12, 3);
		assertEquals(CustomerDateFormatter.formatExpiry(date),
				CustomerDateFormatter.formatExpiry(date));
		assertEquals("2027-12-03", CustomerDateFormatter.formatExpiry(date));
	}

	@Test
	void nullDateFormatsAsEmpty() {
		assertEquals("", CustomerDateFormatter.formatExpiry(null));
	}

	@Test
	void outputCarriesNoTimeOrTimezone() {
		String formatted = CustomerDateFormatter.formatExpiry(LocalDate.of(2030, 1, 15));
		assertFalse(formatted.contains("T"), "must not contain a time separator");
		assertFalse(formatted.contains(":"), "must not contain a time");
		assertFalse(formatted.toUpperCase().contains("Z"), "must not contain a timezone");
	}
}
