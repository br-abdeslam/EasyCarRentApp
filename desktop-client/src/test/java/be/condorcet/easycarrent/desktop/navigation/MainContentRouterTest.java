package be.condorcet.easycarrent.desktop.navigation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Verifies the router's static wiring without opening a Stage. The router's
 * selection behavior is covered by {@link NavigationStateTest}; FXML loading and
 * content replacement are covered by the resource tests and manual launch
 * verification.
 */
class MainContentRouterTest {

	@Test
	void placeholderResourcePathIsCorrect() {
		assertNotNull(MainContentRouter.class.getResource(MainContentRouter.PLACEHOLDER_FXML),
				"section-placeholder.fxml must be on the classpath at the router's path");
	}
}
