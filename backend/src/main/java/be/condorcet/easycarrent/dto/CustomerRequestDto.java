package be.condorcet.easycarrent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request payload used to create or update a {@code Customer}.
 */
public record CustomerRequestDto(

        @NotBlank(message = "firstName is required")
        @Size(max = 60, message = "firstName must be at most 60 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 60, message = "lastName must be at most 60 characters")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 120, message = "email must be at most 120 characters")
        String email,

        @NotBlank(message = "phone is required")
        @Pattern(
                regexp = "^(?=.*[0-9])\\+?[0-9 ()\\-]{6,20}$",
                message = "phone must contain digits and may include spaces, parentheses, hyphens and an optional leading +")
        String phone,

        @NotBlank(message = "address is required")
        @Size(max = 255, message = "address must be at most 255 characters")
        String address,

        @NotBlank(message = "drivingLicenseNumber is required")
        @Size(max = 40, message = "drivingLicenseNumber must be at most 40 characters")
        String drivingLicenseNumber,

        @NotNull(message = "drivingLicenseExpiryDate is required")
        @FutureOrPresent(message = "drivingLicenseExpiryDate must not be in the past")
        LocalDate drivingLicenseExpiryDate
) {
}
