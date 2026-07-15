package be.condorcet.easycarrent.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.condorcet.easycarrent.entity.Customer;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Customer newCustomer(String email, String license) {
        return new Customer("John", "Doe", email, "+32 470 12 34 56",
                "Rue de la Loi 16, Brussels", license, LocalDate.now().plusYears(3));
    }

    @Test
    void persistsAndRetrievesCustomer() {
        Customer saved = customerRepository.save(newCustomer("john.doe@example.com", "BE1234567"));

        Optional<Customer> found = customerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void findsByEmailCaseInsensitively() {
        customerRepository.saveAndFlush(newCustomer("jane.doe@example.com", "BE7654321"));

        assertThat(customerRepository.findByEmailIgnoreCase("JANE.DOE@EXAMPLE.COM")).isPresent();
        assertThat(customerRepository.findByEmailIgnoreCase("jane.doe@example.com")).isPresent();
    }

    @Test
    void existsByEmailIsCaseInsensitive() {
        customerRepository.saveAndFlush(newCustomer("exist@example.com", "BE1111111"));

        assertThat(customerRepository.existsByEmailIgnoreCase("EXIST@example.com")).isTrue();
        assertThat(customerRepository.existsByEmailIgnoreCase("missing@example.com")).isFalse();
    }

    @Test
    void rejectsDuplicateEmail() {
        customerRepository.saveAndFlush(newCustomer("dup@example.com", "BE2222222"));

        assertThatThrownBy(() ->
                customerRepository.saveAndFlush(newCustomer("dup@example.com", "BE3333333")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsByDrivingLicenseCaseInsensitively() {
        customerRepository.saveAndFlush(newCustomer("lic@example.com", "be-abc-1"));

        assertThat(customerRepository.findByDrivingLicenseNumberIgnoreCase("BE-ABC-1")).isPresent();
        assertThat(customerRepository.findByDrivingLicenseNumberIgnoreCase("be-abc-1")).isPresent();
    }

    @Test
    void existsByDrivingLicenseIsCaseInsensitive() {
        customerRepository.saveAndFlush(newCustomer("lic2@example.com", "LIC-9"));

        assertThat(customerRepository.existsByDrivingLicenseNumberIgnoreCase("lic-9")).isTrue();
        assertThat(customerRepository.existsByDrivingLicenseNumberIgnoreCase("lic-0")).isFalse();
    }

    @Test
    void rejectsDuplicateDrivingLicenseNumber() {
        customerRepository.saveAndFlush(newCustomer("a@example.com", "SAME-LIC"));

        assertThatThrownBy(() ->
                customerRepository.saveAndFlush(newCustomer("b@example.com", "SAME-LIC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullRequiredEmail() {
        Customer invalid = new Customer("John", "Doe", null, "+32 470 12 34 56",
                "Rue de la Loi 16", "BE9999999", LocalDate.now().plusYears(2));

        assertThatThrownBy(() -> customerRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsDrivingLicenseExpiryDate() {
        LocalDate expiry = LocalDate.now().plusYears(4);
        Customer saved = customerRepository.saveAndFlush(
                new Customer("Amara", "Diallo", "amara@example.com", "+212 6 12 34 56 78",
                        "Avenue Hassan II, Casablanca", "MA-556677", expiry));

        entityManager.clear();
        Customer reloaded = customerRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDrivingLicenseExpiryDate()).isEqualTo(expiry);
    }
}
