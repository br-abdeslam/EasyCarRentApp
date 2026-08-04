package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX-free formatting of maintenance values for display.
 *
 * <p>Renders dates as a stable ISO {@code yyyy-MM-dd} string (no time, no
 * timezone), the cost as a plain decimal at the backend monetary scale of two (no
 * currency symbol is added because the backend contract does not declare one), and
 * the status through its readable label. Every method handles a null input by
 * returning an empty string so the table never shows {@code null}.</p>
 */
public final class MaintenanceFormatter {

	private static final int MONETARY_SCALE = 2;

	private MaintenanceFormatter() {
	}

	/**
	 * @param date the date, which may be null
	 * @return the date as {@code yyyy-MM-dd}, or an empty string when null
	 */
	public static String formatDate(LocalDate date) {
		return date == null ? "" : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * @param cost the monetary amount, which may be null
	 * @return the amount at scale two as a plain string, or an empty string when null
	 */
	public static String formatCost(BigDecimal cost) {
		return cost == null ? "" : cost.setScale(MONETARY_SCALE, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * @param status the status, which may be null
	 * @return the status display label, or an empty string when null
	 */
	public static String formatStatus(MaintenanceStatus status) {
		return status == null ? "" : status.displayLabel();
	}
}
