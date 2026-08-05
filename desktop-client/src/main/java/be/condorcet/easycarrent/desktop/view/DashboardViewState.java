package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.dashboard.DashboardSnapshot;

/**
 * JavaFX-free state model for the Dashboard screen.
 *
 * <p>Tracks the loading flag, the last usable snapshot, and one general status
 * message so the controller can mirror it without the graphical toolkit. It
 * prevents duplicate simultaneous refreshes and distinguishes a first load from a
 * refresh:</p>
 *
 * <ul>
 *   <li><b>First load, fully successful</b> — adopt the snapshot; no message.</li>
 *   <li><b>First load, partial</b> — adopt the (partial) snapshot; a concise
 *       partial-data notice.</li>
 *   <li><b>First load, fully failed</b> — no snapshot; an unavailable notice; Refresh
 *       stays enabled.</li>
 *   <li><b>Refresh, fully successful</b> — replace the snapshot (new last-updated
 *       time); clear notices.</li>
 *   <li><b>Refresh, partial or fully failed</b> — preserve the previous snapshot and
 *       its last-updated time; show an incomplete-refresh notice.</li>
 * </ul>
 *
 * <p>Stale data is therefore never presented as freshly refreshed, and a later
 * successful refresh clears old errors. It holds no JavaFX controls, services,
 * credentials, or endpoint paths.</p>
 */
public final class DashboardViewState {

	/** The kind of general status message, if any. */
	public enum StatusKind {
		NONE,
		PARTIAL,
		UNAVAILABLE,
		REFRESH_INCOMPLETE
	}

	static final String PARTIAL_MESSAGE = "Some dashboard sections could not be loaded.";
	static final String UNAVAILABLE_MESSAGE = "Dashboard data is currently unavailable.";
	static final String REFRESH_INCOMPLETE_MESSAGE =
			"The dashboard could not be refreshed. Previously loaded data is still shown.";

	private boolean loading;
	private DashboardSnapshot snapshot;
	private StatusKind statusKind = StatusKind.NONE;
	private String statusMessage = "";

	/**
	 * Marks a load/refresh as started.
	 *
	 * @return true if it may proceed; false if one is already running
	 */
	public boolean beginLoad() {
		if (loading) {
			return false;
		}
		this.loading = true;
		return true;
	}

	/**
	 * Applies a completed load using the first-load-vs-refresh policy.
	 *
	 * @param newSnapshot  the freshly computed snapshot
	 * @param allAvailable whether every section loaded successfully
	 * @param anyAvailable whether at least one section loaded successfully
	 */
	public void loadSucceeded(DashboardSnapshot newSnapshot, boolean allAvailable,
			boolean anyAvailable) {
		this.loading = false;
		boolean firstLoad = this.snapshot == null;
		if (firstLoad) {
			if (anyAvailable) {
				this.snapshot = newSnapshot;
				setStatus(allAvailable ? StatusKind.NONE : StatusKind.PARTIAL,
						allAvailable ? "" : PARTIAL_MESSAGE);
			} else {
				setStatus(StatusKind.UNAVAILABLE, UNAVAILABLE_MESSAGE);
			}
		} else if (allAvailable) {
			this.snapshot = newSnapshot;
			setStatus(StatusKind.NONE, "");
		} else {
			// Preserve the previous snapshot and its last-updated time.
			setStatus(StatusKind.REFRESH_INCOMPLETE, REFRESH_INCOMPLETE_MESSAGE);
		}
	}

	/**
	 * Applies an unexpected whole-load failure (the load future itself failing).
	 * Preserves any previous snapshot; otherwise shows the unavailable state.
	 */
	public void loadFailed() {
		this.loading = false;
		if (this.snapshot == null) {
			setStatus(StatusKind.UNAVAILABLE, UNAVAILABLE_MESSAGE);
		} else {
			setStatus(StatusKind.REFRESH_INCOMPLETE, REFRESH_INCOMPLETE_MESSAGE);
		}
	}

	private void setStatus(StatusKind kind, String message) {
		this.statusKind = kind;
		this.statusMessage = message;
	}

	public boolean isLoading() {
		return loading;
	}

	/** @return true once a usable snapshot has been adopted. */
	public boolean hasSnapshot() {
		return snapshot != null;
	}

	/** @return the current snapshot, or null when none has been adopted. */
	public DashboardSnapshot snapshot() {
		return snapshot;
	}

	public StatusKind statusKind() {
		return statusKind;
	}

	public String statusMessage() {
		return statusMessage;
	}

	public boolean hasStatusMessage() {
		return statusKind != StatusKind.NONE;
	}

	/** Refresh is allowed whenever no load is in flight. */
	public boolean canRefresh() {
		return !loading;
	}
}
