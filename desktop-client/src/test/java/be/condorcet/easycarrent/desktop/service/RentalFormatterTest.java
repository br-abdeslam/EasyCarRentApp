package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.condorcet.easycarrent.desktop.dto.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class RentalFormatterTest {

	@Test
	void formatsDateAsIsoAndHandlesNull() {
		assertEquals("2026-09-01", RentalFormatter.formatDate(LocalDate.of(2026, 9, 1)));
		assertEquals("", RentalFormatter.formatDate(null));
	}

	@Test
	void formatsPriceAtScaleTwoWithoutCurrencySymbolAndHandlesNull() {
		assertEquals("135.00", RentalFormatter.formatPrice(new BigDecimal("135")));
		assertEquals("135.50", RentalFormatter.formatPrice(new BigDecimal("135.5")));
		assertEquals("135.46", RentalFormatter.formatPrice(new BigDecimal("135.455")));
		assertEquals("", RentalFormatter.formatPrice(null));
	}

	@Test
	void formatsStatusUsingReadableLabelAndHandlesNull() {
		assertEquals("Planned", RentalFormatter.formatStatus(RentalStatus.PLANNED));
		assertEquals("Cancelled", RentalFormatter.formatStatus(RentalStatus.CANCELLED));
		assertEquals("", RentalFormatter.formatStatus(null));
	}
}
