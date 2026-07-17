package be.condorcet.easycarrent.dto;

import be.condorcet.easycarrent.entity.RentalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload exposing a {@code Rental} to API consumers.
 *
 * <p>The vehicle and customer are flattened into a few safe identifying fields
 * to avoid serializing JPA relationships. Sensitive customer data (address,
 * phone, email, driving licence) is intentionally excluded.
 */
public record RentalResponseDto(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        RentalStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        Long vehicleId,
        String vehicleRegistrationNumber,
        String vehicleBrand,
        String vehicleModel,
        Long customerId,
        String customerFirstName,
        String customerLastName
) {
}
