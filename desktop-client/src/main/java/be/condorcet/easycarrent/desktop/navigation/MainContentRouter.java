package be.condorcet.easycarrent.desktop.navigation;

import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.view.SectionPlaceholderController;
import be.condorcet.easycarrent.desktop.view.VehicleCategoryController;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/**
 * Routes the central content area of the main shell between sections.
 *
 * <p>Given the shell's central {@link StackPane}, it loads the section's view and
 * replaces the host's content with the new node. Implemented sections load their
 * own view (Vehicle Categories loads {@code vehicle-categories-view.fxml});
 * unfinished sections load the reusable {@code section-placeholder.fxml}. It
 * tracks the current section through a {@link NavigationState} and defaults to
 * {@link MainSection#DASHBOARD}. It injects the shared domain service into a
 * loaded domain controller but performs no API operations itself. It owns no
 * Stage, loads no login or main view, performs no authentication, and holds no
 * credentials. Routing runs on the JavaFX Application Thread.</p>
 */
public final class MainContentRouter {

	static final String PLACEHOLDER_FXML =
			"/be/condorcet/easycarrent/desktop/view/section-placeholder.fxml";
	static final String VEHICLE_CATEGORIES_FXML =
			"/be/condorcet/easycarrent/desktop/view/vehicle-categories-view.fxml";

	private final StackPane contentHost;
	private final VehicleCategoryService vehicleCategoryService;
	private final SessionManager sessionManager;
	private final NavigationState state = new NavigationState();

	public MainContentRouter(StackPane contentHost,
			VehicleCategoryService vehicleCategoryService, SessionManager sessionManager) {
		this.contentHost = Objects.requireNonNull(contentHost, "contentHost must not be null");
		this.vehicleCategoryService =
				Objects.requireNonNull(vehicleCategoryService, "vehicleCategoryService must not be null");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
	}

	/** Shows the default section ({@link MainSection#DASHBOARD}). */
	public void showDefault() {
		show(MainSection.DASHBOARD);
	}

	/**
	 * Replaces the central content with the given section's view and records it as
	 * current.
	 *
	 * @param section the section to display
	 */
	public void show(MainSection section) {
		state.select(section);
		contentHost.getChildren().setAll(loadContent(section));
	}

	/** @return the currently displayed section */
	public MainSection currentSection() {
		return state.current();
	}

	/** @return the FXML resource path for the given section (pure mapping) */
	static String resourceFor(MainSection section) {
		return section == MainSection.VEHICLE_CATEGORIES ? VEHICLE_CATEGORIES_FXML : PLACEHOLDER_FXML;
	}

	private Parent loadContent(MainSection section) {
		FXMLLoader loader = new FXMLLoader(requireResource(resourceFor(section)));
		Parent content = load(loader);
		if (section == MainSection.VEHICLE_CATEGORIES) {
			VehicleCategoryController controller = loader.getController();
			// Dependencies are injected only after loading, so the initial API load
			// is started here rather than in the controller's FXML initialize().
			controller.init(vehicleCategoryService, sessionManager);
		} else {
			SectionPlaceholderController controller = loader.getController();
			controller.setSection(section);
		}
		return content;
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
