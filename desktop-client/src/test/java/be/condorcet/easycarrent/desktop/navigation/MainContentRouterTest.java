package be.condorcet.easycarrent.desktop.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Verifies the router's static wiring and its pure section-to-resource mapping
 * without opening a Stage. Selection behavior is covered by
 * {@link NavigationStateTest}; FXML loading, content replacement, and service
 * injection are covered by the resource tests and manual launch verification.
 */
class MainContentRouterTest {

	@Test
	void placeholderResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.PLACEHOLDER_FXML),
				"section-placeholder.fxml must be on the classpath at the router's path");
	}

	@Test
	void vehicleCategoriesResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.VEHICLE_CATEGORIES_FXML),
				"vehicle-categories-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void vehicleCategoriesSectionMapsToItsOwnView() {
		assertEquals(MainContentRouter.VEHICLE_CATEGORIES_FXML,
				MainContentRouter.resourceFor(MainSection.VEHICLE_CATEGORIES));
	}

	@Test
	void allOtherSectionsMapToThePlaceholder() {
		for (MainSection section : MainSection.values()) {
			if (section == MainSection.VEHICLE_CATEGORIES) {
				continue;
			}
			assertEquals(MainContentRouter.PLACEHOLDER_FXML,
					MainContentRouter.resourceFor(section),
					section + " must still use the placeholder view");
		}
	}
}
