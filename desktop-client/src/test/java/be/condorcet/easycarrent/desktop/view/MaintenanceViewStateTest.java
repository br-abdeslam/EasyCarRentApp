package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class MaintenanceViewStateTest {

	private static MaintenanceResponseDto record(long id, MaintenanceStatus status) {
		return new MaintenanceResponseDto(id, 4L, "Brake inspection",
				LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 3), new BigDecimal("180.00"), status);
	}

	private static MaintenanceViewState adminStateWithVehicles() {
		MaintenanceViewState state = MaintenanceViewState.forRole(DesktopUserRole.ADMIN);
		state.setVehiclesAvailable(true);
		return state;
	}

	@Test
	void startsIdleAndNotLoaded() {
		MaintenanceViewState state = adminStateWithVehicles();
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
		assertFalse(state.isEmpty());
		assertFalse(state.isBusy());
		assertFalse(state.isFormVisible());
		assertTrue(state.selected().isEmpty());
	}

	@Test
	void loadingThenNonEmptyThenEmpty() {
		MaintenanceViewState state = adminStateWithVehicles();
		state.beginLoading();
		assertTrue(state.isLoading());

		state.loadSucceeded(List.of(record(1, MaintenanceStatus.PLANNED)));
		assertTrue(state.isLoaded());
		assertFalse(state.isEmpty());
		assertEquals(1, state.records().size());

		state.loadSucceeded(List.of());
		assertTrue(state.isEmpty());
	}

	@Test
	void selectionRestoredByIdAcrossReload() {
		MaintenanceViewState state = adminStateWithVehicles();
		state.loadSucceeded(List.of(record(1, MaintenanceStatus.PLANNED),
				record(2, MaintenanceStatus.IN_PROGRESS)));
		state.select(record(2, MaintenanceStatus.IN_PROGRESS));

		state.loadSucceeded(List.of(record(2, MaintenanceStatus.IN_PROGRESS),
				record(3, MaintenanceStatus.PLANNED)));

		assertTrue(state.selected().isPresent());
		assertEquals(2L, state.selected().get().id());
	}

	@Test
	void createRequiresAMaintainableVehicle() {
		MaintenanceViewState noVehicles = MaintenanceViewState.forRole(DesktopUserRole.ADMIN);
		assertFalse(noVehicles.canCreate(), "no maintainable vehicle means scheduling is disabled");

		noVehicles.setVehiclesAvailable(true);
		assertTrue(noVehicles.canCreate());
		assertTrue(noVehicles.beginCreate());
		assertEquals(MaintenanceViewState.Mode.CREATING, noVehicles.mode());

		noVehicles.cancelForm();
		assertEquals(MaintenanceViewState.Mode.VIEWING, noVehicles.mode());
	}

	@Test
	void transitionsAndDeleteFollowTheBackendWorkflow() {
		MaintenanceViewState state = adminStateWithVehicles();

		state.select(record(1, MaintenanceStatus.PLANNED));
		assertTrue(state.canStart());
		assertTrue(state.canDelete());
		assertFalse(state.canComplete());

		state.select(record(1, MaintenanceStatus.IN_PROGRESS));
		assertTrue(state.canComplete());
		assertFalse(state.canStart());
		assertFalse(state.canDelete(), "an in-progress record cannot be deleted");

		state.select(record(1, MaintenanceStatus.COMPLETED));
		assertFalse(state.canStart());
		assertFalse(state.canComplete());
		assertFalse(state.canDelete(), "a completed record cannot be deleted");
	}

	@Test
	void userIsReadOnlyAndAdminIsNot() {
		MaintenanceViewState user = MaintenanceViewState.forRole(DesktopUserRole.USER);
		user.setVehiclesAvailable(true);
		user.select(record(1, MaintenanceStatus.PLANNED));
		assertTrue(user.isReadOnly(), "USER may not write maintenance");
		assertFalse(user.canCreate());
		assertFalse(user.canStart());
		assertFalse(user.canDelete());
		assertFalse(user.beginCreate());

		assertFalse(adminStateWithVehicles().isReadOnly());

		MaintenanceViewState anonymous = MaintenanceViewState.forRole(null);
		assertTrue(anonymous.isReadOnly());
	}

	@Test
	void busyPreventsConcurrentOperationsAndGatesActions() {
		MaintenanceViewState state = adminStateWithVehicles();
		state.select(record(1, MaintenanceStatus.PLANNED));

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
		MaintenanceViewState state = adminStateWithVehicles();
		state.select(record(1, MaintenanceStatus.PLANNED));
		state.clearSelection();

		assertTrue(state.selected().isEmpty());
		assertFalse(state.canStart());
		assertFalse(state.canDelete());
	}

	@Test
	void refreshIsBlockedWhileLoadingOrBusy() {
		MaintenanceViewState state = adminStateWithVehicles();
		assertTrue(state.canRefresh());
		state.beginLoading();
		assertFalse(state.canRefresh());
		state.loadFailed();
		assertTrue(state.canRefresh());
		state.beginOperation();
		assertFalse(state.canRefresh());
	}
}
