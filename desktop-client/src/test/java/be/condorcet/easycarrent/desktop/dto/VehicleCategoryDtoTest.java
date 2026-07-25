package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

class VehicleCategoryDtoTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void deserializesResponseWithAllFields() throws Exception {
		String json = """
				{"id":7,"name":"Compact","description":"Small city cars"}
				""";

		VehicleCategoryResponseDto dto = objectMapper.readValue(json, VehicleCategoryResponseDto.class);

		assertEquals(7L, dto.id());
		assertEquals("Compact", dto.name());
		assertEquals("Small city cars", dto.description());
	}

	@Test
	void deserializesResponseWithNullDescription() throws Exception {
		String json = """
				{"id":3,"name":"Van","description":null}
				""";

		VehicleCategoryResponseDto dto = objectMapper.readValue(json, VehicleCategoryResponseDto.class);

		assertEquals("Van", dto.name());
		assertNull(dto.description());
	}

	@Test
	void ignoresUnknownResponseProperties() throws Exception {
		String json = """
				{"id":1,"name":"SUV","description":"Sport utility","futureField":"ignored"}
				""";

		VehicleCategoryResponseDto dto = objectMapper.readValue(json, VehicleCategoryResponseDto.class);

		assertEquals("SUV", dto.name());
	}

	@Test
	void deserializesCategoryList() throws Exception {
		String json = """
				[{"id":1,"name":"Compact","description":"a"},
				 {"id":2,"name":"SUV","description":"b"}]
				""";

		List<VehicleCategoryResponseDto> categories = objectMapper.readValue(json,
				objectMapper.getTypeFactory()
						.constructCollectionType(List.class, VehicleCategoryResponseDto.class));

		assertEquals(2, categories.size());
		assertEquals("Compact", categories.get(0).name());
	}

	@Test
	void deserializesEmptyList() throws Exception {
		List<VehicleCategoryResponseDto> categories = objectMapper.readValue("[]",
				objectMapper.getTypeFactory()
						.constructCollectionType(List.class, VehicleCategoryResponseDto.class));

		assertTrue(categories.isEmpty());
	}

	@Test
	void serializesRequestToExactBackendShape() throws Exception {
		VehicleCategoryRequestDto request = new VehicleCategoryRequestDto("Compact", "Small cars");

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"name\":\"Compact\""));
		assertTrue(json.contains("\"description\":\"Small cars\""));
		assertFalse(json.contains("\"id\""), "request must not carry an id");
	}
}
