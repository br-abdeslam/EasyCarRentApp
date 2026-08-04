package be.condorcet.easycarrent.desktop.navigation;

import be.condorcet.easycarrent.desktop.service.CustomerService;
import be.condorcet.easycarrent.desktop.service.RentalService;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.view.CustomerController;
import be.condorcet.easycarrent.desktop.view.RentalController;
import be.condorcet.easycarrent.desktop.view.SectionPlaceholderController;
import be.condorcet.easycarrent.desktop.view.VehicleCategoryController;
import be.condorcet.easycarrent.desktop.view.VehicleController;

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
 * own view (Vehicle Categories, Vehicles, and Customers each load their FXML);
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
	static final String VEHICLES_FXML =
			"/be/condorcet/easycarrent/desktop/view/vehicles-view.fxml";
	static final String CUSTOMERS_FXML =
			"/be/condorcet/easycarrent/desktop/view/customers-view.fxml";
	static final String RENTALS_FXML =
			"/be/condorcet/easycarrent/desktop/view/rentals-view.fxml";

	private final StackPane contentHost;
	private final VehicleCategoryService vehicleCategoryService;
	private final VehicleService vehicleService;
	private final CustomerService customerService;
	private final RentalService rentalService;
	private final SessionManager sessionManager;
	private final NavigationState state = new NavigationState();

	public MainContentRouter(StackPane contentHost,
			VehicleCategoryService vehicleCategoryService, VehicleService vehicleService,
			CustomerService customerService, RentalService rentalService,
			SessionManager sessionManager) {
		this.contentHost = Objects.requireNonNull(contentHost, "contentHost must not be null");
		this.vehicleCategoryService =
				Objects.requireNonNull(vehicleCategoryService, "vehicleCategoryService must not be null");
		this.vehicleService =
				Objects.requireNonNull(vehicleService, "vehicleService must not be null");
		this.customerService =
				Objects.requireNonNull(customerService, "customerService must not be null");
		this.rentalService =
				Objects.requireNonNull(rentalService, "rentalService must not be null");
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
		return switch (section) {
			case VEHICLE_CATEGORIES -> VEHICLE_CATEGORIES_FXML;
			case VEHICLES -> VEHICLES_FXML;
			case CUSTOMERS -> CUSTOMERS_FXML;
			case RENTALS -> RENTALS_FXML;
			default -> PLACEHOLDER_FXML;
		};
	}

	private Parent loadContent(MainSection section) {
		FXMLLoader loader = new FXMLLoader(requireResource(resourceFor(section)));
		Parent content = load(loader);
		// Dependencies are injected only after loading, so any initial API load is
		// started by the controller's init() here, not in its FXML initialize().
		switch (section) {
			case VEHICLE_CATEGORIES -> {
				VehicleCategoryController controller = loader.getController();
				controller.init(vehicleCategoryService, sessionManager);
			}
			case VEHICLES -> {
				VehicleController controller = loader.getController();
				controller.init(vehicleService, vehicleCategoryService, sessionManager);
			}
			case CUSTOMERS -> {
				CustomerController controller = loader.getController();
				controller.init(customerService, sessionManager);
			}
			case RENTALS -> {
				RentalController controller = loader.getController();
				controller.init(rentalService, customerService, vehicleService, sessionManager);
			}
			default -> {
				SectionPlaceholderController controller = loader.getController();
				controller.setSection(section);
			}
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
