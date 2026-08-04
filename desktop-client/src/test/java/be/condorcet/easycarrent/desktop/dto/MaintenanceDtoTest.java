package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class MaintenanceDtoTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	private static final String SAMPLE_JSON = """
			{"id":6,"vehicleId":4,"description":"Scheduled brake inspection",
			 "startDate":"2027-09-01","endDate":"2027-09-03","cost":180.00,"status":"PLANNED"}
			""";

	@Test
	void deserializesResponseWithAllFields() throws Exception {
		MaintenanceResponseDto dto = objectMapper.readValue(SAMPLE_JSON, MaintenanceResponseDto.class);

		assertEquals(6L, dto.id());
		assertEquals(4L, dto.vehicleId());
		assertEquals("Scheduled brake inspection", dto.description());
		assertEquals(LocalDate.of(2027, 9, 1), dto.startDate());
		assertEquals(LocalDate.of(2027, 9, 3), dto.endDate());
		assertEquals(0, new BigDecimal("180.00").compareTo(dto.cost()));
		assertEquals(MaintenanceStatus.PLANNED, dto.status());
	}

	@Test
	void mapsEveryMaintenanceStatusValue() throws Exception {
		for (MaintenanceStatus status : MaintenanceStatus.values()) {
			String json = SAMPLE_JSON.replace("\"PLANNED\"", "\"" + status.name() + "\"");
			assertEquals(status, objectMapper.readValue(json, MaintenanceResponseDto.class).status());
		}
	}

	@Test
	void ignoresUnknownResponseProperties() throws Exception {
		String json = SAMPLE_JSON.replaceFirst("\\{", "{\"futureField\":\"ignored\",");
		assertEquals(6L, objectMapper.readValue(json, MaintenanceResponseDto.class).id());
	}

	@Test
	void deserializesMaintenanceListAndEmptyList() throws Exception {
		List<MaintenanceResponseDto> list = objectMapper.readValue("[" + SAMPLE_JSON + "]",
				objectMapper.getTypeFactory()
						.constructCollectionType(List.class, MaintenanceResponseDto.class));
		assertEquals(1, list.size());

		List<MaintenanceResponseDto> empty = objectMapper.readValue("[]", objectMapper.getTypeFactory()
				.constructCollectionType(List.class, MaintenanceResponseDto.class));
		assertTrue(empty.isEmpty());
	}

	@Test
	void serializesRequestWithVehicleDescriptionPeriodAndCost() throws Exception {
		MaintenanceRequestDto request = new MaintenanceRequestDto(4L, "Scheduled brake inspection",
				LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 3), new BigDecimal("180.00"));

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"vehicleId\":4"));
		assertTrue(json.contains("\"description\":\"Scheduled brake inspection\""));
		assertTrue(json.contains("\"startDate\":\"2027-09-01\""),
				"LocalDate must serialize as an ISO-8601 string, not a numeric timestamp");
		assertTrue(json.contains("\"endDate\":\"2027-09-03\""));
		assertTrue(json.contains("\"cost\":180.00"));
		assertFalse(json.contains("\"id\""), "request must not carry an id");
		assertFalse(json.contains("\"status\""), "request must not carry a status");
	}
}
