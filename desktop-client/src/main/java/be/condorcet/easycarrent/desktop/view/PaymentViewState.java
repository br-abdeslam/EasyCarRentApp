package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.auth.PaymentPermissions;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX-free state model for the Payments screen.
 *
 * <p>Captures the screen's presentation state and the rules that gate its controls
 * so they can be unit-tested without the graphical toolkit: loading and busy flags,
 * the loaded list, the current selection, the create-editor mode, whether an
 * eligible rental is available to pay, and which operations the current role and
 * the selected payment's status allow. The backend workflow is mirrored exactly —
 * pay/fail only from PENDING, retry only from FAILED, refund only from PAID, and
 * delete only for PENDING/FAILED — while the backend remains the authority. There
 * is no edit mode because the backend has no payment update. The controller
 * reflects this state onto the JavaFX controls.</p>
 */
public final class PaymentViewState {

	/** Editor visibility/mode (there is no edit mode). */
	public enum Mode {
		VIEWING,
		CREATING
	}

	private final boolean canCreatePerm;
	private final boolean canMarkPaidPerm;
	private final boolean canMarkFailedPerm;
	private final boolean canRetryPerm;
	private final boolean canRefundPerm;
	private final boolean canDeletePerm;

	private boolean loading;
	private boolean loaded;
	private boolean busy;
	private boolean rentalsAvailable;
	private List<PaymentResponseDto> payments = List.of();
	private PaymentResponseDto selected;
	private Mode mode = Mode.VIEWING;

	public PaymentViewState(boolean canCreatePerm, boolean canMarkPaidPerm, boolean canMarkFailedPerm,
			boolean canRetryPerm, boolean canRefundPerm, boolean canDeletePerm) {
		this.canCreatePerm = canCreatePerm;
		this.canMarkPaidPerm = canMarkPaidPerm;
		this.canMarkFailedPerm = canMarkFailedPerm;
		this.canRetryPerm = canRetryPerm;
		this.canRefundPerm = canRefundPerm;
		this.canDeletePerm = canDeletePerm;
	}

	/** Builds a state whose permissions reflect the backend rules for the given role. */
	public static PaymentViewState forRole(DesktopUserRole role) {
		return new PaymentViewState(
				PaymentPermissions.canCreate(role),
				PaymentPermissions.canMarkPaid(role),
				PaymentPermissions.canMarkFailed(role),
				PaymentPermissions.canRetry(role),
				PaymentPermissions.canRefund(role),
				PaymentPermissions.canDelete(role));
	}

	public void beginLoading() {
		this.loading = true;
	}

	public void loadSucceeded(List<PaymentResponseDto> loadedPayments) {
		Objects.requireNonNull(loadedPayments, "loadedPayments");
		this.loading = false;
		this.loaded = true;
		this.payments = List.copyOf(loadedPayments);
		this.selected = reselect(this.selected);
	}

	public void loadFailed() {
		this.loading = false;
	}

	public void setRentalsAvailable(boolean available) {
		this.rentalsAvailable = available;
	}

	public void select(PaymentResponseDto payment) {
		this.selected = payment;
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
		return loaded && payments.isEmpty();
	}

	/** @return true only when the role may perform no payment write at all. */
	public boolean isReadOnly() {
		return !(canCreatePerm || canMarkPaidPerm || canMarkFailedPerm
				|| canRetryPerm || canRefundPerm || canDeletePerm);
	}

	/** @return true if the role may ever refund (used to hide the control). */
	public boolean canEverRefund() {
		return canRefundPerm;
	}

	/** @return true if the role may ever delete (used to hide the control). */
	public boolean canEverDelete() {
		return canDeletePerm;
	}

	public boolean isFormVisible() {
		return mode != Mode.VIEWING;
	}

	public Mode mode() {
		return mode;
	}

	public List<PaymentResponseDto> payments() {
		return payments;
	}

	public Optional<PaymentResponseDto> selected() {
		return Optional.ofNullable(selected);
	}

	public boolean rentalsAvailable() {
		return rentalsAvailable;
	}

	/** Creating a payment requires the create permission and an eligible rental. */
	public boolean canCreate() {
		return canCreatePerm && !busy && !loading && rentalsAvailable;
	}

	/** Only a PENDING payment can be marked paid. */
	public boolean canMarkPaid() {
		return canMarkPaidPerm && !busy && hasStatus(PaymentStatus.PENDING);
	}

	/** Only a PENDING payment can be marked failed. */
	public boolean canMarkFailed() {
		return canMarkFailedPerm && !busy && hasStatus(PaymentStatus.PENDING);
	}

	/** Only a FAILED payment can be retried. */
	public boolean canRetry() {
		return canRetryPerm && !busy && hasStatus(PaymentStatus.FAILED);
	}

	/** Only a PAID payment can be refunded. */
	public boolean canRefund() {
		return canRefundPerm && !busy && hasStatus(PaymentStatus.PAID);
	}

	/** Only a PENDING or FAILED payment can be deleted. */
	public boolean canDelete() {
		return canDeletePerm && !busy
				&& (hasStatus(PaymentStatus.PENDING) || hasStatus(PaymentStatus.FAILED));
	}

	public boolean canRefresh() {
		return !busy && !loading;
	}

	private boolean hasStatus(PaymentStatus status) {
		return selected != null && selected.status() == status;
	}

	private PaymentResponseDto reselect(PaymentResponseDto previous) {
		if (previous == null || previous.id() == null) {
			return null;
		}
		return payments.stream()
				.filter(candidate -> previous.id().equals(candidate.id()))
				.findFirst()
				.orElse(null);
	}
}
