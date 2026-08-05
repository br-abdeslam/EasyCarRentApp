package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DashboardFormatterTest {

	@Test
	void formatsCountsAsPlainIntegers() {
		assertEquals("0", DashboardFormatter.formatCount(0));
		assertEquals("7", DashboardFormatter.formatCount(7));
		assertEquals("1234", DashboardFormatter.formatCount(1234));
	}

	@Test
	void formatsAmountAtScaleTwoWithoutCurrencySymbolAndDashForNull() {
		assertEquals("30.30", DashboardFormatter.formatAmount(new BigDecimal("30.3")));
		assertEquals("0.00", DashboardFormatter.formatAmount(BigDecimal.ZERO));
		assertEquals("125.46", DashboardFormatter.formatAmount(new BigDecimal("125.455")));
		assertEquals("—", DashboardFormatter.formatAmount(null));
	}

	@Test
	void formatsDateTimeStablyAndHandlesNull() {
		assertEquals("2026-08-05 09:30",
				DashboardFormatter.formatDateTime(LocalDateTime.of(2026, 8, 5, 9, 30, 15)));
		assertEquals("", DashboardFormatter.formatDateTime(null));
	}

	@Test
	void exposesAConsistentUnavailablePlaceholder() {
		assertEquals("Unavailable", DashboardFormatter.UNAVAILABLE);
	}
}
