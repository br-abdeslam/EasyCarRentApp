package be.condorcet.easycarrent.desktop.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX-free formatting of customer dates for display.
 *
 * <p>Produces a stable ISO {@code yyyy-MM-dd} string with no time and no
 * timezone, and handles a null date safely by returning an empty string.</p>
 */
public final class CustomerDateFormatter {

	private CustomerDateFormatter() {
	}

	/**
	 * @param date the date, which may be null
	 * @return the date as {@code yyyy-MM-dd}, or an empty string when null
	 */
	public static String formatExpiry(LocalDate date) {
		return date == null ? "" : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}
}
