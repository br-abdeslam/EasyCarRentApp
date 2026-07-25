package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class CustomerDtoTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	@Test
	void deserializesResponseWithAllFields() throws Exception {
		String json = """
				{"id":7,"firstName":"Test","lastName":"Customer",
				 "email":"test.customer@example.invalid","phone":"+0000000000",
				 "address":"1 Example Street","drivingLicenseNumber":"TEST-LICENCE-001",
				 "drivingLicenseExpiryDate":"2030-01-15"}
				""";

		CustomerResponseDto dto = objectMapper.readValue(json, CustomerResponseDto.class);

		assertEquals(7L, dto.id());
		assertEquals("Test", dto.firstName());
		assertEquals("Customer", dto.lastName());
		assertEquals("test.customer@example.invalid", dto.email());
		assertEquals("+0000000000", dto.phone());
		assertEquals("1 Example Street", dto.address());
		assertEquals("TEST-LICENCE-001", dto.drivingLicenseNumber());
		assertEquals(LocalDate.of(2030, 1, 15), dto.drivingLicenseExpiryDate());
	}

	@Test
	void ignoresUnknownResponseProperties() throws Exception {
		String json = """
				{"id":1,"firstName":"A","lastName":"B","email":"a@b.invalid","phone":"000000",
				 "address":"x","drivingLicenseNumber":"L","drivingLicenseExpiryDate":"2031-05-01",
				 "futureField":"ignored"}
				""";

		CustomerResponseDto dto = objectMapper.readValue(json, CustomerResponseDto.class);

		assertEquals("A", dto.firstName());
	}

	@Test
	void deserializesCustomerListAndEmptyList() throws Exception {
		String json = """
				[{"id":1,"firstName":"A","lastName":"B","email":"a@b.invalid","phone":"000000",
				  "address":"x","drivingLicenseNumber":"L","drivingLicenseExpiryDate":"2031-05-01"}]
				""";

		List<CustomerResponseDto> list = objectMapper.readValue(json, objectMapper.getTypeFactory()
				.constructCollectionType(List.class, CustomerResponseDto.class));
		assertEquals(1, list.size());

		List<CustomerResponseDto> empty = objectMapper.readValue("[]", objectMapper.getTypeFactory()
				.constructCollectionType(List.class, CustomerResponseDto.class));
		assertTrue(empty.isEmpty());
	}

	@Test
	void serializesRequestWithoutIdAndDateAsIso() throws Exception {
		CustomerRequestDto request = new CustomerRequestDto(
				"Test", "Customer", "test.customer@example.invalid", "+0000000000",
				"1 Example Street", "TEST-LICENCE-001", LocalDate.of(2030, 1, 15));

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"firstName\":\"Test\""));
		assertTrue(json.contains("\"drivingLicenseExpiryDate\":\"2030-01-15\""),
				"LocalDate must serialize as an ISO-8601 string, not a numeric timestamp");
		assertFalse(json.contains("\"id\""), "request must not carry an id");
	}
}
