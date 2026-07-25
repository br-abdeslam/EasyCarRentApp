package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleCategoryValidatorTest {

	@Test
	void acceptsValidNameAndDescription() {
		assertTrue(VehicleCategoryValidator.validate("Compact", "Small city cars").isEmpty());
	}

	@Test
	void acceptsMissingDescription() {
		assertTrue(VehicleCategoryValidator.isValid("Compact", null));
		assertTrue(VehicleCategoryValidator.isValid("Compact", ""));
	}

	@Test
	void rejectsBlankName() {
		assertFalse(VehicleCategoryValidator.validate("   ", "desc").isEmpty());
		assertFalse(VehicleCategoryValidator.validate(null, "desc").isEmpty());
	}

	@Test
	void rejectsNameOverMaximumLength() {
		String tooLong = "x".repeat(VehicleCategoryValidator.NAME_MAX_LENGTH + 1);
		assertFalse(VehicleCategoryValidator.isValid(tooLong, null));
	}

	@Test
	void acceptsNameAtMaximumLength() {
		String atMax = "x".repeat(VehicleCategoryValidator.NAME_MAX_LENGTH);
		assertTrue(VehicleCategoryValidator.isValid(atMax, null));
	}

	@Test
	void rejectsDescriptionOverMaximumLength() {
		String tooLong = "d".repeat(VehicleCategoryValidator.DESCRIPTION_MAX_LENGTH + 1);
		assertFalse(VehicleCategoryValidator.isValid("Compact", tooLong));
	}
}
