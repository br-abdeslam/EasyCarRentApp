package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dashboard.DashboardSnapshot;
import be.condorcet.easycarrent.desktop.view.DashboardViewState.StatusKind;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DashboardViewStateTest {

	private static DashboardSnapshot snapshot(LocalDateTime at) {
		return new DashboardSnapshot.Builder().calculatedAt(at)
				.categories(true, 0L).vehicles(true, 0L, java.util.Map.of())
				.customers(true, 0L).rentals(true, 0L, java.util.Map.of())
				.payments(true, 0L, java.util.Map.of(), java.math.BigDecimal.ZERO,
						java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)
				.maintenance(true, 0L, java.util.Map.of())
				.build();
	}

	private static final LocalDateTime T1 = LocalDateTime.of(2026, 8, 5, 9, 0);
	private static final LocalDateTime T2 = LocalDateTime.of(2026, 8, 5, 10, 0);

	@Test
	void startsIdleWithoutSnapshot() {
		DashboardViewState state = new DashboardViewState();
		assertFalse(state.isLoading());
		assertFalse(state.hasSnapshot());
		assertEquals(StatusKind.NONE, state.statusKind());
		assertTrue(state.canRefresh());
	}

	@Test
	void beginLoadPreventsDuplicateSimultaneousRefresh() {
		DashboardViewState state = new DashboardViewState();
		assertTrue(state.beginLoad());
		assertTrue(state.isLoading());
		assertFalse(state.canRefresh());
		assertFalse(state.beginLoad(), "a second concurrent load must be refused");
	}

	@Test
	void firstFullSuccessAdoptsSnapshotWithNoMessage() {
		DashboardViewState state = new DashboardViewState();
		state.beginLoad();
		DashboardSnapshot snapshot = snapshot(T1);
		state.loadSucceeded(snapshot, true, true);

		assertFalse(state.isLoading());
		assertSame(snapshot, state.snapshot());
		assertEquals(StatusKind.NONE, state.statusKind());
		assertFalse(state.hasStatusMessage());
	}

	@Test
	void firstPartialSuccessAdoptsSnapshotWithPartialNotice() {
		DashboardViewState state = new DashboardViewState();
		state.beginLoad();
		DashboardSnapshot snapshot = snapshot(T1);
		state.loadSucceeded(snapshot, false, true);

		assertSame(snapshot, state.snapshot());
		assertEquals(StatusKind.PARTIAL, state.statusKind());
		assertEquals(DashboardViewState.PARTIAL_MESSAGE, state.statusMessage());
	}

	@Test
	void firstFullFailureShowsUnavailableWithoutSnapshot() {
		DashboardViewState state = new DashboardViewState();
		state.beginLoad();
		state.loadSucceeded(snapshot(T1), false, false);

		assertFalse(state.hasSnapshot());
		assertEquals(StatusKind.UNAVAILABLE, state.statusKind());
		assertTrue(state.canRefresh(), "Refresh stays enabled after a full failure");
	}

	@Test
	void refreshFullSuccessReplacesSnapshotAndClearsErrors() {
		DashboardViewState state = new DashboardViewState();
		state.beginLoad();
		state.loadSucceeded(snapshot(T1), false, true); // partial first load
		assertEquals(StatusKind.PARTIAL, state.statusKind());

		state.beginLoad();
		DashboardSnapshot fresh = snapshot(T2);
		state.loadSucceeded(fresh, true, true);

		assertSame(fresh, state.snapshot());
		assertEquals(T2, state.snapshot().calculatedAt());
		assertEquals(StatusKind.NONE, state.statusKind());
	}

	@Test
	void refreshPartialFailurePreservesPreviousSnapshotAndTimestamp() {
		DashboardViewState state = new DashboardViewState();
		state.beginLoad();
		DashboardSnapshot original = snapshot(T1);
		state.loadSucceeded(original, true, true); // full first load

		state.beginLoad();
		state.loadSucceeded(snapshot(T2), false, true); // partial refresh

		assertSame(original, state.snapshot(), "previous snapshot is preserved");
		assertEquals(T1, state.snapshot().calculatedAt(), "last-updated time is unchanged");
		assertEquals(StatusKind.REFRESH_INCOMPLETE, state.statusKind());
	}

	@Test
	void refreshFullFailurePreservesPreviousSnapshot() {
		DashboardViewState state = new DashboardViewState();
		state.beginLoad();
		DashboardSnapshot original = snapshot(T1);
		state.loadSucceeded(original, true, true);

		state.beginLoad();
		state.loadSucceeded(snapshot(T2), false, false);

		assertSame(original, state.snapshot());
		assertEquals(StatusKind.REFRESH_INCOMPLETE, state.statusKind());
	}

	@Test
	void unexpectedWholeLoadFailurePreservesSnapshotOrShowsUnavailable() {
		DashboardViewState withData = new DashboardViewState();
		withData.beginLoad();
		DashboardSnapshot original = snapshot(T1);
		withData.loadSucceeded(original, true, true);
		withData.beginLoad();
		withData.loadFailed();
		assertSame(original, withData.snapshot());
		assertEquals(StatusKind.REFRESH_INCOMPLETE, withData.statusKind());

		DashboardViewState firstTime = new DashboardViewState();
		firstTime.beginLoad();
		firstTime.loadFailed();
		assertFalse(firstTime.hasSnapshot());
		assertEquals(StatusKind.UNAVAILABLE, firstTime.statusKind());
	}
}
