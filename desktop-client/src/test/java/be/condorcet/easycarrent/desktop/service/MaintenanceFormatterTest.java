package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MaintenanceFormatterTest {

	@Test
	void formatsDateAsIsoAndHandlesNull() {
		assertEquals("2027-09-01", MaintenanceFormatter.formatDate(LocalDate.of(2027, 9, 1)));
		assertEquals("", MaintenanceFormatter.formatDate(null));
	}

	@Test
	void formatsCostAtScaleTwoWithoutCurrencySymbolAndHandlesNull() {
		assertEquals("180.00", MaintenanceFormatter.formatCost(new BigDecimal("180")));
		assertEquals("180.50", MaintenanceFormatter.formatCost(new BigDecimal("180.5")));
		assertEquals("180.46", MaintenanceFormatter.formatCost(new BigDecimal("180.455")));
		assertEquals("0.00", MaintenanceFormatter.formatCost(BigDecimal.ZERO));
		assertEquals("", MaintenanceFormatter.formatCost(null));
	}

	@Test
	void formatsStatusUsingReadableLabelAndHandlesNull() {
		assertEquals("Planned", MaintenanceFormatter.formatStatus(MaintenanceStatus.PLANNED));
		assertEquals("In progress", MaintenanceFormatter.formatStatus(MaintenanceStatus.IN_PROGRESS));
		assertEquals("", MaintenanceFormatter.formatStatus(null));
	}
}
