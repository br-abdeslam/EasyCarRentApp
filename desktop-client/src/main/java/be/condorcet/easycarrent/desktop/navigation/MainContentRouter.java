package be.condorcet.easycarrent.desktop.navigation;

import be.condorcet.easycarrent.desktop.view.SectionPlaceholderController;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/**
 * Routes the central content area of the main shell between sections.
 *
 * <p>Given the shell's central {@link StackPane}, it loads the reusable
 * {@code section-placeholder.fxml}, configures its controller for the requested
 * {@link MainSection}, and replaces the host's content with the new node. It
 * tracks the current section through a {@link NavigationState} and defaults to
 * {@link MainSection#DASHBOARD}. It owns no Stage, loads no login or main view,
 * performs no authentication or HTTP, and holds no credentials. Routing is
 * expected to run on the JavaFX Application Thread.</p>
 */
public final class MainContentRouter {

	static final String PLACEHOLDER_FXML =
			"/be/condorcet/easycarrent/desktop/view/section-placeholder.fxml";

	private final StackPane contentHost;
	private final NavigationState state = new NavigationState();

	public MainContentRouter(StackPane contentHost) {
		this.contentHost = Objects.requireNonNull(contentHost, "contentHost must not be null");
	}

	/** Shows the default section ({@link MainSection#DASHBOARD}). */
	public void showDefault() {
		show(MainSection.DASHBOARD);
	}

	/**
	 * Replaces the central content with the placeholder configured for the given
	 * section and records it as current.
	 *
	 * @param section the section to display
	 */
	public void show(MainSection section) {
		state.select(section);
		FXMLLoader loader = new FXMLLoader(requireResource(PLACEHOLDER_FXML));
		Parent content = load(loader);
		SectionPlaceholderController controller = loader.getController();
		controller.setSection(section);
		contentHost.getChildren().setAll(content);
	}

	/** @return the currently displayed section */
	public MainSection currentSection() {
		return state.current();
	}

	private static Parent load(FXMLLoader loader) {
		try {
			return loader.load();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load section content: " + e.getMessage(), e);
		}
	}

	private static URL requireResource(String path) {
		return Objects.requireNonNull(MainContentRouter.class.getResource(path),
				() -> "Required content resource not found on the classpath: " + path);
	}
}
