package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX-free state model for the Vehicles screen.
 *
 * <p>Captures the screen's presentation state and the rules that gate its
 * controls so they can be unit-tested without the graphical toolkit: loading and
 * busy flags, the loaded list, the current selection, the editor mode, whether
 * the current role may write, and whether categories are available for the
 * editor. Creating or editing requires at least one category to choose from; the
 * controller mirrors this state onto the JavaFX controls.</p>
 */
public final class VehicleViewState {

	/** Editor visibility/mode. */
	public enum Mode {
		VIEWING,
		CREATING,
		EDITING
	}

	private final boolean canWrite;

	private boolean loading;
	private boolean loaded;
	private boolean busy;
	private boolean categoriesAvailable;
	private List<VehicleResponseDto> vehicles = List.of();
	private VehicleResponseDto selected;
	private Mode mode = Mode.VIEWING;

	public VehicleViewState(boolean canWrite) {
		this.canWrite = canWrite;
	}

	public void beginLoading() {
		this.loading = true;
	}

	public void loadSucceeded(List<VehicleResponseDto> loadedVehicles) {
		Objects.requireNonNull(loadedVehicles, "loadedVehicles");
		this.loading = false;
		this.loaded = true;
		this.vehicles = List.copyOf(loadedVehicles);
		this.selected = reselect(this.selected);
	}

	public void loadFailed() {
		this.loading = false;
	}

	public void setCategoriesAvailable(boolean available) {
		this.categoriesAvailable = available;
	}

	public void select(VehicleResponseDto vehicle) {
		this.selected = vehicle;
	}

	public void clearSelection() {
		this.selected = null;
	}

	/** @return true if create mode was entered */
	public boolean beginCreate() {
		if (!canWrite || busy || !categoriesAvailable) {
			return false;
		}
		this.mode = Mode.CREATING;
		return true;
	}

	/** @return true if edit mode was entered */
	public boolean beginEdit() {
		if (!canWrite || busy || !categoriesAvailable || selected == null) {
			return false;
		}
		this.mode = Mode.EDITING;
		return true;
	}

	public void cancelForm() {
		this.mode = Mode.VIEWING;
	}

	/**
	 * Marks a save/delete operation as started.
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
		return loaded && vehicles.isEmpty();
	}

	public boolean isReadOnly() {
		return !canWrite;
	}

	public boolean isFormVisible() {
		return mode != Mode.VIEWING;
	}

	public boolean categoriesAvailable() {
		return categoriesAvailable;
	}

	public Mode mode() {
		return mode;
	}

	public List<VehicleResponseDto> vehicles() {
		return vehicles;
	}

	public Optional<VehicleResponseDto> selected() {
		return Optional.ofNullable(selected);
	}

	public boolean canCreate() {
		return canWrite && !busy && !loading && categoriesAvailable;
	}

	public boolean canEdit() {
		return canWrite && !busy && categoriesAvailable && selected != null;
	}

	public boolean canDelete() {
		return canWrite && !busy && selected != null;
	}

	public boolean canRefresh() {
		return !busy && !loading;
	}

	private VehicleResponseDto reselect(VehicleResponseDto previous) {
		if (previous == null || previous.id() == null) {
			return null;
		}
		return vehicles.stream()
				.filter(candidate -> previous.id().equals(candidate.id()))
				.findFirst()
				.orElse(null);
	}
}
