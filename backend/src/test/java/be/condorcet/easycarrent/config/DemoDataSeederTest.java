package be.condorcet.easycarrent.config;

import static org.assertj.core.api.Assertions.assertThat;

import be.condorcet.easycarrent.dto.CustomerResponseDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.dto.RentalResponseDto;
import be.condorcet.easycarrent.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.repository.CustomerRepository;
import be.condorcet.easycarrent.repository.MaintenanceRecordRepository;
import be.condorcet.easycarrent.repository.PaymentRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import be.condorcet.easycarrent.repository.VehicleCategoryRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import be.condorcet.easycarrent.service.CustomerService;
import be.condorcet.easycarrent.service.MaintenanceRecordService;
import be.condorcet.easycarrent.service.PaymentService;
import be.condorcet.easycarrent.service.RentalService;
import be.condorcet.easycarrent.service.VehicleCategoryService;
import be.condorcet.easycarrent.service.VehicleService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for {@link DemoDataSeeder} against an in-memory database under
 * the {@code demo} profile. Each test starts from an explicitly emptied database so
 * the seeder behavior is verified in isolation from the startup runner.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seeder-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@ActiveProfiles("demo")
class DemoDataSeederTest {

    @Autowired private DemoDataSeeder seeder;
    @Autowired private ApplicationContext context;

    @Autowired private VehicleCategoryService categoryService;
    @Autowired private VehicleService vehicleService;
    @Autowired private CustomerService customerService;
    @Autowired private RentalService rentalService;
    @Autowired private PaymentService paymentService;
    @Autowired private MaintenanceRecordService maintenanceService;

    @Autowired private VehicleCategoryRepository categoryRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private RentalRepository rentalRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MaintenanceRecordRepository maintenanceRepository;

