package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.auth.MaintenancePermissions;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX-free state model for the Maintenance screen.
 *
 * <p>Captures the screen's presentation state and the rules that gate its controls
 * so they can be unit-tested without the graphical toolkit: loading and busy flags,
 * the loaded list, the current selection, the create-editor mode, whether a vehicle
 * is available to schedule maintenance for, and which operations the current role and
 * the selected record's status allow. Every maintenance write is ADMIN-only, so a
 * USER (or unknown/null role) sees a read-only screen. The backend workflow is
 * mirrored exactly — start only from PLANNED, complete only from IN_PROGRESS, and
 * delete only while PLANNED — while the backend remains the authority. There is no
 * edit mode because the backend has no maintenance update. The controller reflects
 * this state onto the JavaFX controls.</p>
 */
public final class MaintenanceViewState {

	/** Editor visibility/mode (there is no edit mode). */
	public enum Mode {
		VIEWING,
		CREATING
	}

	private final boolean canWrite;

	private boolean loading;
	private boolean loaded;
	private boolean busy;
	private boolean vehiclesAvailable;
	private List<MaintenanceResponseDto> records = List.of();
	private MaintenanceResponseDto selected;
	private Mode mode = Mode.VIEWING;

	public MaintenanceViewState(boolean canWrite) {
		this.canWrite = canWrite;
	}

	/** Builds a state whose permission reflects the backend rules for the given role. */
	public static MaintenanceViewState forRole(DesktopUserRole role) {
		return new MaintenanceViewState(MaintenancePermissions.canWrite(role));
	}

	public void beginLoading() {
		this.loading = true;
	}

	public void loadSucceeded(List<MaintenanceResponseDto> loadedRecords) {
		Objects.requireNonNull(loadedRecords, "loadedRecords");
		this.loading = false;
		this.loaded = true;
		this.records = List.copyOf(loadedRecords);
		this.selected = reselect(this.selected);
	}

	public void loadFailed() {
		this.loading = false;
	}

	public void setVehiclesAvailable(boolean available) {
		this.vehiclesAvailable = available;
	}

	public void select(MaintenanceResponseDto record) {
		this.selected = record;
	}

	public void clearSelection() {
		this.selected = null;
	}

	/** @return true if create mode was entered */
	public boolean beginCreate() {
		if (!canCreate()) {
			return false;
		}
		this.mode = Mode.CREATING;
		return true;
	}

	public void cancelForm() {
		this.mode = Mode.VIEWING;
	}

	/**
	 * Marks a save/delete/transition operation as started.
	 *
	 * @return true if the operation may proceed; false if one is already running
	 */
	public boolean beginOperation() {
		if (busy) {
			return false;
		}
		this.busy = true;
		return true;
	}

	public void endOperation() {
		this.busy = false;
	}

	public boolean isLoading() {
		return loading;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public boolean isBusy() {
		return busy;
	}

	public boolean isEmpty() {
		return loaded && records.isEmpty();
	}

	/** @return true when the role may perform no maintenance write (USER or null). */
	public boolean isReadOnly() {
		return !canWrite;
	}

	public boolean isFormVisible() {
		return mode != Mode.VIEWING;
	}

	public Mode mode() {
		return mode;
	}

	public List<MaintenanceResponseDto> records() {
		return records;
	}

	public Optional<MaintenanceResponseDto> selected() {
		return Optional.ofNullable(selected);
	}

	public boolean vehiclesAvailable() {
		return vehiclesAvailable;
	}

	/** Scheduling maintenance requires the write permission and an available vehicle. */
	public boolean canCreate() {
		return canWrite && !busy && !loading && vehiclesAvailable;
	}

	/** Only a PLANNED record can be started. */
	public boolean canStart() {
		return canWrite && !busy && hasStatus(MaintenanceStatus.PLANNED);
	}

	/** Only an IN_PROGRESS record can be completed. */
	public boolean canComplete() {
		return canWrite && !busy && hasStatus(MaintenanceStatus.IN_PROGRESS);
	}

	/** Only a PLANNED record can be deleted. */
	public boolean canDelete() {
		return canWrite && !busy && hasStatus(MaintenanceStatus.PLANNED);
	}

	public boolean canRefresh() {
		return !busy && !loading;
	}

	private boolean hasStatus(MaintenanceStatus status) {
		return selected != null && selected.status() == status;
	}

	private MaintenanceResponseDto reselect(MaintenanceResponseDto previous) {
		if (previous == null || previous.id() == null) {
			return null;
		}
		return records.stream()
				.filter(candidate -> previous.id().equals(candidate.id()))
				.findFirst()
				.orElse(null);
	}
}
