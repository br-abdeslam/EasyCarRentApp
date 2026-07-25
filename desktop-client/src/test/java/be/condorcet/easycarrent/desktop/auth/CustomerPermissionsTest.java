package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CustomerPermissionsTest {

	@Test
	void bothRolesMayList() {
		assertTrue(CustomerPermissions.canList(DesktopUserRole.USER));
		assertTrue(CustomerPermissions.canList(DesktopUserRole.ADMIN));
	}

	@Test
	void onlyAdminMayWrite() {
		assertFalse(CustomerPermissions.canCreate(DesktopUserRole.USER));
		assertFalse(CustomerPermissions.canUpdate(DesktopUserRole.USER));
		assertFalse(CustomerPermissions.canDelete(DesktopUserRole.USER));
		assertFalse(CustomerPermissions.canWrite(DesktopUserRole.USER));

		assertTrue(CustomerPermissions.canCreate(DesktopUserRole.ADMIN));
		assertTrue(CustomerPermissions.canUpdate(DesktopUserRole.ADMIN));
		assertTrue(CustomerPermissions.canDelete(DesktopUserRole.ADMIN));
		assertTrue(CustomerPermissions.canWrite(DesktopUserRole.ADMIN));
	}

	@Test
	void nullRoleIsReadOnly() {
		assertFalse(CustomerPermissions.canList(null));
		assertFalse(CustomerPermissions.canWrite(null));
		assertFalse(CustomerPermissions.canCreate(null));
		assertFalse(CustomerPermissions.canUpdate(null));
		assertFalse(CustomerPermissions.canDelete(null));
	}
}