    @BeforeEach
    void clearDatabase() {
        paymentRepository.deleteAll();
        maintenanceRepository.deleteAll();
        rentalRepository.deleteAll();
        customerRepository.deleteAll();
        vehicleRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void demoProfileRegistersTheStartupInitializer() {
        assertThat(context.getBeanNamesForType(DemoDataInitializer.class))
                .as("the demo profile must register the startup initializer")
                .hasSize(1);
        assertThat(context.getBeanNamesForType(DemoDataSeeder.class)).hasSize(1);
    }

    @Test
    void emptyDatabaseReceivesRepresentativeDataForEveryDomain() {
        seeder.seed();

        assertThat(categoryService.findAll()).hasSize(4);
        assertThat(vehicleService.findAll()).hasSize(8);
        assertThat(customerService.findAll()).hasSize(6);
        assertThat(rentalService.findAll()).hasSize(6);
        assertThat(paymentService.findAll()).hasSize(4);
        assertThat(maintenanceService.findAll()).hasSize(3);
    }

    @Test
    void seededStatusesCoverTheDemonstrationStates() {
        seeder.seed();

        assertThat(vehicleService.findAll()).extracting(VehicleResponseDto::status)
                .filteredOn(status -> status == VehicleStatus.RENTED).hasSize(1);
        assertThat(vehicleService.findAll()).extracting(VehicleResponseDto::status)
                .filteredOn(status -> status == VehicleStatus.MAINTENANCE).hasSize(1);
        assertThat(vehicleService.findAll()).extracting(VehicleResponseDto::status)
                .noneMatch(status -> status == VehicleStatus.INACTIVE);

        assertThat(rentalService.findAll()).extracting(RentalResponseDto::status)
                .contains(RentalStatus.PLANNED, RentalStatus.ACTIVE,
                        RentalStatus.COMPLETED, RentalStatus.CANCELLED);

        assertThat(paymentService.findAll()).extracting(PaymentResponseDto::status)
                .containsExactlyInAnyOrder(PaymentStatus.PAID, PaymentStatus.REFUNDED,
                        PaymentStatus.FAILED, PaymentStatus.PENDING);

        assertThat(maintenanceService.findAll()).extracting(MaintenanceRecordResponseDto::status)
                .containsExactlyInAnyOrder(MaintenanceStatus.PLANNED,
                        MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.COMPLETED);
    }

    @Test
    void multipleVehiclesRemainAvailableForInteractiveUse() {
        seeder.seed();

        long available = vehicleService.findAll().stream()
                .filter(vehicle -> vehicle.status() == VehicleStatus.AVAILABLE)
                .count();
        assertThat(available)
                .as("several vehicles must stay available for live demonstration")
                .isGreaterThanOrEqualTo(2);

        // A disposable planned rental and planned maintenance record remain for interaction.
        assertThat(rentalService.findAll()).extracting(RentalResponseDto::status)
                .contains(RentalStatus.PLANNED);
        assertThat(maintenanceService.findAll()).extracting(MaintenanceRecordResponseDto::status)
                .contains(MaintenanceStatus.PLANNED);
    }

    @Test
    void relationshipsReferenceRecordsThatExist() {
        seeder.seed();

        Set<Long> vehicleIds = vehicleService.findAll().stream()
                .map(VehicleResponseDto::id).collect(Collectors.toSet());
        Set<Long> customerIds = customerService.findAll().stream()
                .map(CustomerResponseDto::id).collect(Collectors.toSet());
        Set<Long> rentalIds = rentalService.findAll().stream()
                .map(RentalResponseDto::id).collect(Collectors.toSet());

        assertThat(rentalService.findAll()).allSatisfy(rental -> {
            assertThat(vehicleIds).contains(rental.vehicleId());
            assertThat(customerIds).contains(rental.customerId());
        });
        assertThat(paymentService.findAll()).allSatisfy(payment -> {
            assertThat(rentalIds).contains(payment.rentalId());
            assertThat(payment.rentalStatus()).isIn(RentalStatus.ACTIVE, RentalStatus.COMPLETED);
        });
        assertThat(maintenanceService.findAll()).allSatisfy(record ->
                assertThat(vehicleIds).contains(record.vehicleId()));
    }

    @Test
    void seedIntroducesNoOverlappingBlockingRentalsOnAnyVehicle() {
        seeder.seed();

        List<RentalResponseDto> blocking = rentalService.findAll().stream()
                .filter(rental -> rental.status() == RentalStatus.PLANNED
                        || rental.status() == RentalStatus.ACTIVE)
                .toList();

        for (RentalResponseDto first : blocking) {
            for (RentalResponseDto second : blocking) {
                if (first.id().equals(second.id()) || !first.vehicleId().equals(second.vehicleId())) {
                    continue;
                }
                boolean overlap = !first.startDate().isAfter(second.endDate())
                        && !first.endDate().isBefore(second.startDate());
                assertThat(overlap)
                        .as("blocking rentals on the same vehicle must not overlap")
                        .isFalse();
            }
        }
    }

    @Test
    void secondSeedOnAnAlreadySeededDatabaseCreatesNoDuplicates() {
        seeder.seed();
        int categories = categoryService.findAll().size();
        int vehicles = vehicleService.findAll().size();
        int customers = customerService.findAll().size();
        int rentals = rentalService.findAll().size();
        int payments = paymentService.findAll().size();
        int maintenance = maintenanceService.findAll().size();

        seeder.seed();

        assertThat(categoryService.findAll()).hasSize(categories);
        assertThat(vehicleService.findAll()).hasSize(vehicles);
        assertThat(customerService.findAll()).hasSize(customers);
        assertThat(rentalService.findAll()).hasSize(rentals);
        assertThat(paymentService.findAll()).hasSize(payments);
        assertThat(maintenanceService.findAll()).hasSize(maintenance);
    }

    @Test
    void nonEmptyDatabaseIsNeverOverwritten() {
        // A single pre-existing record must make the seeder skip completely.
        categoryService.create(new VehicleCategoryRequestDto("Existing", "A pre-existing category."));

        seeder.seed();

        assertThat(categoryService.findAll()).hasSize(1);
        assertThat(categoryService.findAll().get(0).name()).isEqualTo("Existing");
        assertThat(vehicleService.findAll()).isEmpty();
        assertThat(customerService.findAll()).isEmpty();
        assertThat(rentalService.findAll()).isEmpty();
        assertThat(paymentService.findAll()).isEmpty();
        assertThat(maintenanceService.findAll()).isEmpty();
    }

    @Test
    void seededIdentifiersAreObviouslyFictional() {
        seeder.seed();

        assertThat(vehicleService.findAll()).allSatisfy(vehicle ->
                assertThat(vehicle.registrationNumber()).matches("DEMO-\\d+"));
        assertThat(customerService.findAll()).allSatisfy(customer -> {
            assertThat(customer.email()).endsWith("@example.invalid");
            assertThat(customer.drivingLicenseNumber()).matches("DEMO-LIC-\\d+");
        });
    }

    @Test
    void seedExposesNoCredentialLikeValues() {
        seeder.seed();

        assertThat(customerService.findAll()).allSatisfy(customer -> {
            String joined = (customer.firstName() + customer.lastName() + customer.email()
                    + customer.phone() + customer.address() + customer.drivingLicenseNumber())
                    .toLowerCase();
            assertThat(joined)
                    .doesNotContain("password")
                    .doesNotContain("secret")
                    .doesNotContain("token")
                    .doesNotContain("authorization");
        });
    }
}
