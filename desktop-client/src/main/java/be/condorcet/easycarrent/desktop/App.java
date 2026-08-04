package be.condorcet.easycarrent.desktop;

import be.condorcet.easycarrent.desktop.config.ApiConfiguration;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.service.AuthenticationService;
import be.condorcet.easycarrent.desktop.service.CustomerService;
import be.condorcet.easycarrent.desktop.service.MaintenanceService;
import be.condorcet.easycarrent.desktop.service.PaymentService;
import be.condorcet.easycarrent.desktop.service.RentalService;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.view.ViewManager;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application bootstrap for the Easy Car Rent desktop client.
 *
 * <p>Assembles the application-scoped collaborators (configuration, API client,
 * in-memory session, and authentication service), hands them to a
 * {@link ViewManager}, and shows the login view. It contains no controls, no
 * credential validation, no HTTP calls of its own, no domain navigation, and no
 * passwords.</p>
 */
public class App extends Application {

	@Override
	public void start(Stage stage) {
		ApiConfiguration configuration = new ApiConfiguration();
		ApiClient apiClient = new ApiClient(configuration.baseUri());
		SessionManager sessionManager = new SessionManager();
		AuthenticationService authenticationService =
				new AuthenticationService(apiClient, sessionManager);
		VehicleCategoryService vehicleCategoryService =
				new VehicleCategoryService(apiClient, sessionManager);
		VehicleService vehicleService = new VehicleService(apiClient, sessionManager);
		CustomerService customerService = new CustomerService(apiClient, sessionManager);
		RentalService rentalService = new RentalService(apiClient, sessionManager);
		PaymentService paymentService = new PaymentService(apiClient, sessionManager);
		MaintenanceService maintenanceService = new MaintenanceService(apiClient, sessionManager);

		ViewManager viewManager = new ViewManager(stage, authenticationService, sessionManager,
				vehicleCategoryService, vehicleService, customerService, rentalService, paymentService,
				maintenanceService);
		viewManager.start();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
