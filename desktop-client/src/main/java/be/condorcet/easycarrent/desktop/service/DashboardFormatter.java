package be.condorcet.easycarrent.desktop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX-free formatting of dashboard values for display.
 *
 * <p>Renders counts as stable integers, monetary amounts as a plain decimal at the
 * backend monetary scale of two (no currency symbol is added because the contract
 * declares none), and the refresh time as a readable {@code yyyy-MM-dd HH:mm} local
 * time (labelled by the controller as a client refresh time, never as server time).
 * The {@link #UNAVAILABLE} placeholder is used by the controller for a section whose
 * request failed, which is deliberately distinct from a zero count.</p>
 */
public final class DashboardFormatter {

	/** Placeholder shown for a section whose source request failed. */
	public static final String UNAVAILABLE = "Unavailable";

	private static final int MONETARY_SCALE = 2;
	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private DashboardFormatter() {
	}

	/** @return the count as a plain integer string. */
	public static String formatCount(long count) {
		return Long.toString(count);
	}

	/**
	 * @param amount the monetary amount, which may be null
	 * @return the amount at scale two as a plain string, or a dash when null
	 */
	public static String formatAmount(BigDecimal amount) {
		return amount == null ? "—"
				: amount.setScale(MONETARY_SCALE, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * @param dateTime the refresh time, which may be null
	 * @return the time as {@code yyyy-MM-dd HH:mm}, or an empty string when null
	 */
	public static String formatDateTime(LocalDateTime dateTime) {
		return dateTime == null ? "" : dateTime.format(DATE_TIME);
	}
}
