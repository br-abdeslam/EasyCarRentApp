package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the backend payment authorization rules: USER and ADMIN may read, create,
 * and run the pay/fail/retry transitions; only ADMIN may refund or delete; a null
 * role is read-only.
 */
class PaymentPermissionsTest {

	@Test
	void userMayReadCreatePayFailRetryButNotRefundOrDelete() {
		assertTrue(PaymentPermissions.canList(DesktopUserRole.USER));
		assertTrue(PaymentPermissions.canCreate(DesktopUserRole.USER));
		assertTrue(PaymentPermissions.canMarkPaid(DesktopUserRole.USER));
		assertTrue(PaymentPermissions.canMarkFailed(DesktopUserRole.USER));
		assertTrue(PaymentPermissions.canRetry(DesktopUserRole.USER));
		assertFalse(PaymentPermissions.canRefund(DesktopUserRole.USER));
		assertFalse(PaymentPermissions.canDelete(DesktopUserRole.USER));
		assertTrue(PaymentPermissions.canWrite(DesktopUserRole.USER));
	}

	@Test
	void adminMayDoEverythingIncludingRefundAndDelete() {
		assertTrue(PaymentPermissions.canList(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canCreate(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canMarkPaid(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canMarkFailed(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canRetry(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canRefund(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canDelete(DesktopUserRole.ADMIN));
		assertTrue(PaymentPermissions.canWrite(DesktopUserRole.ADMIN));
	}

	@Test
	void nullRoleIsReadOnly() {
		assertFalse(PaymentPermissions.canList(null));
		assertFalse(PaymentPermissions.canCreate(null));
		assertFalse(PaymentPermissions.canMarkPaid(null));
		assertFalse(PaymentPermissions.canMarkFailed(null));
		assertFalse(PaymentPermissions.canRetry(null));
		assertFalse(PaymentPermissions.canRefund(null));
		assertFalse(PaymentPermissions.canDelete(null));
		assertFalse(PaymentPermissions.canWrite(null));
	}
}
