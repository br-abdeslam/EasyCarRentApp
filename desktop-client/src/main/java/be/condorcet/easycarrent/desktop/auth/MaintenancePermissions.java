package be.condorcet.easycarrent.desktop.auth;

/**
 * Client-side view of the backend's maintenance authorization rules.
 *
 * <p>Reflects the backend security configuration exactly: reading maintenance
 * records is allowed for USER and ADMIN, while creating, starting, completing, and
 * deleting a record all require ADMIN (maintenance management is an administrative
 * operation). An unknown or null role is treated as read-only. These checks only
 * decide which controls to show; the backend remains the authority and still
 * rejects unauthorized requests. There is no maintenance update operation on the
 * backend.</p>
 */
public final class MaintenancePermissions {

	private MaintenancePermissions() {
	}

	/** Reading and refreshing the list is allowed for any authenticated role. */
	public static boolean canList(DesktopUserRole role) {
		return role == DesktopUserRole.USER || role == DesktopUserRole.ADMIN;
	}

	/** Creating a maintenance record requires ADMIN. */
	public static boolean canCreate(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/** Starting a maintenance record requires ADMIN. */
	public static boolean canStart(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/** Completing a maintenance record requires ADMIN. */
	public static boolean canComplete(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/** Deleting a maintenance record requires ADMIN. */
	public static boolean canDelete(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/** @return true if the role may perform any maintenance write operation. */
	public static boolean canWrite(DesktopUserRole role) {
		return canCreate(role) || canStart(role) || canComplete(role) || canDelete(role);
	}
}
