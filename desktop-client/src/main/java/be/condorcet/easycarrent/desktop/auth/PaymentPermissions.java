package be.condorcet.easycarrent.desktop.auth;

/**
 * Client-side view of the backend's payment authorization rules.
 *
 * <p>Reflects the backend security configuration exactly: reading payments,
 * creating a payment, and the pay/fail/retry lifecycle transitions are allowed for
 * USER and ADMIN, while <em>refunding</em> and <em>deleting</em> a payment require
 * ADMIN. An unknown or null role is treated as read-only. These checks only decide
 * which controls to show; the backend remains the authority and still rejects
 * unauthorized requests. There is no payment update operation on the backend.</p>
 */
public final class PaymentPermissions {

	private PaymentPermissions() {
	}

	/** Reading and refreshing the list is allowed for any authenticated role. */
	public static boolean canList(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Creating a payment is allowed for USER and ADMIN. */
	public static boolean canCreate(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Marking a payment paid is allowed for USER and ADMIN. */
	public static boolean canMarkPaid(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Marking a payment failed is allowed for USER and ADMIN. */
	public static boolean canMarkFailed(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Retrying a failed payment is allowed for USER and ADMIN. */
	public static boolean canRetry(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Refunding a paid payment requires ADMIN. */
	public static boolean canRefund(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/** Deleting a payment requires ADMIN. */
	public static boolean canDelete(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/**
	 * @return true if the role may perform any payment write operation (create, a
	 *         lifecycle transition, refund, or delete)
	 */
	public static boolean canWrite(DesktopUserRole role) {
		return canCreate(role) || canMarkPaid(role) || canMarkFailed(role)
				|| canRetry(role) || canRefund(role) || canDelete(role);
	}

	private static boolean isAuthenticated(DesktopUserRole role) {
		return role == DesktopUserRole.USER || role == DesktopUserRole.ADMIN;
	}
}
