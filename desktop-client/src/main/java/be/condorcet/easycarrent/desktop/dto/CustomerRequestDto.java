package be.condorcet.easycarrent.desktop.dto;

import java.time.LocalDate;

/**
 * Request payload sent to the backend to create or update a customer.
 *
 * <p>Mirrors the backend request contract exactly. It carries no identifier (the
 * id is in the request path) and no credentials. The driving-licence expiry is a
 * {@link LocalDate}.</p>
 *
 * @param firstName                the first name (required)
 * @param lastName                 the last name (required)
 * @param email                    the email address (required)
 * @param phone                    the phone number (required)
 * @param address                  the postal address (required)
 * @param drivingLicenseNumber     the driving-licence number (required)
 * @param drivingLicenseExpiryDate the driving-licence expiry date (required)
 */
public record CustomerRequestDto(
		String firstName,
		String lastName,
		String email,
		String phone,
		String address,
		String drivingLicenseNumber,
		LocalDate drivingLicenseExpiryDate) {
}
