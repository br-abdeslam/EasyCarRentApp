package be.condorcet.easycarrent.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
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
class RentalRepositoryTest {

    /** Statuses the service layer will later treat as blocking. */
    private static final List<RentalStatus> BLOCKING =
            List.of(RentalStatus.PLANNED, RentalStatus.ACTIVE);

    private static final LocalDate EXISTING_START = LocalDate.of(2026, 6, 10);
    private static final LocalDate EXISTING_END = LocalDate.of(2026, 6, 20);

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

    private Vehicle persistVehicle() {
        sequence++;
        VehicleCategory category = categoryRepository.saveAndFlush(new VehicleCategory("Cat-" + sequence, null));
        return vehicleRepository.saveAndFlush(new Vehicle(
                "REG-" + sequence, "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, category));
    }

    private Customer persistCustomer() {
        sequence++;
        return customerRepository.saveAndFlush(new Customer(
                "John", "Doe", "customer" + sequence + "@example.com", "+32 470 12 34 56",
                "Rue de la Loi 16", "LIC-" + sequence, LocalDate.of(2030, 1, 1)));
    }

    private Rental persistRental(Vehicle vehicle, Customer customer,
                                 LocalDate start, LocalDate end, RentalStatus status) {
        return rentalRepository.saveAndFlush(
                new Rental(vehicle, customer, start, end, new BigDecimal("100.00"), status));
    }

    /** Persists one existing rental on a fresh vehicle and returns that vehicle. */
    private Vehicle vehicleWithExistingRental(LocalDate start, LocalDate end, RentalStatus status) {
        Vehicle vehicle = persistVehicle();
        persistRental(vehicle, persistCustomer(), start, end, status);
        return vehicle;
    }

    // ---------------------------------------------------------------------
    // Persistence and relationships
    // ---------------------------------------------------------------------

    @Test
    void persistsAndRetrievesRental() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();

        Rental saved = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        Optional<Rental> found = rentalRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStartDate()).isEqualTo(EXISTING_START);
        assertThat(found.get().getEndDate()).isEqualTo(EXISTING_END);
    }

    @Test
    void persistsCustomerRelationship() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental saved = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        entityManager.clear();

