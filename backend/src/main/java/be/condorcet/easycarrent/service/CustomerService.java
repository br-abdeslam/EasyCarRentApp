package be.condorcet.easycarrent.service;

import be.condorcet.easycarrent.dto.CustomerRequestDto;
import be.condorcet.easycarrent.dto.CustomerResponseDto;
import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.CustomerMapper;
import be.condorcet.easycarrent.repository.CustomerRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing customers, including case-insensitive uniqueness
 * of email and driving licence number.
 */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final CustomerMapper mapper;

    public CustomerService(CustomerRepository customerRepository,
                           RentalRepository rentalRepository,
                           CustomerMapper mapper) {
        this.customerRepository = customerRepository;
        this.rentalRepository = rentalRepository;
        this.mapper = mapper;
    }

    public List<CustomerResponseDto> findAll() {
        return customerRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CustomerResponseDto findById(Long id) {
        return mapper.toResponse(getCustomer(id));
    }

    @Transactional
    public CustomerResponseDto create(CustomerRequestDto request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException(
                    "A customer with email '" + request.email() + "' already exists");
        }
        if (customerRepository.existsByDrivingLicenseNumberIgnoreCase(request.drivingLicenseNumber())) {
            throw new DuplicateResourceException(
                    "A customer with driving licence number '" + request.drivingLicenseNumber()
                            + "' already exists");
        }
        Customer saved = customerRepository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto update(Long id, CustomerRequestDto request) {
        Customer customer = getCustomer(id);

        customerRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A customer with email '" + request.email() + "' already exists");
                });

        customerRepository.findByDrivingLicenseNumberIgnoreCase(request.drivingLicenseNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A customer with driving licence number '" + request.drivingLicenseNumber()
                                    + "' already exists");
                });

        mapper.updateEntity(customer, request);
        return mapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getCustomer(id);
        if (rentalRepository.existsByCustomer_Id(id)) {
            throw new ResourceConflictException(
                    "Customer " + id + " cannot be deleted while it has rentals");
        }
        customerRepository.delete(customer);
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }
}
