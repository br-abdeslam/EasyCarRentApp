package be.condorcet.easycarrent.mapper;

import be.condorcet.easycarrent.dto.CustomerRequestDto;
import be.condorcet.easycarrent.dto.CustomerResponseDto;
import be.condorcet.easycarrent.entity.Customer;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Customer} entities and their DTOs. Stateless and
 * without repository access.
 */
@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequestDto request) {
        return new Customer(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.address(),
                request.drivingLicenseNumber(),
                request.drivingLicenseExpiryDate());
    }

    /**
     * Applies the editable request fields onto an existing customer. The
     * identifier is never modified.
     */
    public void updateEntity(Customer customer, CustomerRequestDto request) {
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        customer.setDrivingLicenseNumber(request.drivingLicenseNumber());
        customer.setDrivingLicenseExpiryDate(request.drivingLicenseExpiryDate());
    }

    public CustomerResponseDto toResponse(Customer customer) {
        return new CustomerResponseDto(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getDrivingLicenseNumber(),
                customer.getDrivingLicenseExpiryDate());
    }
}
