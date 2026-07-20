package be.condorcet.easycarrent.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.entity.Payment;
import be.condorcet.easycarrent.entity.PaymentMethod;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** Distinct suffix so unique columns (registration, email, licence, name) never clash. */
    private int sequence = 0;

    /** Persists a full Category -> Vehicle -> Customer -> Rental graph and returns the rental. */
    private Rental persistRental() {
        sequence++;
        VehicleCategory category = categoryRepository.saveAndFlush(new VehicleCategory("Cat-" + sequence, null));
        Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(
                "REG-" + sequence, "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, category));
        Customer customer = customerRepository.saveAndFlush(new Customer(
                "John", "Doe", "customer" + sequence + "@example.com", "+32 470 12 34 56",
                "Rue de la Loi 16", "LIC-" + sequence, LocalDate.of(2030, 1, 1)));
        return rentalRepository.saveAndFlush(new Rental(
                vehicle, customer, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                new BigDecimal("100.00")));
    }

    private Payment newPayment(Rental rental, PaymentMethod method, String amount) {
        return new Payment(rental, method, new BigDecimal(amount));
    }

    // ---------------------------------------------------------------------
    // Persistence and relationship
    // ---------------------------------------------------------------------

    @Test
    void persistsAndReloadsPayment() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CARD, "150.00"));

        entityManager.clear();

        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("150.00");
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void keepsRentalRelationship() {
        Rental rental = persistRental();
        Payment saved = paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CARD, "150.00"));

        entityManager.clear();

        Payment reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRental().getId()).isEqualTo(rental.getId());
    }

    @Test
    void roundTripsBigDecimalAmount() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.BANK_TRANSFER, "1234.56"));

        entityManager.clear();

        Payment reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAmount()).isEqualByComparingTo("1234.56");
    }

    @Test
    void persistsPaymentMethod() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.BANK_TRANSFER, "80.00"));

        entityManager.clear();

        Payment reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
    }

    @Test
    void defaultsStatusToPending() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CASH, "60.00"));

        entityManager.clear();

        Payment reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void initializesCreatedAtOnPersist() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CARD, "60.00"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createdAtRemainsPopulatedAfterReload() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CARD, "60.00"));

        entityManager.flush();
        entityManager.clear();

        Payment reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void paidAtIsInitiallyNull() {
        Payment saved = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CARD, "60.00"));

        entityManager.clear();

        Payment reloaded = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPaidAt()).isNull();
    }

    // ---------------------------------------------------------------------
    // Constraints
    // ---------------------------------------------------------------------

    @Test
    void rejectsMissingRental() {
        Payment invalid = newPayment(null, PaymentMethod.CARD, "60.00");

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateRentalId() {
        Rental rental = persistRental();
        paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CARD, "60.00"));

        assertThatThrownBy(() ->
                paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CASH, "70.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------------
    // Lookup methods
    // ---------------------------------------------------------------------

    @Test
    void findByRentalIdReturnsPayment() {
        Rental rental = persistRental();
        Payment saved = paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CARD, "60.00"));

        Optional<Payment> found = paymentRepository.findByRental_Id(rental.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByRentalIdReturnsEmptyWhenNoPayment() {
        Rental rental = persistRental();

        assertThat(paymentRepository.findByRental_Id(rental.getId())).isEmpty();
    }

    @Test
    void existsByRentalIdIsTrueWhenPaymentExists() {
        Rental rental = persistRental();
        paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CARD, "60.00"));

        assertThat(paymentRepository.existsByRental_Id(rental.getId())).isTrue();
    }

    @Test
    void existsByRentalIdIsFalseWhenNone() {
        Rental rental = persistRental();

        assertThat(paymentRepository.existsByRental_Id(rental.getId())).isFalse();
        assertThat(paymentRepository.existsByRental_Id(-1L)).isFalse();
    }

    @Test
    void findAllByStatusReturnsPendingPayments() {
        Payment p1 = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CARD, "60.00"));
        Payment p2 = paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CASH, "70.00"));

        List<Payment> pending = paymentRepository.findAllByStatus(PaymentStatus.PENDING);

        assertThat(pending).extracting(Payment::getId)
                .containsExactlyInAnyOrder(p1.getId(), p2.getId());
    }

    @Test
    void findAllByStatusReturnsEmptyForStatusWithNoRows() {
        paymentRepository.saveAndFlush(newPayment(persistRental(), PaymentMethod.CARD, "60.00"));

        assertThat(paymentRepository.findAllByStatus(PaymentStatus.FAILED)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Foreign-key protection
    // ---------------------------------------------------------------------

    @Test
    void deletingPaymentDoesNotDeleteRental() {
        Rental rental = persistRental();
        Payment payment = paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CARD, "60.00"));

        paymentRepository.delete(payment);
        paymentRepository.flush();
        entityManager.clear();

        assertThat(rentalRepository.findById(rental.getId())).isPresent();
    }

    @Test
    void deletingReferencedRentalIsBlockedAndDoesNotCascade() {
        Rental rental = persistRental();
        paymentRepository.saveAndFlush(newPayment(rental, PaymentMethod.CARD, "60.00"));

        entityManager.flush();
        entityManager.clear();

        // No cascade is configured, so the payments.rental_id foreign key must block
        // deleting a rental that still has a payment. The thrown exception proves both
        // that the delete is rejected AND that the payment was not cascade-deleted
        // (a cascade would have removed the payment and let the delete succeed).
        Long rentalId = rental.getId();
        assertThatThrownBy(() -> {
            rentalRepository.deleteById(rentalId);
            rentalRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
