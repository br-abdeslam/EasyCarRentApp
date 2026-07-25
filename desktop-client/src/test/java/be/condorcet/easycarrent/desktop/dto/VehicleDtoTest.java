package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class VehicleDtoTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void deserializesResponseWithAllFields() throws Exception {
		String json = """
				{"id":5,"registrationNumber":"1-ABC-123","brand":"Toyota","model":"Yaris",
				 "manufacturingYear":2022,"color":"Blue","dailyPrice":42.50,"mileage":15000,
				 "status":"AVAILABLE","categoryId":3,"categoryName":"Compact"}
				""";

		VehicleResponseDto dto = objectMapper.readValue(json, VehicleResponseDto.class);

		assertEquals(5L, dto.id());
		assertEquals("1-ABC-123", dto.registrationNumber());
		assertEquals("Toyota", dto.brand());
		assertEquals("Yaris", dto.model());
		assertEquals(2022, dto.manufacturingYear());
		assertEquals("Blue", dto.color());
		assertEquals(new BigDecimal("42.50"), dto.dailyPrice());
		assertEquals(15000L, dto.mileage());
		assertEquals(VehicleStatus.AVAILABLE, dto.status());
		assertEquals(3L, dto.categoryId());
		assertEquals("Compact", dto.categoryName());
	}

	@Test
	void deserializesResponseWithNullOptionalFields() throws Exception {
		String json = """
				{"id":1,"registrationNumber":"X","brand":"B","model":"M","manufacturingYear":null,
				 "color":null,"dailyPrice":10,"mileage":null,"status":"INACTIVE",
				 "categoryId":2,"categoryName":"Van"}
				""";

		VehicleResponseDto dto = objectMapper.readValue(json, VehicleResponseDto.class);

		assertNull(dto.manufacturingYear());
		assertNull(dto.color());
		assertNull(dto.mileage());
		assertEquals(VehicleStatus.INACTIVE, dto.status());
	}

	@Test
	void ignoresUnknownResponseProperties() throws Exception {
		String json = """
				{"id":1,"registrationNumber":"X","brand":"B","model":"M","dailyPrice":10,
				 "status":"RENTED","categoryId":2,"categoryName":"Van","futureField":"ignored"}
				""";

		VehicleResponseDto dto = objectMapper.readValue(json, VehicleResponseDto.class);

		assertEquals(VehicleStatus.RENTED, dto.status());
	}

	@Test
	void deserializesVehicleListAndEmptyList() throws Exception {
		String json = """
				[{"id":1,"registrationNumber":"A","brand":"B","model":"M","dailyPrice":9.99,
				  "status":"AVAILABLE","categoryId":1,"categoryName":"C"}]
				""";

		List<VehicleResponseDto> list = objectMapper.readValue(json, objectMapper.getTypeFactory()
				.constructCollectionType(List.class, VehicleResponseDto.class));
		assertEquals(1, list.size());

		List<VehicleResponseDto> empty = objectMapper.readValue("[]", objectMapper.getTypeFactory()
				.constructCollectionType(List.class, VehicleResponseDto.class));
		assertTrue(empty.isEmpty());
	}

	@Test
	void serializesRequestWithoutIdOrStatus() throws Exception {
		VehicleRequestDto request = new VehicleRequestDto(
				"1-ABC-123", "Toyota", "Yaris", 2022, "Blue",
				new BigDecimal("42.50"), 15000L, 3L);

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"registrationNumber\":\"1-ABC-123\""));
		assertTrue(json.contains("\"categoryId\":3"));
		assertTrue(json.contains("\"dailyPrice\":42.50"), "BigDecimal must serialize as a JSON number");
		assertFalse(json.contains("\"id\""), "request must not carry an id");
		assertFalse(json.contains("\"status\""), "request must not carry a status");
		assertFalse(json.contains("\"categoryName\""), "request must not carry a category name");
	}

	@Test
	void serializesRequestWithNullOptionalFields() throws Exception {
		VehicleRequestDto request = new VehicleRequestDto(
				"X", "B", "M", null, null, new BigDecimal("10.00"), null, 1L);

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"categoryId\":1"));
		assertTrue(json.contains("\"manufacturingYear\":null"));
	}
}
