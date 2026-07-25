package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehiclePermissionsTest {

	@Test
	void bothRolesMayList() {
		assertTrue(VehiclePermissions.canList(DesktopUserRole.USER));
		assertTrue(VehiclePermissions.canList(DesktopUserRole.ADMIN));
	}

	@Test
	void onlyAdminMayWrite() {
		assertFalse(VehiclePermissions.canCreate(DesktopUserRole.USER));
		assertFalse(VehiclePermissions.canUpdate(DesktopUserRole.USER));
		assertFalse(VehiclePermissions.canDelete(DesktopUserRole.USER));
		assertFalse(VehiclePermissions.canWrite(DesktopUserRole.USER));

		assertTrue(VehiclePermissions.canCreate(DesktopUserRole.ADMIN));
		assertTrue(VehiclePermissions.canUpdate(DesktopUserRole.ADMIN));
		assertTrue(VehiclePermissions.canDelete(DesktopUserRole.ADMIN));
		assertTrue(VehiclePermissions.canWrite(DesktopUserRole.ADMIN));
	}

	@Test
	void nullRoleIsReadOnly() {
		assertFalse(VehiclePermissions.canList(null));
		assertFalse(VehiclePermissions.canWrite(null));
		assertFalse(VehiclePermissions.canCreate(null));
		assertFalse(VehiclePermissions.canUpdate(null));
		assertFalse(VehiclePermissions.canDelete(null));
	}
}
