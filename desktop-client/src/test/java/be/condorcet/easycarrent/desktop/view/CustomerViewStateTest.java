package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class CustomerViewStateTest {

	private static final CustomerResponseDto ANNA = new CustomerResponseDto(1L, "Anna", "Example",
			"anna@example.invalid", "000001", "addr", "L1", LocalDate.of(2030, 1, 1));
	private static final CustomerResponseDto BOB = new CustomerResponseDto(2L, "Bob", "Example",
			"bob@example.invalid", "000002", "addr", "L2", LocalDate.of(2031, 1, 1));

	private CustomerViewState writable() {
		return new CustomerViewState(true);
	}

	@Test
	void initialStateIsIdle() {
		CustomerViewState state = writable();
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
		assertFalse(state.isBusy());
		assertFalse(state.isEmpty());
		assertFalse(state.isFormVisible());
		assertTrue(state.selected().isEmpty());
	}

	@Test
	void loadingThenNonEmptyThenEmpty() {
		CustomerViewState state = writable();
		state.beginLoading();
		assertTrue(state.isLoading());
		state.loadSucceeded(List.of(ANNA, BOB));
		assertTrue(state.isLoaded());
		assertFalse(state.isEmpty());
		assertEquals(2, state.customers().size());

		state.loadSucceeded(List.of());
		assertTrue(state.isEmpty());
	}

	@Test
	void loadFailureClearsLoading() {
		CustomerViewState state = writable();
		state.beginLoading();
		state.loadFailed();
		assertFalse(state.isLoading());
		assertFalse(state.isLoaded());
	}

	@Test
	void selectionEnablesEditAndDelete() {
		CustomerViewState state = writable();
		state.loadSucceeded(List.of(ANNA));
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		state.select(ANNA);
		assertTrue(state.canEdit());
		assertTrue(state.canDelete());
		state.clearSelection();
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
	}

	@Test
	void readOnlyRoleCannotWrite() {
		CustomerViewState state = new CustomerViewState(false);
		state.loadSucceeded(List.of(ANNA));
		state.select(ANNA);
		assertTrue(state.isReadOnly());
		assertFalse(state.canCreate());
		assertFalse(state.canEdit());
		assertFalse(state.canDelete());
		assertFalse(state.beginCreate());
		assertFalse(state.beginEdit());
	}

	@Test
	void createModeAndCancel() {
		CustomerViewState state = writable();
		assertTrue(state.beginCreate());
		assertEquals(CustomerViewState.Mode.CREATING, state.mode());
		assertTrue(state.isFormVisible());
		state.cancelForm();
		assertFalse(state.isFormVisible());
	}

	@Test
	void editModeRequiresSelection() {
		CustomerViewState state = writable();
		state.loadSucceeded(List.of(ANNA));
		assertFalse(state.beginEdit());
		state.select(ANNA);
		assertTrue(state.beginEdit());
		assertEquals(CustomerViewState.Mode.EDITING, state.mode());
	}

	@Test
	void operationInProgressBlocksAnotherAndDisablesWrites() {
		CustomerViewState state = writable();
		state.loadSucceeded(List.of(ANNA));
		state.select(ANNA);

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
		CustomerViewState state = writable();
		state.loadSucceeded(List.of(ANNA, BOB));
		state.select(BOB);
		state.loadSucceeded(List.of(ANNA, BOB));
		assertEquals(2L, state.selected().orElseThrow().id());

		state.loadSucceeded(List.of(ANNA));
		assertTrue(state.selected().isEmpty());
	}
}
