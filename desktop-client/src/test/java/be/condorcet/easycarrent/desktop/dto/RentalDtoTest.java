package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class RentalDtoTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	private static final String SAMPLE_JSON = """
			{"id":9,"startDate":"2026-09-01","endDate":"2026-09-04","status":"PLANNED",
			 "totalPrice":135.00,"createdAt":"2026-08-01T10:15:30",
			 "vehicleId":4,"vehicleRegistrationNumber":"TEST-REG-001","vehicleBrand":"TestBrand",
			 "vehicleModel":"TestModel","customerId":3,"customerFirstName":"Test",
			 "customerLastName":"Customer"}
			""";

	@Test
	void deserializesResponseWithAllFields() throws Exception {
		RentalResponseDto dto = objectMapper.readValue(SAMPLE_JSON, RentalResponseDto.class);

		assertEquals(9L, dto.id());
		assertEquals(LocalDate.of(2026, 9, 1), dto.startDate());
		assertEquals(LocalDate.of(2026, 9, 4), dto.endDate());
		assertEquals(RentalStatus.PLANNED, dto.status());
		assertEquals(0, new BigDecimal("135.00").compareTo(dto.totalPrice()));
		assertEquals(LocalDateTime.of(2026, 8, 1, 10, 15, 30), dto.createdAt());
		assertEquals(4L, dto.vehicleId());
		assertEquals("TEST-REG-001", dto.vehicleRegistrationNumber());
		assertEquals("TestBrand", dto.vehicleBrand());
		assertEquals("TestModel", dto.vehicleModel());
		assertEquals(3L, dto.customerId());
		assertEquals("Test", dto.customerFirstName());
		assertEquals("Customer", dto.customerLastName());
	}

	@Test
	void mapsEveryRentalStatusValue() throws Exception {
		for (RentalStatus status : RentalStatus.values()) {
			String json = SAMPLE_JSON.replace("\"PLANNED\"", "\"" + status.name() + "\"");
			assertEquals(status, objectMapper.readValue(json, RentalResponseDto.class).status());
		}
	}

	@Test
	void ignoresUnknownResponseProperties() throws Exception {
		String json = SAMPLE_JSON.replaceFirst("\\{", "{\"futureField\":\"ignored\",");
		RentalResponseDto dto = objectMapper.readValue(json, RentalResponseDto.class);
		assertEquals(9L, dto.id());
	}

	@Test
	void deserializesRentalListAndEmptyList() throws Exception {
		List<RentalResponseDto> list = objectMapper.readValue("[" + SAMPLE_JSON + "]",
				objectMapper.getTypeFactory().constructCollectionType(List.class, RentalResponseDto.class));
		assertEquals(1, list.size());

		List<RentalResponseDto> empty = objectMapper.readValue("[]", objectMapper.getTypeFactory()
				.constructCollectionType(List.class, RentalResponseDto.class));
		assertTrue(empty.isEmpty());
	}

	@Test
	void serializesRequestWithOnlyPeriodAndReferences() throws Exception {
		RentalRequestDto request =
				new RentalRequestDto(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4), 4L, 3L);

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"startDate\":\"2026-09-01\""),
				"LocalDate must serialize as an ISO-8601 string, not a numeric timestamp");
		assertTrue(json.contains("\"endDate\":\"2026-09-04\""));
		assertTrue(json.contains("\"vehicleId\":4"));
		assertTrue(json.contains("\"customerId\":3"));
		assertFalse(json.contains("\"id\""), "request must not carry an id");
		assertFalse(json.contains("\"status\""), "request must not carry a status");
		assertFalse(json.contains("\"totalPrice\""), "request must not carry a total price");
		assertFalse(json.contains("\"createdAt\""), "request must not carry a creation timestamp");
	}
}
