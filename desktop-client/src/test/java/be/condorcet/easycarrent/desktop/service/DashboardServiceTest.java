package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dashboard.DashboardLoadResult;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.support.RoutingHttpClient;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class DashboardServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private static final String VEHICLES_JSON = """
			[{"id":1,"registrationNumber":"R","brand":"B","model":"M","manufacturingYear":2022,
			  "color":"Blue","dailyPrice":50.00,"mileage":1000,"status":"AVAILABLE",
			  "categoryId":1,"categoryName":"C"}]
			""";
	private static final String PAYMENTS_JSON = """
			[{"id":1,"rentalId":1,"rentalStatus":"ACTIVE","amount":100.00,"paymentMethod":"CARD",
			  "status":"PAID","createdAt":"2026-08-01T10:00:00","paidAt":null}]
			""";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private DashboardService service(RoutingHttpClient http, SessionManager session) {
		ApiClient apiClient = new ApiClient(BASE_URI, http, JsonMapperFactory.create(),
				Duration.ofSeconds(10));
		return new DashboardService(
				new VehicleCategoryService(apiClient, session),
				new VehicleService(apiClient, session),
				new CustomerService(apiClient, session),
				new RentalService(apiClient, session),
				new PaymentService(apiClient, session),
				new MaintenanceService(apiClient, session));
	}

	@Test
	void loadsAllSixSectionsAndInvokesEveryEndpoint() {
		RoutingHttpClient http = new RoutingHttpClient()
				.ok("/api/vehicles", VEHICLES_JSON)
				.ok("/api/payments", PAYMENTS_JSON);

		DashboardLoadResult result = service(http, authenticatedSession()).load().join();

		assertEquals(6, http.callCount(), "every source endpoint must be queried");
		assertTrue(result.allAvailable());
		assertEquals(1, result.vehicles().records().size());
		assertEquals(1, result.payments().records().size());
	}

	@Test
	void completeEmptySuccessMarksEverySectionAvailableAndEmpty() {
		DashboardLoadResult result = service(new RoutingHttpClient(), authenticatedSession()).load().join();

		assertTrue(result.allAvailable());
		assertTrue(result.categories().records().isEmpty());
		assertTrue(result.vehicles().records().isEmpty());
		assertTrue(result.customers().records().isEmpty());
		assertTrue(result.rentals().records().isEmpty());
		assertTrue(result.payments().records().isEmpty());
		assertTrue(result.maintenance().records().isEmpty());
	}

	@Test
	void oneFailedSourceProducesAPartialResultWithWorkingSectionsAvailable() {
		RoutingHttpClient http = new RoutingHttpClient()
				.fail("/api/vehicles", new ConnectException("refused"));

		DashboardLoadResult result = service(http, authenticatedSession()).load().join();

		assertFalse(result.allAvailable());
		assertTrue(result.anyAvailable());
		assertFalse(result.vehicles().isAvailable());
		assertTrue(result.customers().isAvailable(), "a working section stays available");
		assertTrue(result.payments().isAvailable());
		assertEquals(java.util.List.of("Vehicles"), result.unavailableSectionNames());
	}

	@Test
	void severalFailedSourcesProduceAPartialResult() {
		RoutingHttpClient http = new RoutingHttpClient()
				.fail("/api/vehicles", new ConnectException("refused"))
				.status("/api/payments", 500, "{\"status\":500,\"message\":\"boom\"}");

		DashboardLoadResult result = service(http, authenticatedSession()).load().join();

		assertFalse(result.allAvailable());
		assertTrue(result.anyAvailable());
		assertFalse(result.vehicles().isAvailable());
		assertFalse(result.payments().isAvailable());
		assertEquals(java.util.List.of("Vehicles", "Payments"), result.unavailableSectionNames());
	}

	@Test
	void allFailedSourcesProduceAnUnavailableResult() {
		RoutingHttpClient http = new RoutingHttpClient()
				.fail("/api/categories", new ConnectException("x"))
				.fail("/api/vehicles", new ConnectException("x"))
				.fail("/api/customers", new ConnectException("x"))
				.fail("/api/rentals", new ConnectException("x"))
				.fail("/api/payments", new ConnectException("x"))
				.fail("/api/maintenance-records", new ConnectException("x"));

		DashboardLoadResult result = service(http, authenticatedSession()).load().join();

		assertTrue(result.allUnavailable());
		assertFalse(result.anyAvailable());
	}

	@Test
	void apiRequestFailureBecomesASafeSectionFailure() {
		RoutingHttpClient http = new RoutingHttpClient()
				.status("/api/vehicles", 500, "{\"status\":500,\"message\":\"boom\"}");

		DashboardLoadResult result = service(http, authenticatedSession()).load().join();

		assertFalse(result.vehicles().isAvailable());
		String message = result.vehicles().failureMessage();
		assertTrue(message.toLowerCase().contains("could not be loaded"));
		assertSafeMessage(message);
	}

	@Test
	void apiConnectionFailureBecomesASafeSectionFailure() {
		RoutingHttpClient http = new RoutingHttpClient()
				.fail("/api/customers", new ConnectException("Connection refused"));

		DashboardLoadResult result = service(http, authenticatedSession()).load().join();

		assertFalse(result.customers().isAvailable());
		String message = result.customers().failureMessage();
		assertTrue(message.toLowerCase().contains("unavailable"));
		assertSafeMessage(message);
	}

	private static void assertSafeMessage(String message) {
		assertFalse(message.contains(PASSWORD), "no password in message");
		assertFalse(message.contains(USERNAME), "no username in message");
		assertFalse(message.toLowerCase().contains("exception"), "no exception-class name");
		assertFalse(message.contains("{"), "no raw JSON");
		assertFalse(message.toLowerCase().contains("authorization"), "no Authorization value");
	}
}
