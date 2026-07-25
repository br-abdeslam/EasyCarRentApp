package be.condorcet.easycarrent.desktop.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Verifies the view resources the {@link ViewManager} depends on and that its
 * responsibility stays limited to login/main switching. Does not construct a
 * {@link ViewManager} or start a Stage.
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

	@Test
	void managesOnlyLoginAndMainResources() {
		boolean referencesLogin = false;
		boolean referencesMain = false;
		for (Field field : ViewManager.class.getDeclaredFields()) {
			if (field.getType() == String.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				field.setAccessible(true);
				try {
					Object value = field.get(null);
					if ("login-view.fxml".equals(fileName(value))) {
						referencesLogin = true;
					}
					if ("main-view.fxml".equals(fileName(value))) {
						referencesMain = true;
					}
					assertTrue(!isDomainOrPlaceholderResource(value),
							"ViewManager must not embed section or domain view resources");
				} catch (IllegalAccessException e) {
					throw new AssertionError(e);
				}
			}
		}
		assertTrue(referencesLogin, "ViewManager must reference the login view");
		assertTrue(referencesMain, "ViewManager must reference the main view");
	}

	@Test
	void doesNotEmbedContentRoutingApi() {
		boolean hasRoutingMethod = Arrays.stream(ViewManager.class.getDeclaredMethods())
				.anyMatch(method -> {
					String name = method.getName().toLowerCase();
					return name.contains("section") || name.contains("route") || name.contains("content");
				});
		assertTrue(!hasRoutingMethod,
				"content routing must live in MainContentRouter, not ViewManager");
	}

	private static String fileName(Object value) {
		if (!(value instanceof String path)) {
			return "";
		}
		int slash = path.lastIndexOf('/');
		return slash >= 0 ? path.substring(slash + 1) : path;
	}

	private static boolean isDomainOrPlaceholderResource(Object value) {
		String name = fileName(value);
		return "section-placeholder.fxml".equals(name)
				|| "vehicle-categories-view.fxml".equals(name)
				|| "vehicles-view.fxml".equals(name);
	}
}
