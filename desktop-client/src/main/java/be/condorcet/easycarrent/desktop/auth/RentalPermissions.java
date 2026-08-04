package be.condorcet.easycarrent.desktop.auth;

/**
 * Client-side view of the backend's rental authorization rules.
 *
 * <p>Reflects the backend security configuration exactly: reading rentals,
 * creating and updating a rental, and running the lifecycle transitions (start,
 * complete, cancel) are all allowed for USER and ADMIN, while <em>deleting</em> a
 * rental requires ADMIN. An unknown or null role is treated as read-only. These
 * checks only decide which controls to show; the backend remains the authority and
 * still rejects unauthorized requests.</p>
 */
public final class RentalPermissions {

	private RentalPermissions() {
	}

	/** Reading and refreshing the list is allowed for any authenticated role. */
	public static boolean canList(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Booking a rental is allowed for USER and ADMIN. */
	public static boolean canCreate(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Updating a PLANNED rental is allowed for USER and ADMIN. */
	public static boolean canUpdate(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Starting a rental is allowed for USER and ADMIN. */
	public static boolean canStart(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Completing a rental is allowed for USER and ADMIN. */
	public static boolean canComplete(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Cancelling a rental is allowed for USER and ADMIN. */
	public static boolean canCancel(DesktopUserRole role) {
		return isAuthenticated(role);
	}

	/** Deleting a rental requires ADMIN. */
	public static boolean canDelete(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/**
	 * @return true if the role may perform any rental write operation (booking,
	 *         updating, a lifecycle transition, or deletion)
	 */
	public static boolean canWrite(DesktopUserRole role) {
		return canCreate(role) || canUpdate(role) || canStart(role)
				|| canComplete(role) || canCancel(role) || canDelete(role);
	}

	private static boolean isAuthenticated(DesktopUserRole role) {
		return role == DesktopUserRole.USER || role == DesktopUserRole.ADMIN;
	}
}
