package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.view.RentalMessageState.StatusKind;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Placement rules for the two Rental message areas, verified without JavaFX: an
 * Add/Edit form error lives below the editor, a general operation message lives
 * above the table, and the same failure is never shown in both places at once.
 */
class RentalMessageStateTest {

	@Test
	void startsEmpty() {
		RentalMessageState state = new RentalMessageState();
		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formErrorsGoBelowTheFormAndClearTheStatusArea() {
		RentalMessageState state = new RentalMessageState();
		state.statusError("Load failed.");
		state.formErrors(List.of("Customer is required."));

		assertEquals(List.of("Customer is required."), state.formMessages());
		assertFalse(state.hasStatusMessage(), "a form error must not also show above the table");
	}

	@Test
	void overlapConflictThenLocalErrorLeavesNoStaleMessageAboveTable() {
		RentalMessageState state = new RentalMessageState();

		// First Save: backend overlap conflict, shown below the form.
		state.formErrors(List.of(
				"The vehicle already has a planned or active rental overlapping the requested period"));
		assertTrue(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());

		// Second Save blocked locally by a missing customer: only that shows, nothing stale.
		state.clearAll();
		state.formErrors(List.of("Customer is required."));

		assertEquals(List.of("Customer is required."), state.formMessages());
		assertFalse(state.hasStatusMessage());
		assertFalse(state.formMessages().stream().anyMatch(line -> line.contains("overlapping")),
				"the old overlap message must not survive the next Save");
	}

	@Test
	void successGoesAboveTheTableAndClearsFormErrors() {
		RentalMessageState state = new RentalMessageState();
		state.formErrors(List.of("Vehicle is required."));

		state.success("Rental created.");

		assertFalse(state.hasFormMessages());
		assertEquals("Rental created.", state.statusMessage());
		assertEquals(StatusKind.SUCCESS, state.statusKind());
	}

	@Test
	void statusErrorGoesAboveTheTableAndClearsFormErrors() {
		RentalMessageState state = new RentalMessageState();
		state.formErrors(List.of("Customer is required."));

		state.statusError("An active rental cannot be deleted");

		assertFalse(state.hasFormMessages());
		assertEquals(StatusKind.ERROR, state.statusKind());
	}

	@Test
	void neverShowsFormAndStatusErrorsAtTheSameTime() {
		RentalMessageState state = new RentalMessageState();

		state.statusError("A transition conflict occurred.");
		assertTrue(state.hasStatusMessage() && !state.hasFormMessages());

		state.formErrors(List.of("End date must be after the start date."));
		assertTrue(state.hasFormMessages() && !state.hasStatusMessage());
	}

	@Test
	void clearAllResetsBothAreas() {
		RentalMessageState state = new RentalMessageState();
		state.formErrors(List.of("Vehicle is required."));
		state.clearAll();

		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formMessagesAreDefensivelyCopied() {
		RentalMessageState state = new RentalMessageState();
		java.util.List<String> source = new java.util.ArrayList<>();
		source.add("Customer is required.");
		state.formErrors(source);

		source.add("Vehicle is required.");

		assertEquals(1, state.formMessages().size(),
				"mutating the caller's list must not change the stored messages");
	}
}
