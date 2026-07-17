package be.condorcet.easycarrent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.condorcet.easycarrent.dto.CustomerRequestDto;
import be.condorcet.easycarrent.dto.CustomerResponseDto;
import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.CustomerMapper;
import be.condorcet.easycarrent.repository.CustomerRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RentalRepository rentalRepository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, rentalRepository, new CustomerMapper());
    }

    private Customer customerWithId(Long id, String email, String license) {
        Customer customer = new Customer("John", "Doe", email, "+32 470 12 34 56",
                "Rue de la Loi 16", license, LocalDate.now().plusYears(2));
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private CustomerRequestDto request(String email, String license) {
        return new CustomerRequestDto("John", "Doe", email, "+32 470 12 34 56",
                "Rue de la Loi 16", license, LocalDate.now().plusYears(2));
    }

    @Test
    void findAllReturnsMappedCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(
                customerWithId(1L, "a@example.com", "L1"),
                customerWithId(2L, "b@example.com", "L2")));

        List<CustomerResponseDto> result = service.findAll();

        assertThat(result).extracting(CustomerResponseDto::email)
                .containsExactly("a@example.com", "b@example.com");
    }

    @Test
    void findByIdReturnsCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithId(1L, "a@example.com", "L1")));

        CustomerResponseDto result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("a@example.com");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPersistsWhenUnique() {
        when(customerRepository.existsByEmailIgnoreCase("a@example.com")).thenReturn(false);
        when(customerRepository.existsByDrivingLicenseNumberIgnoreCase("L1")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customerWithId(1L, "a@example.com", "L1"));

        CustomerResponseDto result = service.create(request("a@example.com", "L1"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("a@example.com");
    }

    @Test
    void createRejectsDuplicateEmail() {
        // Case-insensitive matching itself lives in the repository derived query
        // (covered by CustomerRepositoryTest); here we verify the service rejects
        // when the existence check reports a match.
        when(customerRepository.existsByEmailIgnoreCase("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("a@example.com", "L1")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateDrivingLicence() {
        when(customerRepository.existsByEmailIgnoreCase("a@example.com")).thenReturn(false);
        when(customerRepository.existsByDrivingLicenseNumberIgnoreCase("L1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("a@example.com", "L1")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateAppliesChanges() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithId(1L, "a@example.com", "L1")));
        when(customerRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByDrivingLicenseNumberIgnoreCase("L2")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponseDto result = service.update(1L, request("new@example.com", "L2"));

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.drivingLicenseNumber()).isEqualTo("L2");
    }

    @Test
    void updateAllowsUnchangedEmailAndLicenceOwnedBySelf() {
        Customer self = customerWithId(1L, "a@example.com", "L1");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(self));
        when(customerRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(self));
        when(customerRepository.findByDrivingLicenseNumberIgnoreCase("L1")).thenReturn(Optional.of(self));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponseDto result = service.update(1L, request("a@example.com", "L1"));

        assertThat(result.email()).isEqualTo("a@example.com");
    }

    @Test
    void updateRejectsEmailOwnedByAnotherCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithId(1L, "a@example.com", "L1")));
        when(customerRepository.findByEmailIgnoreCase("taken@example.com"))
                .thenReturn(Optional.of(customerWithId(2L, "taken@example.com", "L2")));

        assertThatThrownBy(() -> service.update(1L, request("taken@example.com", "L1")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateRejectsLicenceOwnedByAnotherCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithId(1L, "a@example.com", "L1")));
        when(customerRepository.findByEmailIgnoreCase("a@example.com"))
                .thenReturn(Optional.of(customerWithId(1L, "a@example.com", "L1")));
        when(customerRepository.findByDrivingLicenseNumberIgnoreCase("TAKEN"))
                .thenReturn(Optional.of(customerWithId(2L, "b@example.com", "TAKEN")));

        assertThatThrownBy(() -> service.update(1L, request("a@example.com", "TAKEN")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("a@example.com", "L1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesUnreferencedCustomer() {
        Customer customer = customerWithId(1L, "a@example.com", "L1");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(rentalRepository.existsByCustomer_Id(1L)).thenReturn(false);

        service.delete(1L);

        verify(rentalRepository).existsByCustomer_Id(1L);
        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteReferencedCustomerThrowsConflict() {
        Customer customer = customerWithId(1L, "a@example.com", "L1");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(rentalRepository.existsByCustomer_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceConflictException.class);
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
