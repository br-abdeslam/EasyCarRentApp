package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.navigation.MainSection;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the reusable section placeholder.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor. It only displays the title and description of the selected
 * {@link MainSection}; it holds no Stage, service, session, HTTP client, or
 * domain logic, and injects no fake data at initialization.</p>
 */
public class SectionPlaceholderController {

	@FXML
	private Label sectionTitleLabel;

	@FXML
	private Label sectionDescriptionLabel;

	/**
	 * Displays the given section's title and description.
	 *
	 * @param section the section to present
	 * @throws NullPointerException if {@code section} is null
	 */
	public void setSection(MainSection section) {
		Objects.requireNonNull(section, "section must not be null");
		sectionTitleLabel.setText(section.title());
		sectionDescriptionLabel.setText(section.description());
	}
}
