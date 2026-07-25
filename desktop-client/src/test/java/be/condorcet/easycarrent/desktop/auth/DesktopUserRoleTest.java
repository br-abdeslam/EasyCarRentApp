package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DesktopUserRoleTest {

	@Test
	void mapsUserAccountToUserRole() {
		assertEquals(Optional.of(DesktopUserRole.USER),
				DesktopUserRole.forDevelopmentUsername("user"));
	}

	@Test
	void mapsAdminAccountToAdminRole() {
		assertEquals(Optional.of(DesktopUserRole.ADMIN),
				DesktopUserRole.forDevelopmentUsername("admin"));
	}

	@Test
	void returnsEmptyForUnknownOrNullUsername() {
		assertTrue(DesktopUserRole.forDevelopmentUsername("someone-else").isEmpty());
		assertTrue(DesktopUserRole.forDevelopmentUsername(null).isEmpty());
	}
}
