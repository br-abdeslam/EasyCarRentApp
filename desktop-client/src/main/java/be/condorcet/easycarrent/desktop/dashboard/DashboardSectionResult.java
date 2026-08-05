package be.condorcet.easycarrent.desktop.dashboard;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of loading one dashboard source section (one domain list).
 *
 * <p>Immutable and JavaFX-free. A section is either <em>available</em> (it carries
 * the loaded records, which may legitimately be an empty list) or
 * <em>unavailable</em> (its request failed, and it carries only a short, safe
 * user-facing message — never a raw exception, credential, {@code Authorization}
 * value, or JSON). An empty successful response and a failed response are kept
 * distinct on purpose: an empty list means "0 records", while an unavailable
 * section must be shown as "Unavailable", not as zero.</p>
 *
 * @param <T> the domain response type for this section
 */
public final class DashboardSectionResult<T> {

	private final List<T> records;
	private final boolean available;
	private final String failureMessage;

	private DashboardSectionResult(List<T> records, boolean available, String failureMessage) {
		this.records = records;
		this.available = available;
		this.failureMessage = failureMessage;
	}

	/** A successful section carrying its (possibly empty) records. */
	public static <T> DashboardSectionResult<T> available(List<T> records) {
		Objects.requireNonNull(records, "records");
		return new DashboardSectionResult<>(List.copyOf(records), true, null);
	}

	/** A failed section carrying only a short, safe message. */
	public static <T> DashboardSectionResult<T> unavailable(String failureMessage) {
		Objects.requireNonNull(failureMessage, "failureMessage");
		return new DashboardSectionResult<>(null, false, failureMessage);
	}

	public boolean isAvailable() {
		return available;
	}

	/**
	 * @return the loaded records
	 * @throws IllegalStateException if this section is unavailable
	 */
	public List<T> records() {
		if (!available) {
			throw new IllegalStateException("Section is unavailable; no records are present");
		}
		return records;
	}

	/** @return the safe failure message, or null when the section is available */
	public String failureMessage() {
		return failureMessage;
	}
}
