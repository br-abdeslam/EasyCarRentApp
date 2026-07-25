package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.support.FakeHttpResponse;
import be.condorcet.easycarrent.desktop.support.RecordingHttpClient;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String FICTIONAL_PASSWORD = "fictional-password";

	private ApiClient apiClient(RecordingHttpClient http) {
		return new ApiClient(BASE_URI, http, JsonMapperFactory.create(), Duration.ofSeconds(10));
	}

	private BasicCredentials credentials(String username) {
		return new BasicCredentials(username, FICTIONAL_PASSWORD);
	}

	@Test
	void validUserCredentialsAuthenticateAsUserAndCreateSession() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.returning(
						new FakeHttpResponse(200, "[]"))), session);

		AuthenticationResult result = service.authenticate(credentials("user")).join();

		assertEquals(AuthenticationResult.Status.AUTHENTICATED, result.status());
		assertTrue(session.isAuthenticated());
		assertEquals("user", session.currentUser().orElseThrow().username());
		assertEquals(DesktopUserRole.USER, session.currentUser().orElseThrow().role());
	}

	@Test
	void validAdminCredentialsAuthenticateAsAdmin() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.returning(
						new FakeHttpResponse(200, "[]"))), session);

		AuthenticationResult result = service.authenticate(credentials("admin")).join();

		assertEquals(AuthenticationResult.Status.AUTHENTICATED, result.status());
		assertEquals(DesktopUserRole.ADMIN, session.currentUser().orElseThrow().role());
	}

	@Test
	void unauthorizedIsInvalidCredentialsAndCreatesNoSession() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.returning(
						new FakeHttpResponse(401, ""))), session);

		AuthenticationResult result = service.authenticate(credentials("user")).join();

		assertEquals(AuthenticationResult.Status.INVALID_CREDENTIALS, result.status());
		assertFalse(session.isAuthenticated());
	}

	@Test
	void forbiddenIsUnexpectedErrorAndCreatesNoSession() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.returning(
						new FakeHttpResponse(403, ""))), session);

		AuthenticationResult result = service.authenticate(credentials("user")).join();

		assertEquals(AuthenticationResult.Status.UNEXPECTED_ERROR, result.status());
		assertFalse(session.isAuthenticated());
	}

	@Test
	void connectionFailureIsBackendUnavailableAndCreatesNoSession() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.failingWith(
						new ConnectException("Connection refused"))), session);

		AuthenticationResult result = service.authenticate(credentials("user")).join();

		assertEquals(AuthenticationResult.Status.BACKEND_UNAVAILABLE, result.status());
		assertFalse(session.isAuthenticated());
	}

	@Test
	void otherNonSuccessIsUnexpectedErrorAndCreatesNoSession() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.returning(
						new FakeHttpResponse(500, "boom"))), session);

		AuthenticationResult result = service.authenticate(credentials("user")).join();

		assertEquals(AuthenticationResult.Status.UNEXPECTED_ERROR, result.status());
		assertFalse(session.isAuthenticated());
	}

	@Test
	void doesNotBlockAndNeverExposesPasswordInMessages() {
		SessionManager session = new SessionManager();
		AuthenticationService service =
				new AuthenticationService(apiClient(RecordingHttpClient.deferred(
						new java.util.concurrent.CompletableFuture<HttpResponse<String>>())), session);

		var future = service.authenticate(credentials("user"));

		assertFalse(future.isDone(), "authentication must not block the caller");

		for (int status : new int[] {401, 403, 500}) {
			AuthenticationResult result = new AuthenticationService(
					apiClient(RecordingHttpClient.returning(new FakeHttpResponse(status, "x"))),
					new SessionManager()).authenticate(credentials("user")).join();
			assertFalse(result.message().contains(FICTIONAL_PASSWORD),
					"result message must never contain the password");
		}
	}
}
