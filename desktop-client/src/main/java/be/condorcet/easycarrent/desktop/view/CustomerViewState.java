package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX-free state model for the Customers screen.
 *
 * <p>Captures the screen's presentation state and the rules that gate its
 * controls so they can be unit-tested without the graphical toolkit: loading and
 * busy flags, the loaded list, the current selection, the editor mode, and
 * whether the current role may write. The controller mirrors this state onto the
 * JavaFX controls.</p>
 */
public final class CustomerViewState {

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
	private List<CustomerResponseDto> customers = List.of();
	private CustomerResponseDto selected;
	private Mode mode = Mode.VIEWING;

	public CustomerViewState(boolean canWrite) {
		this.canWrite = canWrite;
	}

	public void beginLoading() {
		this.loading = true;
	}

	public void loadSucceeded(List<CustomerResponseDto> loadedCustomers) {
		Objects.requireNonNull(loadedCustomers, "loadedCustomers");
		this.loading = false;
		this.loaded = true;
		this.customers = List.copyOf(loadedCustomers);
		this.selected = reselect(this.selected);
	}

	public void loadFailed() {
		this.loading = false;
	}

	public void select(CustomerResponseDto customer) {
		this.selected = customer;
	}

	public void clearSelection() {
		this.selected = null;
	}

	/** @return true if create mode was entered */
	public boolean beginCreate() {
		if (!canWrite || busy) {
			return false;
		}
		this.mode = Mode.CREATING;
		return true;
	}

	/** @return true if edit mode was entered */
	public boolean beginEdit() {
		if (!canWrite || busy || selected == null) {
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
		return loaded && customers.isEmpty();
	}

	public boolean isReadOnly() {
		return !canWrite;
	}

	public boolean isFormVisible() {
		return mode != Mode.VIEWING;
	}

	public Mode mode() {
		return mode;
	}

	public List<CustomerResponseDto> customers() {
		return customers;
	}

	public Optional<CustomerResponseDto> selected() {
		return Optional.ofNullable(selected);
	}

	public boolean canCreate() {
		return canWrite && !busy && !loading;
	}

	public boolean canEdit() {
		return canWrite && !busy && selected != null;
	}

	public boolean canDelete() {
		return canWrite && !busy && selected != null;
	}

	public boolean canRefresh() {
		return !busy && !loading;
	}

	private CustomerResponseDto reselect(CustomerResponseDto previous) {
		if (previous == null || previous.id() == null) {
			return null;
		}
		return customers.stream()
				.filter(candidate -> previous.id().equals(candidate.id()))
				.findFirst()
				.orElse(null);
	}
}
