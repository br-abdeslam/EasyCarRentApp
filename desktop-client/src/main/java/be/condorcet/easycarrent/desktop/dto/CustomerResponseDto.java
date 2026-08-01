package be.condorcet.easycarrent.desktop.dto;

import java.time.LocalDate;

/**
 * Client-side representation of a customer returned by the backend.
 *
 * <p>Mirrors the backend response contract exactly. It carries no rentals and no
 * credentials. The driving-licence expiry is a {@link LocalDate}.</p>
 *
 * @param id                       the customer identifier
 * @param firstName                the first name
 * @param lastName                 the last name
 * @param email                    the email address
 * @param phone                    the phone number
 * @param address                  the postal address
 * @param drivingLicenseNumber     the driving-licence number
 * @param drivingLicenseExpiryDate the driving-licence expiry date
 */
public record CustomerResponseDto(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone,
		String address,
		String drivingLicenseNumber,
		LocalDate drivingLicenseExpiryDate) {
}
