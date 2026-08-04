package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.view.MaintenanceMessageState.StatusKind;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Placement rules for the two Maintenance message areas, verified without JavaFX:
 * an Add form error lives below the editor, a general operation message lives above
 * the table, and the same failure is never shown in both places at once.
 */
class MaintenanceMessageStateTest {

	@Test
	void startsEmpty() {
		MaintenanceMessageState state = new MaintenanceMessageState();
		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formErrorsGoBelowTheFormAndClearTheStatusArea() {
		MaintenanceMessageState state = new MaintenanceMessageState();
		state.statusError("Load failed.");
		state.formErrors(List.of("Vehicle is required."));

		assertEquals(List.of("Vehicle is required."), state.formMessages());
		assertFalse(state.hasStatusMessage(), "a form error must not also show above the table");
	}

	@Test
	void overlapConflictThenLocalErrorLeavesNoStaleMessageAboveTable() {
		MaintenanceMessageState state = new MaintenanceMessageState();

		// First Save: backend overlap conflict, shown below the form.
		state.formErrors(List.of(
				"Vehicle 4 already has maintenance scheduled overlapping 2027-09-01 to 2027-09-03"));
		assertTrue(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());

		// Second Save blocked locally by a missing cost: only that shows, nothing stale.
		state.clearAll();
		state.formErrors(List.of("Cost is required."));

		assertEquals(List.of("Cost is required."), state.formMessages());
		assertFalse(state.hasStatusMessage());
		assertFalse(state.formMessages().stream().anyMatch(line -> line.contains("overlapping")),
				"the old overlap message must not survive the next Save");
	}

	@Test
	void successGoesAboveTheTableAndClearsFormErrors() {
		MaintenanceMessageState state = new MaintenanceMessageState();
		state.formErrors(List.of("Description is required."));

		state.success("Maintenance record created.");

		assertFalse(state.hasFormMessages());
		assertEquals("Maintenance record created.", state.statusMessage());
		assertEquals(StatusKind.SUCCESS, state.statusKind());
	}

	@Test
	void deleteConflictStaysAboveTheTable() {
		MaintenanceMessageState state = new MaintenanceMessageState();
		state.formErrors(List.of("Vehicle is required."));

		state.statusError("Maintenance record 6 is IN_PROGRESS and can only be deleted while PLANNED");

		assertFalse(state.hasFormMessages());
		assertEquals(StatusKind.ERROR, state.statusKind());
	}

	@Test
	void neverShowsFormAndStatusErrorsAtTheSameTime() {
		MaintenanceMessageState state = new MaintenanceMessageState();

		state.statusError("A transition conflict occurred.");
		assertTrue(state.hasStatusMessage() && !state.hasFormMessages());

		state.formErrors(List.of("End date must be on or after the start date."));
		assertTrue(state.hasFormMessages() && !state.hasStatusMessage());
	}

	@Test
	void clearAllResetsBothAreas() {
		MaintenanceMessageState state = new MaintenanceMessageState();
		state.formErrors(List.of("Vehicle is required."));
		state.clearAll();

		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formMessagesAreDefensivelyCopied() {
		MaintenanceMessageState state = new MaintenanceMessageState();
		java.util.List<String> source = new java.util.ArrayList<>();
		source.add("Vehicle is required.");
		state.formErrors(source);

		source.add("Cost is required.");

		assertEquals(1, state.formMessages().size(),
				"mutating the caller's list must not change the stored messages");
	}
}
