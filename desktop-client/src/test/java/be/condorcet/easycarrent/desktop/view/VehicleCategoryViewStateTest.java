package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;

import java.util.List;

import org.junit.jupiter.api.Test;

class VehicleCategoryViewStateTest {

	private static final VehicleCategoryResponseDto COMPACT =
			new VehicleCategoryResponseDto(1L, "Compact", "Small cars");
	private static final VehicleCategoryResponseDto SUV =
			new VehicleCategoryResponseDto(2L, "SUV", "Sport utility");

	private VehicleCategoryViewState writable() {
		return new VehicleCategoryViewState(true);
	}

	private VehicleCategoryViewState readOnly() {
		return new VehicleCategoryViewState(false);
	}

	@Test
	void initialStateIsIdleAndNotLoaded() {
		VehicleCategoryViewState state = writable();

		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
		assertFalse(state.isBusy());
		assertFalse(state.isEmpty());
		assertFalse(state.isFormVisible());
		assertTrue(state.selected().isEmpty());
	}

	@Test
	void loadingThenNonEmptySuccess() {
		VehicleCategoryViewState state = writable();

		state.beginLoading();
		assertTrue(state.isLoading());

		state.loadSucceeded(List.of(COMPACT, SUV));
		assertFalse(state.isLoading());
		assertTrue(state.isLoaded());
		assertFalse(state.isEmpty());
		assertEquals(2, state.categories().size());
	}

	@Test
	void loadingThenEmptySuccess() {
		VehicleCategoryViewState state = writable();

		state.beginLoading();
		state.loadSucceeded(List.of());

		assertTrue(state.isLoaded());
		assertTrue(state.isEmpty());
	}

	@Test
	void loadFailureClearsLoading() {
		VehicleCategoryViewState state = writable();

		state.beginLoading();
		state.loadFailed();

		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
	}

	@Test
	void selectionEnablesEditAndDeleteForWritableRole() {
		VehicleCategoryViewState state = writable();
		state.loadSucceeded(List.of(COMPACT));

		assertFalse(state.canEdit());
		assertFalse(state.canDelete());

		state.select(COMPACT);

		assertTrue(state.canEdit());
		assertTrue(state.canDelete());
	}

	@Test
	void readOnlyRoleCannotCreateEditOrDelete() {
		VehicleCategoryViewState state = readOnly();
		state.loadSucceeded(List.of(COMPACT));
		state.select(COMPACT);

		assertTrue(state.isReadOnly());
		assertFalse(state.canCreate());
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		assertFalse(state.beginCreate());
		assertFalse(state.beginEdit());
	}

	@Test
	void createModeAndCancel() {
		VehicleCategoryViewState state = writable();

		assertTrue(state.beginCreate());
		assertEquals(VehicleCategoryViewState.Mode.CREATING, state.mode());
		assertTrue(state.isFormVisible());

		state.cancelForm();
		assertEquals(VehicleCategoryViewState.Mode.VIEWING, state.mode());
		assertFalse(state.isFormVisible());
	}

	@Test
	void editModeRequiresSelection() {
		VehicleCategoryViewState state = writable();
		state.loadSucceeded(List.of(COMPACT));

		assertFalse(state.beginEdit());

		state.select(COMPACT);
		assertTrue(state.beginEdit());
		assertEquals(VehicleCategoryViewState.Mode.EDITING, state.mode());
	}

	@Test
	void operationInProgressBlocksAnotherOperation() {
		VehicleCategoryViewState state = writable();

		assertTrue(state.beginOperation());
		assertTrue(state.isBusy());
		assertFalse(state.beginOperation());

		state.endOperation();
		assertFalse(state.isBusy());
		assertTrue(state.beginOperation());
	}

	@Test
	void busyDisablesWriteActions() {
		VehicleCategoryViewState state = writable();
		state.loadSucceeded(List.of(COMPACT));
		state.select(COMPACT);

		state.beginOperation();

		assertFalse(state.canCreate());
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		assertFalse(state.canRefresh());
	}

	@Test
	void reloadPreservesSelectionById() {
		VehicleCategoryViewState state = writable();
		state.loadSucceeded(List.of(COMPACT, SUV));
		state.select(SUV);

		state.loadSucceeded(List.of(
				new VehicleCategoryResponseDto(1L, "Compact", "x"),
				new VehicleCategoryResponseDto(2L, "SUV renamed", "y")));

		assertTrue(state.selected().isPresent());
		assertEquals(2L, state.selected().orElseThrow().id());
	}

	@Test
	void reloadDropsSelectionWhenCategoryRemoved() {
		VehicleCategoryViewState state = writable();
		state.loadSucceeded(List.of(COMPACT, SUV));
		state.select(SUV);

		state.loadSucceeded(List.of(COMPACT));

		assertTrue(state.selected().isEmpty());
	}
}
