package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class VehicleViewStateTest {

	private static final VehicleResponseDto CAR = new VehicleResponseDto(1L, "R1", "B", "M",
			2022, "Blue", new BigDecimal("10.00"), 100L, VehicleStatus.AVAILABLE, 1L, "Compact");
	private static final VehicleResponseDto VAN = new VehicleResponseDto(2L, "R2", "B", "M",
			2021, "Red", new BigDecimal("20.00"), 200L, VehicleStatus.RENTED, 2L, "Van");

	private VehicleViewState writable() {
		VehicleViewState state = new VehicleViewState(true);
		state.setCategoriesAvailable(true);
		return state;
	}

	@Test
	void initialStateIsIdle() {
		VehicleViewState state = new VehicleViewState(true);
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
		assertFalse(state.isBusy());
		assertFalse(state.isEmpty());
		assertFalse(state.isFormVisible());
		assertTrue(state.selected().isEmpty());
	}

	@Test
	void loadingThenNonEmptyThenEmpty() {
		VehicleViewState state = writable();
		state.beginLoading();
		assertTrue(state.isLoading());
		state.loadSucceeded(List.of(CAR, VAN));
		assertTrue(state.isLoaded());
		assertFalse(state.isEmpty());
		assertEquals(2, state.vehicles().size());

		state.loadSucceeded(List.of());
		assertTrue(state.isEmpty());
	}

	@Test
	void loadFailureClearsLoading() {
		VehicleViewState state = writable();
		state.beginLoading();
		state.loadFailed();
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
	}

	@Test
	void selectionEnablesEditAndDelete() {
		VehicleViewState state = writable();
		state.loadSucceeded(List.of(CAR));
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		state.select(CAR);
		assertTrue(state.canEdit());
		assertTrue(state.canDelete());
		state.clearSelection();
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
	}

	@Test
	void readOnlyRoleCannotWrite() {
		VehicleViewState state = new VehicleViewState(false);
		state.setCategoriesAvailable(true);
		state.loadSucceeded(List.of(CAR));
		state.select(CAR);
		assertTrue(state.isReadOnly());
		assertFalse(state.canCreate());
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		assertFalse(state.beginCreate());
		assertFalse(state.beginEdit());
	}

	@Test
	void createAndEditRequireCategoriesAvailable() {
		VehicleViewState state = new VehicleViewState(true);
		state.setCategoriesAvailable(false);
		state.loadSucceeded(List.of(CAR));
		state.select(CAR);

		assertFalse(state.canCreate());
		assertFalse(state.canEdit());
		assertFalse(state.beginCreate());
		assertFalse(state.beginEdit());
		// Delete does not need a category.
		assertTrue(state.canDelete());

		state.setCategoriesAvailable(true);
		assertTrue(state.canCreate());
		assertTrue(state.canEdit());
	}

	@Test
	void createModeAndCancel() {
		VehicleViewState state = writable();
		assertTrue(state.beginCreate());
		assertEquals(VehicleViewState.Mode.CREATING, state.mode());
		assertTrue(state.isFormVisible());
		state.cancelForm();
		assertFalse(state.isFormVisible());
	}

	@Test
	void editModeRequiresSelection() {
		VehicleViewState state = writable();
		state.loadSucceeded(List.of(CAR));
		assertFalse(state.beginEdit());
		state.select(CAR);
		assertTrue(state.beginEdit());
		assertEquals(VehicleViewState.Mode.EDITING, state.mode());
	}

	@Test
	void operationInProgressBlocksAnotherAndDisablesWrites() {
		VehicleViewState state = writable();
		state.loadSucceeded(List.of(CAR));
		state.select(CAR);

		assertTrue(state.beginOperation());
		assertFalse(state.beginOperation());
		assertFalse(state.canCreate());
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		assertFalse(state.canRefresh());

		state.endOperation();
		assertTrue(state.canRefresh());
	}

	@Test
	void reloadPreservesSelectionByIdAndDropsRemoved() {
		VehicleViewState state = writable();
		state.loadSucceeded(List.of(CAR, VAN));
		state.select(VAN);
		state.loadSucceeded(List.of(CAR, VAN));
		assertEquals(2L, state.selected().orElseThrow().id());

		state.loadSucceeded(List.of(CAR));
		assertTrue(state.selected().isEmpty());
	}
}
