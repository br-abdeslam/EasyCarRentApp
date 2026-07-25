package be.condorcet.easycarrent.desktop.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NavigationStateTest {

	@Test
	void defaultsToDashboard() {
		assertEquals(MainSection.DASHBOARD, new NavigationState().current());
	}

	@Test
	void selectingVehiclesRecordsVehiclesAsCurrent() {
		NavigationState state = new NavigationState();

		state.select(MainSection.VEHICLES);

		assertEquals(MainSection.VEHICLES, state.current());
	}

	@Test
	void selectingEachSectionUpdatesCurrent() {
		NavigationState state = new NavigationState();

		for (MainSection section : MainSection.values()) {
			state.select(section);
			assertEquals(section, state.current());
		}
	}

	@Test
	void rejectsNullSection() {
		assertThrows(NullPointerException.class, () -> new NavigationState().select(null));
	}

	@Test
	void retainsOnlyTheLatestSectionNotAHistory() {
		NavigationState state = new NavigationState();

		state.select(MainSection.CUSTOMERS);
		state.select(MainSection.PAYMENTS);

		// Only the most recent selection is kept; there is no history to inspect.
		assertEquals(MainSection.PAYMENTS, state.current());
	}
}
