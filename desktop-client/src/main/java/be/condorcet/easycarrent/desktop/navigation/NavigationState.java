package be.condorcet.easycarrent.desktop.navigation;

import java.util.Objects;

/**
 * Pure holder for the currently selected {@link MainSection}.
 *
 * <p>Extracted from {@link MainContentRouter} so the selection logic can be
 * tested without the JavaFX toolkit. It keeps only the single current section
 * (defaulting to {@link MainSection#DASHBOARD}); no history of previous sections
 * is retained.</p>
 */
public final class NavigationState {

	private MainSection current = MainSection.DASHBOARD;

	/** @return the currently selected section (never null) */
	public MainSection current() {
		return current;
	}

	/**
	 * Selects a section, replacing the previous one.
	 *
	 * @param section the section to select
	 * @throws NullPointerException if {@code section} is null
	 */
	public void select(MainSection section) {
		this.current = Objects.requireNonNull(section, "section must not be null");
	}
}
