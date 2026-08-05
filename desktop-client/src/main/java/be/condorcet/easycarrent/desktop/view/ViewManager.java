package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.service.AuthenticationService;
import be.condorcet.easycarrent.desktop.service.CustomerService;
import be.condorcet.easycarrent.desktop.service.DashboardService;
import be.condorcet.easycarrent.desktop.service.MaintenanceService;
import be.condorcet.easycarrent.desktop.service.PaymentService;
import be.condorcet.easycarrent.desktop.service.RentalService;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Minimal view router for the authentication flow.
 *
 * <p>Owns the primary {@link Stage}, loads {@code login-view.fxml} and
 * {@code main-view.fxml}, applies {@code app.css}, and swaps between the two by
 * replacing the scene root (preserving the window title and minimum size). It
 * injects the shared application services into each controller after loading.
 * It performs no HTTP calls, no credential validation, and knows nothing about
 * domain screens.</p>
 */
public final class ViewManager {

	static final String LOGIN_FXML = "/be/condorcet/easycarrent/desktop/view/login-view.fxml";
	static final String MAIN_FXML = "/be/condorcet/easycarrent/desktop/view/main-view.fxml";
	static final String APP_CSS = "/be/condorcet/easycarrent/desktop/view/app.css";

	private static final double INITIAL_WIDTH = 900;
	private static final double INITIAL_HEIGHT = 600;
	private static final double MIN_WIDTH = 640;
	private static final double MIN_HEIGHT = 480;

	private final Stage stage;
	private final AuthenticationService authenticationService;
	private final SessionManager sessionManager;
	private final VehicleCategoryService vehicleCategoryService;
	private final VehicleService vehicleService;
	private final CustomerService customerService;
	private final RentalService rentalService;
	private final PaymentService paymentService;
	private final MaintenanceService maintenanceService;
	private final DashboardService dashboardService;

	public ViewManager(Stage stage, AuthenticationService authenticationService,
			SessionManager sessionManager, VehicleCategoryService vehicleCategoryService,
			VehicleService vehicleService, CustomerService customerService,
			RentalService rentalService, PaymentService paymentService,
			MaintenanceService maintenanceService, DashboardService dashboardService) {
		this.stage = Objects.requireNonNull(stage, "stage");
		this.authenticationService =
				Objects.requireNonNull(authenticationService, "authenticationService");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
		this.vehicleCategoryService =
				Objects.requireNonNull(vehicleCategoryService, "vehicleCategoryService");
		this.vehicleService = Objects.requireNonNull(vehicleService, "vehicleService");
		this.customerService = Objects.requireNonNull(customerService, "customerService");
		this.rentalService = Objects.requireNonNull(rentalService, "rentalService");
		this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
		this.maintenanceService = Objects.requireNonNull(maintenanceService, "maintenanceService");
		this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
	}

	/** Builds the primary scene showing the login view and displays the stage. */
	public void start() {
		Scene scene = new Scene(loadLogin(), INITIAL_WIDTH, INITIAL_HEIGHT);
		scene.getStylesheets().add(requireResource(APP_CSS).toExternalForm());
		stage.setTitle("Easy Car Rent");
		stage.setMinWidth(MIN_WIDTH);
		stage.setMinHeight(MIN_HEIGHT);
		stage.setScene(scene);
		stage.show();
	}

	public void showLogin() {
		setRoot(loadLogin());
	}

	public void showMain() {
		setRoot(loadMain());
	}

	private Parent loadLogin() {
		FXMLLoader loader = new FXMLLoader(requireResource(ViewManager.LOGIN_FXML));
		Parent root = load(loader);
		LoginController controller = loader.getController();
		controller.init(authenticationService, sessionManager, this);
		return root;
	}

	private Parent loadMain() {
		FXMLLoader loader = new FXMLLoader(requireResource(ViewManager.MAIN_FXML));
		Parent root = load(loader);
		MainViewController controller = loader.getController();
		controller.init(sessionManager, this, vehicleCategoryService, vehicleService, customerService,
				rentalService, paymentService, maintenanceService, dashboardService);
		return root;
	}

	private void setRoot(Parent root) {
		stage.getScene().setRoot(root);
	}

	private static Parent load(FXMLLoader loader) {
		try {
			return loader.load();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load view: " + e.getMessage(), e);
		}
	}

	private static URL requireResource(String path) {
		return Objects.requireNonNull(ViewManager.class.getResource(path),
				() -> "Required view resource not found on the classpath: " + path);
	}
}
