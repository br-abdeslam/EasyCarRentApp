package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the backend maintenance authorization rules: USER and ADMIN may read, while
 * creating, starting, completing, and deleting all require ADMIN; a null role is
 * read-only.
 */
class MaintenancePermissionsTest {

	@Test
	void userMayOnlyRead() {
		assertTrue(MaintenancePermissions.canList(DesktopUserRole.USER));
		assertFalse(MaintenancePermissions.canCreate(DesktopUserRole.USER));
		assertFalse(MaintenancePermissions.canStart(DesktopUserRole.USER));
		assertFalse(MaintenancePermissions.canComplete(DesktopUserRole.USER));
		assertFalse(MaintenancePermissions.canDelete(DesktopUserRole.USER));
		assertFalse(MaintenancePermissions.canWrite(DesktopUserRole.USER));
	}

	@Test
	void adminMayReadAndWriteEverything() {
		assertTrue(MaintenancePermissions.canList(DesktopUserRole.ADMIN));
		assertTrue(MaintenancePermissions.canCreate(DesktopUserRole.ADMIN));
		assertTrue(MaintenancePermissions.canStart(DesktopUserRole.ADMIN));
		assertTrue(MaintenancePermissions.canComplete(DesktopUserRole.ADMIN));
		assertTrue(MaintenancePermissions.canDelete(DesktopUserRole.ADMIN));
		assertTrue(MaintenancePermissions.canWrite(DesktopUserRole.ADMIN));
	}

	@Test
	void nullRoleIsReadOnly() {
		assertFalse(MaintenancePermissions.canList(null));
		assertFalse(MaintenancePermissions.canCreate(null));
		assertFalse(MaintenancePermissions.canStart(null));
		assertFalse(MaintenancePermissions.canComplete(null));
		assertFalse(MaintenancePermissions.canDelete(null));
		assertFalse(MaintenancePermissions.canWrite(null));
	}
}
