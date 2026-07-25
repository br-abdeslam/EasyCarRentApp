package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleCategoryPermissionsTest {

	@Test
	void bothRolesMayList() {
		assertTrue(VehicleCategoryPermissions.canList(DesktopUserRole.USER));
		assertTrue(VehicleCategoryPermissions.canList(DesktopUserRole.ADMIN));
	}

	@Test
	void onlyAdminMayCreate() {
		assertFalse(VehicleCategoryPermissions.canCreate(DesktopUserRole.USER));
		assertTrue(VehicleCategoryPermissions.canCreate(DesktopUserRole.ADMIN));
	}

	@Test
	void onlyAdminMayUpdate() {
		assertFalse(VehicleCategoryPermissions.canUpdate(DesktopUserRole.USER));
		assertTrue(VehicleCategoryPermissions.canUpdate(DesktopUserRole.ADMIN));
	}

	@Test
	void onlyAdminMayDelete() {
		assertFalse(VehicleCategoryPermissions.canDelete(DesktopUserRole.USER));
		assertTrue(VehicleCategoryPermissions.canDelete(DesktopUserRole.ADMIN));
	}

	@Test
	void onlyAdminCanWrite() {
		assertFalse(VehicleCategoryPermissions.canWrite(DesktopUserRole.USER));
		assertTrue(VehicleCategoryPermissions.canWrite(DesktopUserRole.ADMIN));
	}

	@Test
	void nullRoleIsReadOnly() {
		assertFalse(VehicleCategoryPermissions.canList(null));
		assertFalse(VehicleCategoryPermissions.canWrite(null));
		assertFalse(VehicleCategoryPermissions.canCreate(null));
		assertFalse(VehicleCategoryPermissions.canUpdate(null));
		assertFalse(VehicleCategoryPermissions.canDelete(null));
	}
}
