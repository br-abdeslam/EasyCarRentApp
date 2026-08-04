package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PaymentViewStateTest {

	private static PaymentResponseDto payment(long id, PaymentStatus status) {
		return new PaymentResponseDto(id, 4L, RentalStatus.ACTIVE, new BigDecimal("135.00"),
				PaymentMethod.CARD, status, LocalDateTime.of(2026, 8, 1, 10, 0), null);
	}

	private static PaymentViewState adminStateWithRentals() {
		PaymentViewState state = PaymentViewState.forRole(DesktopUserRole.ADMIN);
		state.setRentalsAvailable(true);
		return state;
	}

	@Test
	void startsIdleAndNotLoaded() {
		PaymentViewState state = adminStateWithRentals();
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
		assertFalse(state.isEmpty());
		assertFalse(state.isBusy());
		assertFalse(state.isFormVisible());
		assertTrue(state.selected().isEmpty());
	}

	@Test
	void loadingThenNonEmptyThenEmpty() {
		PaymentViewState state = adminStateWithRentals();
		state.beginLoading();
		assertTrue(state.isLoading());

		state.loadSucceeded(List.of(payment(1, PaymentStatus.PENDING)));
		assertTrue(state.isLoaded());
		assertFalse(state.isEmpty());
		assertEquals(1, state.payments().size());

		state.loadSucceeded(List.of());
		assertTrue(state.isEmpty());
	}

	@Test
	void selectionRestoredByIdAcrossReload() {
		PaymentViewState state = adminStateWithRentals();
		state.loadSucceeded(List.of(payment(1, PaymentStatus.PENDING), payment(2, PaymentStatus.PAID)));
		state.select(payment(2, PaymentStatus.PAID));

		state.loadSucceeded(List.of(payment(2, PaymentStatus.PAID), payment(3, PaymentStatus.PENDING)));

		assertTrue(state.selected().isPresent());
		assertEquals(2L, state.selected().get().id());
	}

	@Test
	void createRequiresAnEligibleRental() {
		PaymentViewState noRentals = PaymentViewState.forRole(DesktopUserRole.ADMIN);
		assertFalse(noRentals.canCreate(), "no eligible rental means creating a payment is disabled");

		noRentals.setRentalsAvailable(true);
		assertTrue(noRentals.canCreate());
		assertTrue(noRentals.beginCreate());
		assertEquals(PaymentViewState.Mode.CREATING, noRentals.mode());

		noRentals.cancelForm();
		assertEquals(PaymentViewState.Mode.VIEWING, noRentals.mode());
	}

	@Test
	void transitionsFollowTheBackendWorkflow() {
		PaymentViewState state = adminStateWithRentals();

		state.select(payment(1, PaymentStatus.PENDING));
		assertTrue(state.canMarkPaid());
		assertTrue(state.canMarkFailed());
		assertFalse(state.canRetry());
		assertFalse(state.canRefund());

		state.select(payment(1, PaymentStatus.FAILED));
		assertTrue(state.canRetry());
		assertFalse(state.canMarkPaid());
		assertFalse(state.canRefund());

		state.select(payment(1, PaymentStatus.PAID));
		assertTrue(state.canRefund());
		assertFalse(state.canMarkPaid());
		assertFalse(state.canRetry());

		state.select(payment(1, PaymentStatus.REFUNDED));
		assertFalse(state.canMarkPaid());
		assertFalse(state.canRetry());
		assertFalse(state.canRefund());
	}

	@Test
	void deleteIsAllowedOnlyForPendingOrFailedAndOnlyForAdmin() {
		PaymentViewState admin = adminStateWithRentals();
		admin.select(payment(1, PaymentStatus.PENDING));
		assertTrue(admin.canDelete());
		admin.select(payment(1, PaymentStatus.FAILED));
		assertTrue(admin.canDelete());
		admin.select(payment(1, PaymentStatus.PAID));
		assertFalse(admin.canDelete());
		admin.select(payment(1, PaymentStatus.REFUNDED));
		assertFalse(admin.canDelete());

		PaymentViewState user = PaymentViewState.forRole(DesktopUserRole.USER);
		user.setRentalsAvailable(true);
		user.select(payment(1, PaymentStatus.PENDING));
		assertFalse(user.canDelete(), "USER may never delete a payment");
		assertFalse(user.canEverDelete());
	}

	@Test
	void refundIsAdminOnly() {
		PaymentViewState user = PaymentViewState.forRole(DesktopUserRole.USER);
		user.select(payment(1, PaymentStatus.PAID));
		assertFalse(user.canRefund(), "USER may never refund");
		assertFalse(user.canEverRefund());

		PaymentViewState admin = adminStateWithRentals();
		admin.select(payment(1, PaymentStatus.PAID));
		assertTrue(admin.canRefund());
		assertTrue(admin.canEverRefund());
	}

	@Test
	void userIsNotReadOnlyButNullRoleIs() {
		PaymentViewState user = PaymentViewState.forRole(DesktopUserRole.USER);
		assertFalse(user.isReadOnly(), "USER can create, pay, fail and retry");

		PaymentViewState anonymous = PaymentViewState.forRole(null);
		assertTrue(anonymous.isReadOnly());
		anonymous.setRentalsAvailable(true);
		assertFalse(anonymous.canCreate());
		assertFalse(anonymous.beginCreate());
	}

	@Test
	void busyPreventsConcurrentOperationsAndGatesActions() {
		PaymentViewState state = adminStateWithRentals();
		state.select(payment(1, PaymentStatus.PENDING));

		assertTrue(state.beginOperation());
		assertFalse(state.beginOperation(), "a second concurrent operation must be refused");
		assertFalse(state.canMarkPaid());
		assertFalse(state.canDelete());
		assertFalse(state.canCreate());

		state.endOperation();
		assertTrue(state.canMarkPaid());
	}

	@Test
	void clearSelectionDisablesSelectionDependentActions() {
		PaymentViewState state = adminStateWithRentals();
		state.select(payment(1, PaymentStatus.PENDING));
		state.clearSelection();

		assertTrue(state.selected().isEmpty());
		assertFalse(state.canMarkPaid());
		assertFalse(state.canDelete());
	}

	@Test
	void refreshIsBlockedWhileLoadingOrBusy() {
		PaymentViewState state = adminStateWithRentals();
		assertTrue(state.canRefresh());
		state.beginLoading();
		assertFalse(state.canRefresh());
		state.loadFailed();
		assertTrue(state.canRefresh());
		state.beginOperation();
		assertFalse(state.canRefresh());
	}
}
