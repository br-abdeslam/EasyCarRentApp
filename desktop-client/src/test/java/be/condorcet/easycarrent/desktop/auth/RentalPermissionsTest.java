package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the backend rental authorization rules: USER and ADMIN may read, book,
 * update, and run the lifecycle transitions; only ADMIN may delete; a null role is
 * read-only.
 */
class RentalPermissionsTest {

	@Test
	void userMayReadBookUpdateAndTransitionButNotDelete() {
		assertTrue(RentalPermissions.canList(DesktopUserRole.USER));
		assertTrue(RentalPermissions.canCreate(DesktopUserRole.USER));
		assertTrue(RentalPermissions.canUpdate(DesktopUserRole.USER));
		assertTrue(RentalPermissions.canStart(DesktopUserRole.USER));
		assertTrue(RentalPermissions.canComplete(DesktopUserRole.USER));
		assertTrue(RentalPermissions.canCancel(DesktopUserRole.USER));
		assertFalse(RentalPermissions.canDelete(DesktopUserRole.USER));
		assertTrue(RentalPermissions.canWrite(DesktopUserRole.USER));
	}

	@Test
	void adminMayDoEverythingIncludingDelete() {
		assertTrue(RentalPermissions.canList(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canCreate(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canUpdate(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canStart(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canComplete(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canCancel(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canDelete(DesktopUserRole.ADMIN));
		assertTrue(RentalPermissions.canWrite(DesktopUserRole.ADMIN));
	}

	@Test
	void nullRoleIsReadOnly() {
		assertFalse(RentalPermissions.canList(null));
		assertFalse(RentalPermissions.canCreate(null));
		assertFalse(RentalPermissions.canUpdate(null));
		assertFalse(RentalPermissions.canStart(null));
		assertFalse(RentalPermissions.canComplete(null));
		assertFalse(RentalPermissions.canCancel(null));
		assertFalse(RentalPermissions.canDelete(null));
		assertFalse(RentalPermissions.canWrite(null));
	}
}
