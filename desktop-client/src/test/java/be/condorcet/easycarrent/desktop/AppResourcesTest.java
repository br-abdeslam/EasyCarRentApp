package be.condorcet.easycarrent.desktop;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Focused, non-UI tests that verify the desktop client's FXML/CSS resources are
 * present and well-formed. These tests never initialize the JavaFX toolkit and
 * never open a Stage.
 */
class AppResourcesTest {

	private static final String MAIN_VIEW_FXML =
			"/be/condorcet/easycarrent/desktop/view/main-view.fxml";
	private static final String APP_STYLESHEET =
			"/be/condorcet/easycarrent/desktop/view/app.css";

	@Test
	void mainViewFxmlExistsOnClasspath() {
		assertNotNull(resource(MAIN_VIEW_FXML),
				"main-view.fxml must exist on the test classpath");
	}

	@Test
	void appStylesheetExistsOnClasspath() {
		assertNotNull(resource(APP_STYLESHEET),
				"app.css must exist on the test classpath");
	}

	@Test
	void mainViewFxmlIsWellFormedXml() throws Exception {
		try (InputStream in = stream(MAIN_VIEW_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// The FXML uses processing instructions for imports; do not resolve them.
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"main-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void mainViewFxmlDeclaresExpectedController() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(
				fxml.contains(
						"fx:controller=\"be.condorcet.easycarrent.desktop.view.MainViewController\""),
				"main-view.fxml must declare MainViewController as its controller");
	}

	@Test
	void mainViewFxmlContainsStatusLabelId() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"statusLabel\""),
				"main-view.fxml must contain the statusLabel fx:id");
	}

	@Test
	void appStylesheetContainsRequiredClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		assertTrue(css.contains(".app-root"), "app.css must define .app-root");
		assertTrue(css.contains(".app-title"), "app.css must define .app-title");
		assertTrue(css.contains(".app-status"), "app.css must define .app-status");
	}

	private static URL resource(String path) {
		return AppResourcesTest.class.getResource(path);
	}

	private static InputStream stream(String path) {
		InputStream in = AppResourcesTest.class.getResourceAsStream(path);
		assertNotNull(in, () -> "Resource not found on classpath: " + path);
		return in;
	}

	private static String readResource(String path) throws Exception {
		try (InputStream in = stream(path)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
