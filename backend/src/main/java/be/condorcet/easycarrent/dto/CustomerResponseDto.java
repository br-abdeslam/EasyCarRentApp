package be.condorcet.easycarrent.dto;

import java.time.LocalDate;

/**
 * Response payload exposing a {@code Customer} to API consumers.
 */
public record CustomerResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String drivingLicenseNumber,
        LocalDate drivingLicenseExpiryDate
) {
}
