package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.auth.RentalPermissions;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX-free state model for the Rentals screen.
 *
 * <p>Captures the screen's presentation state and the rules that gate its controls
 * so they can be unit-tested without the graphical toolkit: loading and busy flags,
 * the loaded list, the current selection, the editor mode, whether the customer and
 * vehicle lookups are available, and which operations the current role and the
 * selected rental's status allow. The backend workflow is mirrored exactly — only a
 * PLANNED rental can be updated, started, or cancelled; only an ACTIVE rental can be
 * completed; and only a PLANNED or CANCELLED rental can be deleted — while the
 * backend remains the authority. The controller reflects this state onto the JavaFX
 * controls.</p>
 */
public final class RentalViewState {

	/** Editor visibility/mode. */
	public enum Mode {
		VIEWING,
		CREATING,
		EDITING
	}

	private final boolean canCreatePerm;
	private final boolean canUpdatePerm;
	private final boolean canDeletePerm;
	private final boolean canStartPerm;
	private final boolean canCompletePerm;
	private final boolean canCancelPerm;

	private boolean loading;
	private boolean loaded;
	private boolean busy;
	private boolean customersAvailable;
	private boolean vehiclesAvailable;
	private List<RentalResponseDto> rentals = List.of();
	private RentalResponseDto selected;
	private Mode mode = Mode.VIEWING;

	public RentalViewState(boolean canCreatePerm, boolean canUpdatePerm, boolean canDeletePerm,
			boolean canStartPerm, boolean canCompletePerm, boolean canCancelPerm) {
		this.canCreatePerm = canCreatePerm;
		this.canUpdatePerm = canUpdatePerm;
		this.canDeletePerm = canDeletePerm;
		this.canStartPerm = canStartPerm;
		this.canCompletePerm = canCompletePerm;
		this.canCancelPerm = canCancelPerm;
	}

	/** Builds a state whose permissions reflect the backend rules for the given role. */
	public static RentalViewState forRole(DesktopUserRole role) {
		return new RentalViewState(
				RentalPermissions.canCreate(role),
				RentalPermissions.canUpdate(role),
				RentalPermissions.canDelete(role),
				RentalPermissions.canStart(role),
				RentalPermissions.canComplete(role),
				RentalPermissions.canCancel(role));
	}

	public void beginLoading() {
		this.loading = true;
	}

	public void loadSucceeded(List<RentalResponseDto> loadedRentals) {
		Objects.requireNonNull(loadedRentals, "loadedRentals");
		this.loading = false;
		this.loaded = true;
		this.rentals = List.copyOf(loadedRentals);
		this.selected = reselect(this.selected);
	}

	public void loadFailed() {
		this.loading = false;
	}

	public void setCustomersAvailable(boolean available) {
		this.customersAvailable = available;
	}

	public void setVehiclesAvailable(boolean available) {
		this.vehiclesAvailable = available;
	}

	public void select(RentalResponseDto rental) {
		this.selected = rental;
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

	/** @return true if edit mode was entered */
	public boolean beginEdit() {
		if (!canEdit()) {
			return false;
		}
		this.mode = Mode.EDITING;
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
		return loaded && rentals.isEmpty();
	}

	/** @return true only when the role may perform no rental write at all. */
	public boolean isReadOnly() {
		return !(canCreatePerm || canUpdatePerm || canDeletePerm
				|| canStartPerm || canCompletePerm || canCancelPerm);
	}

	/** @return true if the role may ever delete a rental (used to hide the control). */
	public boolean canEverDelete() {
		return canDeletePerm;
	}

	public boolean isFormVisible() {
		return mode != Mode.VIEWING;
	}

	public Mode mode() {
		return mode;
	}

	public List<RentalResponseDto> rentals() {
		return rentals;
	}

	public Optional<RentalResponseDto> selected() {
		return Optional.ofNullable(selected);
	}

	public boolean customersAvailable() {
		return customersAvailable;
	}

	public boolean vehiclesAvailable() {
		return vehiclesAvailable;
	}

	/** Booking requires the create permission and both lookups being usable. */
	public boolean canCreate() {
		return canCreatePerm && !busy && !loading && customersAvailable && vehiclesAvailable;
	}

	/** Only a PLANNED rental can be edited, and the lookups must be usable. */
	public boolean canEdit() {
		return canUpdatePerm && !busy && hasStatus(RentalStatus.PLANNED)
				&& customersAvailable && vehiclesAvailable;
	}

	/** Only a PLANNED or CANCELLED rental can be deleted (ADMIN only). */
	public boolean canDelete() {
		return canDeletePerm && !busy
				&& (hasStatus(RentalStatus.PLANNED) || hasStatus(RentalStatus.CANCELLED));
	}

	/** Only a PLANNED rental can be started. */
	public boolean canStart() {
		return canStartPerm && !busy && hasStatus(RentalStatus.PLANNED);
	}

	/** Only an ACTIVE rental can be completed. */
	public boolean canComplete() {
		return canCompletePerm && !busy && hasStatus(RentalStatus.ACTIVE);
	}

	/** Only a PLANNED rental can be cancelled. */
	public boolean canCancel() {
		return canCancelPerm && !busy && hasStatus(RentalStatus.PLANNED);
	}

	public boolean canRefresh() {
		return !busy && !loading;
	}

	private boolean hasStatus(RentalStatus status) {
		return selected != null && selected.status() == status;
	}

	private RentalResponseDto reselect(RentalResponseDto previous) {
		if (previous == null || previous.id() == null) {
			return null;
		}
		return rentals.stream()
				.filter(candidate -> previous.id().equals(candidate.id()))
				.findFirst()
				.orElse(null);
	}
}
