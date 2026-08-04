package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class RentalViewStateTest {

	private static RentalResponseDto rental(long id, RentalStatus status) {
		return new RentalResponseDto(id, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4),
				status, new BigDecimal("135.00"), LocalDateTime.of(2026, 8, 1, 10, 0),
				4L, "TEST-REG-001", "TestBrand", "TestModel", 3L, "Test", "Customer");
	}

	private static RentalViewState adminStateWithLookups() {
		RentalViewState state = RentalViewState.forRole(DesktopUserRole.ADMIN);
		state.setCustomersAvailable(true);
		state.setVehiclesAvailable(true);
		return state;
	}

	@Test
	void startsIdleAndNotLoaded() {
		RentalViewState state = adminStateWithLookups();
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
		assertFalse(state.isEmpty());
		assertFalse(state.isBusy());
		assertFalse(state.isFormVisible());
		assertTrue(state.selected().isEmpty());
	}

	@Test
	void loadingThenNonEmptyThenEmpty() {
		RentalViewState state = adminStateWithLookups();
		state.beginLoading();
		assertTrue(state.isLoading());

		state.loadSucceeded(List.of(rental(1, RentalStatus.PLANNED)));
		assertTrue(state.isLoaded());
		assertFalse(state.isEmpty());
		assertEquals(1, state.rentals().size());

		state.loadSucceeded(List.of());
		assertTrue(state.isEmpty());
	}

	@Test
	void selectionRestoredByIdAcrossReload() {
		RentalViewState state = adminStateWithLookups();
		state.loadSucceeded(List.of(rental(1, RentalStatus.PLANNED), rental(2, RentalStatus.ACTIVE)));
		state.select(rental(2, RentalStatus.ACTIVE));

		state.loadSucceeded(List.of(rental(2, RentalStatus.ACTIVE), rental(3, RentalStatus.PLANNED)));

		assertTrue(state.selected().isPresent());
		assertEquals(2L, state.selected().get().id());
	}

	@Test
	void createRequiresBothLookups() {
		RentalViewState noLookups = RentalViewState.forRole(DesktopUserRole.ADMIN);
		assertFalse(noLookups.canCreate(), "no lookups means booking is disabled");

		noLookups.setCustomersAvailable(true);
		assertFalse(noLookups.canCreate(), "a vehicle is still required");

		noLookups.setVehiclesAvailable(true);
		assertTrue(noLookups.canCreate());
		assertTrue(noLookups.beginCreate());
		assertEquals(RentalViewState.Mode.CREATING, noLookups.mode());
	}

	@Test
	void editIsOnlyAllowedForPlannedRentals() {
		RentalViewState state = adminStateWithLookups();
		state.loadSucceeded(List.of(rental(1, RentalStatus.PLANNED)));

		state.select(rental(1, RentalStatus.ACTIVE));
		assertFalse(state.canEdit(), "an ACTIVE rental cannot be edited");

		state.select(rental(1, RentalStatus.PLANNED));
		assertTrue(state.canEdit());
		assertTrue(state.beginEdit());
		assertEquals(RentalViewState.Mode.EDITING, state.mode());

		state.cancelForm();
		assertEquals(RentalViewState.Mode.VIEWING, state.mode());
	}

	@Test
	void transitionsFollowTheBackendWorkflow() {
		RentalViewState state = adminStateWithLookups();

		state.select(rental(1, RentalStatus.PLANNED));
		assertTrue(state.canStart());
		assertTrue(state.canCancel());
		assertFalse(state.canComplete());

		state.select(rental(1, RentalStatus.ACTIVE));
		assertTrue(state.canComplete());
		assertFalse(state.canStart());
		assertFalse(state.canCancel());

		state.select(rental(1, RentalStatus.COMPLETED));
		assertFalse(state.canStart());
		assertFalse(state.canComplete());
		assertFalse(state.canCancel());
	}

	@Test
	void deleteIsAllowedOnlyForPlannedOrCancelledAndOnlyForAdmin() {
		RentalViewState admin = adminStateWithLookups();
		admin.select(rental(1, RentalStatus.PLANNED));
		assertTrue(admin.canDelete());
		admin.select(rental(1, RentalStatus.CANCELLED));
		assertTrue(admin.canDelete());
		admin.select(rental(1, RentalStatus.ACTIVE));
		assertFalse(admin.canDelete());
		admin.select(rental(1, RentalStatus.COMPLETED));
		assertFalse(admin.canDelete());

		RentalViewState user = RentalViewState.forRole(DesktopUserRole.USER);
		user.setCustomersAvailable(true);
		user.setVehiclesAvailable(true);
		user.select(rental(1, RentalStatus.PLANNED));
		assertFalse(user.canDelete(), "USER may never delete a rental");
		assertFalse(user.canEverDelete());
	}

	@Test
	void userIsNotReadOnlyButNullRoleIs() {
		RentalViewState user = RentalViewState.forRole(DesktopUserRole.USER);
		assertFalse(user.isReadOnly(), "USER can book, update and transition");

		RentalViewState anonymous = RentalViewState.forRole(null);
		assertTrue(anonymous.isReadOnly());
		anonymous.setCustomersAvailable(true);
		anonymous.setVehiclesAvailable(true);
		assertFalse(anonymous.canCreate());
		assertFalse(anonymous.beginCreate());
	}

	@Test
	void busyPreventsConcurrentOperationsAndGatesActions() {
		RentalViewState state = adminStateWithLookups();
		state.select(rental(1, RentalStatus.PLANNED));

		assertTrue(state.beginOperation());
		assertFalse(state.beginOperation(), "a second concurrent operation must be refused");
		assertFalse(state.canStart());
		assertFalse(state.canDelete());
		assertFalse(state.canCreate());

		state.endOperation();
		assertTrue(state.canStart());
	}

	@Test
	void clearSelectionDisablesSelectionDependentActions() {
		RentalViewState state = adminStateWithLookups();
		state.select(rental(1, RentalStatus.PLANNED));
		state.clearSelection();

		assertTrue(state.selected().isEmpty());
		assertFalse(state.canEdit());
		assertFalse(state.canStart());
		assertFalse(state.canDelete());
	}

	@Test
	void refreshIsBlockedWhileLoadingOrBusy() {
		RentalViewState state = adminStateWithLookups();
		assertTrue(state.canRefresh());
		state.beginLoading();
		assertFalse(state.canRefresh());
		state.loadFailed();
		assertTrue(state.canRefresh());
		state.beginOperation();
		assertFalse(state.canRefresh());
	}
}
