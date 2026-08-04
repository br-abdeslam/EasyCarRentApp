package be.condorcet.easycarrent.desktop.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PaymentDtoTest {

	private final ObjectMapper objectMapper = JsonMapperFactory.create();

	private static final String SAMPLE_JSON = """
			{"id":8,"rentalId":4,"rentalStatus":"ACTIVE","amount":135.00,"paymentMethod":"CARD",
			 "status":"PAID","createdAt":"2026-08-01T10:15:30","paidAt":"2026-08-02T09:00:00"}
			""";

	@Test
	void deserializesResponseWithAllFields() throws Exception {
		PaymentResponseDto dto = objectMapper.readValue(SAMPLE_JSON, PaymentResponseDto.class);

		assertEquals(8L, dto.id());
		assertEquals(4L, dto.rentalId());
		assertEquals(RentalStatus.ACTIVE, dto.rentalStatus());
		assertEquals(0, new BigDecimal("135.00").compareTo(dto.amount()));
		assertEquals(PaymentMethod.CARD, dto.paymentMethod());
		assertEquals(PaymentStatus.PAID, dto.status());
		assertEquals(LocalDateTime.of(2026, 8, 1, 10, 15, 30), dto.createdAt());
		assertEquals(LocalDateTime.of(2026, 8, 2, 9, 0, 0), dto.paidAt());
	}

	@Test
	void deserializesResponseWithNullPaidAt() throws Exception {
		String json = SAMPLE_JSON.replace("\"paidAt\":\"2026-08-02T09:00:00\"", "\"paidAt\":null");
		PaymentResponseDto dto = objectMapper.readValue(json, PaymentResponseDto.class);
		assertEquals(PaymentStatus.PAID, dto.status());
		assertNull(dto.paidAt());
	}

	@Test
	void mapsEveryPaymentStatusValue() throws Exception {
		for (PaymentStatus status : PaymentStatus.values()) {
			String json = SAMPLE_JSON.replace("\"status\":\"PAID\"", "\"status\":\"" + status.name() + "\"");
			assertEquals(status, objectMapper.readValue(json, PaymentResponseDto.class).status());
		}
	}

	@Test
	void mapsEveryPaymentMethodValue() throws Exception {
		for (PaymentMethod method : PaymentMethod.values()) {
			String json = SAMPLE_JSON.replace("\"paymentMethod\":\"CARD\"",
					"\"paymentMethod\":\"" + method.name() + "\"");
			assertEquals(method, objectMapper.readValue(json, PaymentResponseDto.class).paymentMethod());
		}
	}

	@Test
	void ignoresUnknownResponseProperties() throws Exception {
		String json = SAMPLE_JSON.replaceFirst("\\{", "{\"futureField\":\"ignored\",");
		assertEquals(8L, objectMapper.readValue(json, PaymentResponseDto.class).id());
	}

	@Test
	void deserializesPaymentListAndEmptyList() throws Exception {
		List<PaymentResponseDto> list = objectMapper.readValue("[" + SAMPLE_JSON + "]",
				objectMapper.getTypeFactory().constructCollectionType(List.class, PaymentResponseDto.class));
		assertEquals(1, list.size());

		List<PaymentResponseDto> empty = objectMapper.readValue("[]", objectMapper.getTypeFactory()
				.constructCollectionType(List.class, PaymentResponseDto.class));
		assertTrue(empty.isEmpty());
	}

	@Test
	void serializesRequestWithOnlyRentalAndMethod() throws Exception {
		PaymentRequestDto request = new PaymentRequestDto(4L, PaymentMethod.BANK_TRANSFER);

		String json = objectMapper.writeValueAsString(request);

		assertTrue(json.contains("\"rentalId\":4"));
		assertTrue(json.contains("\"paymentMethod\":\"BANK_TRANSFER\""));
		assertFalse(json.contains("\"id\""), "request must not carry an id");
		assertFalse(json.contains("\"amount\""), "request must not carry an amount");
		assertFalse(json.contains("\"status\""), "request must not carry a status");
		assertFalse(json.contains("\"createdAt\""), "request must not carry a timestamp");
	}
}
