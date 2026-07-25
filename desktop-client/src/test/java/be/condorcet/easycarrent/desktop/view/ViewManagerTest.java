package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Verifies the view resources the {@link ViewManager} depends on are present on
 * the classpath. Does not construct a {@link ViewManager} or start a Stage.
 */
class ViewManagerTest {

	@Test
	void loginViewResourceExists() {
		assertNotNull(ViewManager.class.getResource(ViewManager.LOGIN_FXML),
				"login-view.fxml must be on the classpath");
	}

	@Test
	void mainViewResourceExists() {
		assertNotNull(ViewManager.class.getResource(ViewManager.MAIN_FXML),
				"main-view.fxml must be on the classpath");
	}

	@Test
	void stylesheetResourceExists() {
		assertNotNull(ViewManager.class.getResource(ViewManager.APP_CSS),
				"app.css must be on the classpath");
	}
}
