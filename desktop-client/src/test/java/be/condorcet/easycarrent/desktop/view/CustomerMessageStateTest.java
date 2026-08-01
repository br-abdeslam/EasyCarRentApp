package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.view.CustomerMessageState.StatusKind;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Placement rules for the two Customer message areas, verified without JavaFX.
 *
 * <p>These tests pin the behaviour that a manual visual test exposed: an Add/Edit
 * form error must live below the form and a general operation message above the
 * table, and the same failure must never appear in both regions at once. In
 * particular, a fresh local phone error must not leave a stale duplicate-email
 * error visible above the table.</p>
 */
class CustomerMessageStateTest {

	@Test
	void startsEmpty() {
		CustomerMessageState state = new CustomerMessageState();
		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertTrue(state.formMessages().isEmpty());
		assertEquals("", state.statusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void formErrorsGoBelowTheFormAndClearTheStatusArea() {
		CustomerMessageState state = new CustomerMessageState();
		state.formErrors(List.of("Phone: must match the accepted phone format"));

		assertTrue(state.hasFormMessages());
		assertEquals(List.of("Phone: must match the accepted phone format"), state.formMessages());
		assertFalse(state.hasStatusMessage(), "a form error must not also show above the table");
		assertEquals("", state.statusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void reportedRegressionDuplicateEmailThenLocalPhoneLeavesNoStaleMessageAboveTable() {
		CustomerMessageState state = new CustomerMessageState();

		// First Save: backend rejects the duplicate email; it belongs below the form.
		state.formErrors(List.of("A customer with that email already exists"));
		assertTrue(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());

		// Second Save is blocked locally by an invalid phone. The stale duplicate-email
		// message must be gone and only the phone error shown below the form.
		state.clearAll();
		state.formErrors(List.of("Phone: must match the accepted phone format"));

		assertEquals(List.of("Phone: must match the accepted phone format"), state.formMessages());
		assertFalse(state.hasStatusMessage(),
				"the old duplicate-email message must not remain above the table");
		assertFalse(state.formMessages().stream().anyMatch(line -> line.contains("email")),
				"the duplicate-email error must not survive the next Save");
	}

	@Test
	void aSuccessMessageGoesAboveTheTableAndClearsFormErrors() {
		CustomerMessageState state = new CustomerMessageState();
		state.formErrors(List.of("Email: must be a valid email address"));

		state.success("Customer created.");

		assertFalse(state.hasFormMessages(), "a success clears any pending form errors");
		assertTrue(state.hasStatusMessage());
		assertEquals("Customer created.", state.statusMessage());
		assertEquals(StatusKind.SUCCESS, state.statusKind());
	}

	@Test
	void aStatusErrorGoesAboveTheTableAndClearsFormErrors() {
		CustomerMessageState state = new CustomerMessageState();
		state.formErrors(List.of("Email: must be a valid email address"));

		state.statusError("This customer cannot be deleted because one or more rentals reference it.");

		assertFalse(state.hasFormMessages());
		assertTrue(state.hasStatusMessage());
		assertEquals(StatusKind.ERROR, state.statusKind());
	}

	@Test
	void aFormErrorClearsAPriorStatusMessage() {
		CustomerMessageState state = new CustomerMessageState();
		state.success("Customer created.");

		state.formErrors(List.of("Phone: must match the accepted phone format"));

		assertTrue(state.hasFormMessages());
		assertFalse(state.hasStatusMessage(), "opening a form error clears the earlier success line");
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void clearAllResetsBothAreas() {
		CustomerMessageState state = new CustomerMessageState();
		state.formErrors(List.of("Email: must be a valid email address"));
		state.clearAll();

		assertFalse(state.hasFormMessages());
		assertFalse(state.hasStatusMessage());
		assertTrue(state.formMessages().isEmpty());
		assertEquals("", state.statusMessage());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void neverShowsFormAndStatusErrorsAtTheSameTime() {
		CustomerMessageState state = new CustomerMessageState();

		state.statusError("Load failed.");
		assertTrue(state.hasStatusMessage() && !state.hasFormMessages());

		state.formErrors(List.of("First name: is required"));
		assertTrue(state.hasFormMessages() && !state.hasStatusMessage());
	}

	@Test
	void formMessagesAreDefensivelyCopied() {
		CustomerMessageState state = new CustomerMessageState();
		java.util.List<String> source = new java.util.ArrayList<>();
		source.add("First name: is required");
		state.formErrors(source);

		source.add("Email: must be a valid email address");

		assertEquals(1, state.formMessages().size(),
				"mutating the caller's list must not change the stored messages");
	}
}
