package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.view.PaymentMessageState.StatusKind;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Placement rules for the two Payment message areas, verified without JavaFX: an
 * Add form error lives below the editor, a general operation message lives above
 * the table, and the same failure is never shown in both places at once.
 */
class PaymentMessageStateTest {

	@Test
	void startsEmpty() {
		PaymentMessageState state = new PaymentMessageState();
		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formErrorsGoBelowTheFormAndClearTheStatusArea() {
		PaymentMessageState state = new PaymentMessageState();
		state.statusError("Load failed.");
		state.formErrors(List.of("Rental is required."));

		assertEquals(List.of("Rental is required."), state.formMessages());
		assertFalse(state.hasStatusMessage(), "a form error must not also show above the table");
	}

	@Test
	void duplicateConflictThenLocalErrorLeavesNoStaleMessageAboveTable() {
		PaymentMessageState state = new PaymentMessageState();

		// First Save: backend duplicate-payment conflict, shown below the form.
		state.formErrors(List.of("A payment already exists for rental 4"));
		assertTrue(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());

		// Second Save blocked locally by a missing method: only that shows, nothing stale.
		state.clearAll();
		state.formErrors(List.of("Payment method is required."));

		assertEquals(List.of("Payment method is required."), state.formMessages());
		assertFalse(state.hasStatusMessage());
		assertFalse(state.formMessages().stream().anyMatch(line -> line.contains("already exists")),
				"the old duplicate message must not survive the next Save");
	}

	@Test
	void overpaymentStyleConflictStaysBelowTheForm() {
		PaymentMessageState state = new PaymentMessageState();
		state.formErrors(List.of(
				"A payment can only be created for an ACTIVE or COMPLETED rental but rental 4 is PLANNED"));
		assertTrue(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
	}

	@Test
	void successGoesAboveTheTableAndClearsFormErrors() {
		PaymentMessageState state = new PaymentMessageState();
		state.formErrors(List.of("Payment method is required."));

		state.success("Payment created.");

		assertFalse(state.hasFormMessages());
		assertEquals("Payment created.", state.statusMessage());
		assertEquals(StatusKind.SUCCESS, state.statusKind());
	}

	@Test
	void deleteConflictStaysAboveTheTable() {
		PaymentMessageState state = new PaymentMessageState();
		state.formErrors(List.of("Rental is required."));

		state.statusError("A payment that is PAID cannot be deleted");

		assertFalse(state.hasFormMessages());
		assertEquals(StatusKind.ERROR, state.statusKind());
	}

	@Test
	void neverShowsFormAndStatusErrorsAtTheSameTime() {
		PaymentMessageState state = new PaymentMessageState();

		state.statusError("A transition conflict occurred.");
		assertTrue(state.hasStatusMessage() && !state.hasFormMessages());

		state.formErrors(List.of("Rental is required."));
		assertTrue(state.hasFormMessages() && !state.hasStatusMessage());
	}

	@Test
	void clearAllResetsBothAreas() {
		PaymentMessageState state = new PaymentMessageState();
		state.formErrors(List.of("Rental is required."));
		state.clearAll();

		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formMessagesAreDefensivelyCopied() {
		PaymentMessageState state = new PaymentMessageState();
		java.util.List<String> source = new java.util.ArrayList<>();
		source.add("Rental is required.");
		state.formErrors(source);

		source.add("Payment method is required.");

		assertEquals(1, state.formMessages().size(),
				"mutating the caller's list must not change the stored messages");
	}
}