        Rental reloaded = rentalRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCustomer().getId()).isEqualTo(customer.getId());
    }

    @Test
    void persistsVehicleRelationship() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental saved = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        entityManager.clear();

        Rental reloaded = rentalRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVehicle().getId()).isEqualTo(vehicle.getId());
    }

    @Test
    void defaultsStatusToPlannedWhenNotSupplied() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();

        Rental saved = rentalRepository.saveAndFlush(
                new Rental(vehicle, customer, EXISTING_START, EXISTING_END, new BigDecimal("100.00")));

        assertThat(saved.getStatus()).isEqualTo(RentalStatus.PLANNED);
    }

    @Test
    void preservesExplicitStatus() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental saved = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.ACTIVE);

        entityManager.clear();

        Rental reloaded = rentalRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void persistsBigDecimalTotalPrice() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental saved = rentalRepository.saveAndFlush(new Rental(
                vehicle, customer, EXISTING_START, EXISTING_END, new BigDecimal("1234.56")));

        entityManager.clear();

        Rental reloaded = rentalRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTotalPrice()).isEqualByComparingTo("1234.56");
    }

    @Test
    void populatesCreatedAtOnPersist() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();

        Rental saved = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createdAtRemainsNonNullAfterReload() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental saved = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        entityManager.clear();

        Rental reloaded = rentalRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsMissingCustomer() {
        Vehicle vehicle = persistVehicle();
        Rental invalid = new Rental(vehicle, null, EXISTING_START, EXISTING_END, new BigDecimal("100.00"));

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingVehicle() {
        Customer customer = persistCustomer();
        Rental invalid = new Rental(null, customer, EXISTING_START, EXISTING_END, new BigDecimal("100.00"));

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------------
    // Lookup methods
    // ---------------------------------------------------------------------

    @Test
    void findAllByCustomerIdReturnsMatchingRentals() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Customer other = persistCustomer();
        Rental mine = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);
        persistRental(vehicle, other, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), RentalStatus.PLANNED);

        List<Rental> result = rentalRepository.findAllByCustomer_Id(customer.getId());

        assertThat(result).extracting(Rental::getId).containsExactly(mine.getId());
    }

    @Test
    void findAllByVehicleIdReturnsMatchingRentals() {
        Vehicle vehicle = persistVehicle();
        Vehicle other = persistVehicle();
        Customer customer = persistCustomer();
        Rental mine = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);
        persistRental(other, customer, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), RentalStatus.PLANNED);

        List<Rental> result = rentalRepository.findAllByVehicle_Id(vehicle.getId());

        assertThat(result).extracting(Rental::getId).containsExactly(mine.getId());
    }

    @Test
    void findAllByStatusReturnsMatchingRentals() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental completed = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.COMPLETED);
        persistRental(vehicle, customer, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), RentalStatus.PLANNED);

        List<Rental> result = rentalRepository.findAllByStatus(RentalStatus.COMPLETED);

        assertThat(result).extracting(Rental::getId).containsExactly(completed.getId());
    }

    @Test
    void existsByCustomerId() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        assertThat(rentalRepository.existsByCustomer_Id(customer.getId())).isTrue();
        assertThat(rentalRepository.existsByCustomer_Id(-1L)).isFalse();
    }

    @Test
    void existsByVehicleId() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        assertThat(rentalRepository.existsByVehicle_Id(vehicle.getId())).isTrue();
        assertThat(rentalRepository.existsByVehicle_Id(-1L)).isFalse();
    }

    // ---------------------------------------------------------------------
    // Overlap detection (creation query). Existing rental: [Jun 10, Jun 20].
    // ---------------------------------------------------------------------

    @Test
    void detectsEqualPeriodOverlap() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsPartialOverlapFromLeft() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 12), BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsPartialOverlapFromRight() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 18), LocalDate.of(2026, 6, 25), BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsRequestedPeriodContainedInExisting() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 18), BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsRequestedPeriodContainingExisting() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsBoundaryOverlapWhenExistingEndEqualsRequestedStart() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        // requested starts exactly on the existing end date -> inclusive overlap
        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), EXISTING_END, LocalDate.of(2026, 6, 25), BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsBoundaryOverlapWhenExistingStartEqualsRequestedEnd() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        // requested ends exactly on the existing start date -> inclusive overlap
        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 1), EXISTING_START, BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void ignoresPeriodEntirelyBefore() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), BLOCKING);

        assertThat(count).isZero();
    }

    @Test
    void ignoresPeriodEntirelyAfter() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 30), BLOCKING);

        assertThat(count).isZero();
    }

    @Test
    void ignoresRentalForAnotherVehicle() {
        vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);
        Vehicle otherVehicle = persistVehicle();

        long count = rentalRepository.countOverlappingRentals(
                otherVehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isZero();
    }

    @Test
    void ignoresCancelledWhenBlockingPlannedAndActive() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.CANCELLED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isZero();
    }

    @Test
    void ignoresCompletedWhenBlockingPlannedAndActive() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.COMPLETED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isZero();
    }

    @Test
    void detectsActiveWhenActiveSupplied() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.ACTIVE);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void detectsPlannedWhenPlannedSupplied() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentals(
                vehicle.getId(), EXISTING_START, EXISTING_END, List.of(RentalStatus.PLANNED));

        assertThat(count).isEqualTo(1);
    }

    // ---------------------------------------------------------------------
    // Overlap detection (update query, excluding one rental id)
    // ---------------------------------------------------------------------

    @Test
    void updateQueryExcludesCurrentRentalById() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental current = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentalsExcludingRentalId(
                vehicle.getId(), current.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isZero();
    }

    @Test
    void updateQueryStillDetectsAnotherConflictingRental() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        Rental current = persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);
        persistRental(vehicle, customer, LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 18), RentalStatus.PLANNED);

        long count = rentalRepository.countOverlappingRentalsExcludingRentalId(
                vehicle.getId(), current.getId(), EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void updateQueryIgnoresRentalForAnotherVehicle() {
        Vehicle vehicleWithRental = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.PLANNED);
        Vehicle otherVehicle = persistVehicle();

        long count = rentalRepository.countOverlappingRentalsExcludingRentalId(
                otherVehicle.getId(), -1L, EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isZero();
        assertThat(vehicleWithRental.getId()).isNotEqualTo(otherVehicle.getId());
    }

    @Test
    void updateQueryIgnoresNonBlockingStatus() {
        Vehicle vehicle = vehicleWithExistingRental(EXISTING_START, EXISTING_END, RentalStatus.CANCELLED);

        long count = rentalRepository.countOverlappingRentalsExcludingRentalId(
                vehicle.getId(), -1L, EXISTING_START, EXISTING_END, BLOCKING);

        assertThat(count).isZero();
    }

    // ---------------------------------------------------------------------
    // Foreign-key protection: no cascade delete from Customer/Vehicle to Rental
    // ---------------------------------------------------------------------

    @Test
    void deletingReferencedCustomerIsBlockedAndDoesNotCascade() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        entityManager.flush();
        entityManager.clear();

        // No CascadeType.REMOVE exists, so the customer_id foreign key must block the
        // deletion of a customer still referenced by a rental. The thrown exception
        // proves both: the delete is rejected AND the rental was not cascade-deleted
        // (a cascade would have removed the rental and let the delete succeed).
        Long customerId = customer.getId();
        assertThatThrownBy(() -> {
            customerRepository.deleteById(customerId);
            customerRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingReferencedVehicleIsBlockedAndDoesNotCascade() {
        Vehicle vehicle = persistVehicle();
        Customer customer = persistCustomer();
        persistRental(vehicle, customer, EXISTING_START, EXISTING_END, RentalStatus.PLANNED);

        entityManager.flush();
        entityManager.clear();

        // Same reasoning as above for the vehicle_id foreign key.
        Long vehicleId = vehicle.getId();
        assertThatThrownBy(() -> {
            vehicleRepository.deleteById(vehicleId);
            vehicleRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
