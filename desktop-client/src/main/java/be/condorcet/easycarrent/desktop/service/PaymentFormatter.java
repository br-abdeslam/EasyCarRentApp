package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX-free formatting of payment values for display.
 *
 * <p>Renders the backend-calculated amount as a plain decimal at the backend
 * monetary scale of two (no currency symbol is added because the backend contract
 * does not declare one), timestamps as a stable {@code yyyy-MM-dd HH:mm} string
 * (no timezone), and the status and method through their readable labels. Every
 * method handles a null input by returning an empty string so the table never
 * shows {@code null}.</p>
 */
public final class PaymentFormatter {

	private static final int MONETARY_SCALE = 2;
	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private PaymentFormatter() {
	}

	/**
	 * @param amount the monetary amount, which may be null
	 * @return the amount at scale two as a plain string, or an empty string when null
	 */
	public static String formatAmount(BigDecimal amount) {
		return amount == null ? "" : amount.setScale(MONETARY_SCALE, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * @param dateTime the timestamp, which may be null
	 * @return the timestamp as {@code yyyy-MM-dd HH:mm}, or an empty string when null
	 */
	public static String formatDateTime(LocalDateTime dateTime) {
		return dateTime == null ? "" : dateTime.format(DATE_TIME);
	}

	/**
	 * @param status the status, which may be null
	 * @return the status display label, or an empty string when null
	 */
	public static String formatStatus(PaymentStatus status) {
		return status == null ? "" : status.displayLabel();
	}

	/**
	 * @param method the method, which may be null
	 * @return the method display label, or an empty string when null
	 */
	public static String formatMethod(PaymentMethod method) {
		return method == null ? "" : method.displayLabel();
	}
}
