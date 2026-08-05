package be.condorcet.easycarrent.desktop.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
	void vehiclesResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.VEHICLES_FXML),
				"vehicles-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void customersResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.CUSTOMERS_FXML),
				"customers-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void rentalsResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.RENTALS_FXML),
				"rentals-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void paymentsResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.PAYMENTS_FXML),
				"payments-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void maintenanceResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.MAINTENANCE_FXML),
				"maintenance-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void dashboardResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.DASHBOARD_FXML),
				"dashboard-view.fxml must be on the classpath at the router's path");
	}

	@Test
	void everySectionMapsToItsOwnRealView() {
		assertEquals(MainContentRouter.DASHBOARD_FXML,
				MainContentRouter.resourceFor(MainSection.DASHBOARD));
		assertEquals(MainContentRouter.VEHICLE_CATEGORIES_FXML,
				MainContentRouter.resourceFor(MainSection.VEHICLE_CATEGORIES));
		assertEquals(MainContentRouter.VEHICLES_FXML,
				MainContentRouter.resourceFor(MainSection.VEHICLES));
		assertEquals(MainContentRouter.CUSTOMERS_FXML,
				MainContentRouter.resourceFor(MainSection.CUSTOMERS));
		assertEquals(MainContentRouter.RENTALS_FXML,
				MainContentRouter.resourceFor(MainSection.RENTALS));
		assertEquals(MainContentRouter.PAYMENTS_FXML,
				MainContentRouter.resourceFor(MainSection.PAYMENTS));
		assertEquals(MainContentRouter.MAINTENANCE_FXML,
				MainContentRouter.resourceFor(MainSection.MAINTENANCE));
	}

	@Test
	void noSectionMapsToThePlaceholder() {
		for (MainSection section : MainSection.values()) {
			assertNotEquals(MainContentRouter.PLACEHOLDER_FXML,
					MainContentRouter.resourceFor(section),
					section + " must load its own real view, not the placeholder");
		}
	}
}
