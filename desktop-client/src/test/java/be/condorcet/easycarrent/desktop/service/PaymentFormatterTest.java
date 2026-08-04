package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PaymentFormatterTest {

	@Test
	void formatsAmountAtScaleTwoWithoutCurrencySymbolAndHandlesNull() {
		assertEquals("135.00", PaymentFormatter.formatAmount(new BigDecimal("135")));
		assertEquals("135.50", PaymentFormatter.formatAmount(new BigDecimal("135.5")));
		assertEquals("135.46", PaymentFormatter.formatAmount(new BigDecimal("135.455")));
		assertEquals("", PaymentFormatter.formatAmount(null));
	}

	@Test
	void formatsDateTimeStablyAndHandlesNull() {
		assertEquals("2026-08-01 10:15",
				PaymentFormatter.formatDateTime(LocalDateTime.of(2026, 8, 1, 10, 15, 30)));
		assertEquals("", PaymentFormatter.formatDateTime(null));
	}

	@Test
	void formatsStatusAndMethodUsingReadableLabelsAndHandlesNull() {
		assertEquals("Pending", PaymentFormatter.formatStatus(PaymentStatus.PENDING));
		assertEquals("Refunded", PaymentFormatter.formatStatus(PaymentStatus.REFUNDED));
		assertEquals("", PaymentFormatter.formatStatus(null));
		assertEquals("Bank transfer", PaymentFormatter.formatMethod(PaymentMethod.BANK_TRANSFER));
		assertEquals("", PaymentFormatter.formatMethod(null));
	}
}
