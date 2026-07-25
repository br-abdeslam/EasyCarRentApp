package be.condorcet.easycarrent.desktop.auth;

/**
 * Client-side view of the backend's vehicle-category authorization rules.
 *
 * <p>Reflects the backend security configuration exactly: reading categories is
 * allowed for USER and ADMIN, while creating, updating, and deleting a category
 * require ADMIN. An unknown or null role is treated as read-only. These checks
 * only decide which controls to show; the backend remains the authority and
 * still rejects unauthorized requests.</p>
 */
public final class VehicleCategoryPermissions {

	private VehicleCategoryPermissions() {
	}

	/** Reading and refreshing the list is allowed for any authenticated role. */
	public static boolean canList(DesktopUserRole role) {
		return role == DesktopUserRole.USER || role == DesktopUserRole.ADMIN;
	}

	public static boolean canCreate(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	public static boolean canUpdate(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	public static boolean canDelete(DesktopUserRole role) {
		return role == DesktopUserRole.ADMIN;
	}

	/** @return true if the role may perform any write operation */
	public static boolean canWrite(DesktopUserRole role) {
		return canCreate(role) || canUpdate(role) || canDelete(role);
	}
}
