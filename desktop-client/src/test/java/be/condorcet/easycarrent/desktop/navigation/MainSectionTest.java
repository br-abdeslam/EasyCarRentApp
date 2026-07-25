package be.condorcet.easycarrent.desktop.navigation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class MainSectionTest {

	@Test
	void hasExactlySevenSectionsInExpectedOrder() {
		MainSection[] expected = {
				MainSection.DASHBOARD,
				MainSection.VEHICLE_CATEGORIES,
				MainSection.VEHICLES,
				MainSection.CUSTOMERS,
				MainSection.RENTALS,
				MainSection.PAYMENTS,
				MainSection.MAINTENANCE
		};

		assertEquals(7, MainSection.values().length);
		assertArrayEquals(expected, MainSection.values());
	}

	@Test
	void everyTitleIsNonBlank() {
		for (MainSection section : MainSection.values()) {
			assertFalse(section.title() == null || section.title().isBlank(),
					section + " must have a non-blank title");
		}
	}

	@Test
	void everyDescriptionIsNonBlank() {
		for (MainSection section : MainSection.values()) {
			assertFalse(section.description() == null || section.description().isBlank(),
					section + " must have a non-blank description");
		}
	}

	@Test
	void titlesAreUnique() {
		long distinctTitles = Arrays.stream(MainSection.values())
				.map(MainSection::title)
				.distinct()
				.count();

		assertEquals(MainSection.values().length, distinctTitles);
	}

	@Test
	void descriptionsContainNoDigitsOrCredentials() {
		for (MainSection section : MainSection.values()) {
			String description = section.description();
			assertFalse(description.chars().anyMatch(Character::isDigit),
					section + " description must not contain fake domain numbers");
			assertFalse(description.toLowerCase().contains("password"),
					section + " description must not contain credentials");
		}
	}
}
