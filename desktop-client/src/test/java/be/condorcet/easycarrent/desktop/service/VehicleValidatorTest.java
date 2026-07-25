package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.VehicleRequestDto;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class VehicleValidatorTest {

	private static final int CURRENT_YEAR = 2026;

	private VehicleValidator.Result validate(String reg, String brand, String model, String year,
			String color, String price, String mileage, Long categoryId) {
		return VehicleValidator.validate(reg, brand, model, year, color, price, mileage, categoryId,
				CURRENT_YEAR);
	}

	@Test
	void acceptsValidInputAndBuildsRequest() {
		VehicleValidator.Result result =
				validate("1-ABC-123", "Toyota", "Yaris", "2022", "Blue", "42.50", "15000", 3L);

		assertTrue(result.isValid());
		VehicleRequestDto request = result.request();
		assertEquals("1-ABC-123", request.registrationNumber());
		assertEquals("Toyota", request.brand());
		assertEquals(2022, request.manufacturingYear());
		assertEquals(new BigDecimal("42.50"), request.dailyPrice());
		assertEquals(15000L, request.mileage());
		assertEquals(3L, request.categoryId());
	}

	@Test
	void acceptsMissingOptionalFields() {
		VehicleValidator.Result result =
				validate("REG", "Brand", "Model", "", "", "10.00", "", 1L);

		assertTrue(result.isValid());
		assertNull(result.request().manufacturingYear());
		assertNull(result.request().color());
		assertNull(result.request().mileage());
	}

	@Test
	void rejectsBlankRequiredText() {
		assertFalse(validate("", "B", "M", "", "", "10", "", 1L).isValid());
		assertFalse(validate("R", "", "M", "", "", "10", "", 1L).isValid());
		assertFalse(validate("R", "B", "", "", "", "10", "", 1L).isValid());
	}

	@Test
	void rejectsOversizedText() {
		assertFalse(validate("x".repeat(21), "B", "M", "", "", "10", "", 1L).isValid());
		assertFalse(validate("R", "x".repeat(61), "M", "", "", "10", "", 1L).isValid());
		assertFalse(validate("R", "B", "x".repeat(61), "", "", "10", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", "", "x".repeat(41), "10", "", 1L).isValid());
	}

	@Test
	void rejectsYearOutOfRange() {
		assertFalse(validate("R", "B", "M", "1899", "", "10", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", String.valueOf(CURRENT_YEAR + 1), "", "10", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", "notayear", "", "10", "", 1L).isValid());
	}

	@Test
	void acceptsCurrentYearAndMinYear() {
		assertTrue(validate("R", "B", "M", String.valueOf(CURRENT_YEAR), "", "10", "", 1L).isValid());
		assertTrue(validate("R", "B", "M", "1900", "", "10", "", 1L).isValid());
	}

	@Test
	void rejectsMissingCategory() {
		assertFalse(validate("R", "B", "M", "", "", "10", "", null).isValid());
	}

	@Test
	void rejectsInvalidPrice() {
		assertFalse(validate("R", "B", "M", "", "", "", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", "", "", "0", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", "", "", "-5", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", "", "", "abc", "", 1L).isValid());
	}

	@Test
	void rejectsExcessivePriceScaleAndPrecision() {
		assertFalse(validate("R", "B", "M", "", "", "10.123", "", 1L).isValid());
		assertFalse(validate("R", "B", "M", "", "", "123456789.00", "", 1L).isValid());
	}

	@Test
	void rejectsInvalidMileage() {
		assertFalse(validate("R", "B", "M", "", "", "10", "-1", 1L).isValid());
		assertFalse(validate("R", "B", "M", "", "", "10", "abc", 1L).isValid());
	}
}
