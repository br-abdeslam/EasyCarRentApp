package be.condorcet.easycarrent.config;

import be.condorcet.easycarrent.dto.CustomerRequestDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordRequestDto;
import be.condorcet.easycarrent.dto.PaymentRequestDto;
import be.condorcet.easycarrent.dto.RentalRequestDto;
import be.condorcet.easycarrent.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.dto.VehicleRequestDto;
import be.condorcet.easycarrent.entity.PaymentMethod;
import be.condorcet.easycarrent.service.CustomerService;
import be.condorcet.easycarrent.service.MaintenanceRecordService;
import be.condorcet.easycarrent.service.PaymentService;
import be.condorcet.easycarrent.service.RentalService;
import be.condorcet.easycarrent.service.VehicleCategoryService;
import be.condorcet.easycarrent.service.VehicleService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds a compact, coherent, entirely fictional demonstration dataset for a
 * fresh database.
 *
 * <p>The seeder is only registered under the {@code demo} profile. It runs the
 * whole insertion in one transaction (all-or-nothing) and only when every domain
 * table is empty, so it never overwrites, deletes or mixes with data that is
 * already present: a second startup against an already-seeded database simply
 * detects the non-empty state and skips.
 *
 * <p>Records are created through the existing domain services so that the same
 * validation, overlap rules, server-calculated rental price, server-derived
 * payment amount and vehicle-status synchronization apply to the demonstration
 * data as to any other request. The dataset intentionally leaves several vehicles
 * available and keeps a disposable planned rental and planned maintenance record
 * so the running application stays interactive.
 */
@Component
@Profile("demo")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final VehicleCategoryService categoryService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final RentalService rentalService;
    private final PaymentService paymentService;
    private final MaintenanceRecordService maintenanceService;
    private final Clock clock;

    public DemoDataSeeder(VehicleCategoryService categoryService,
                          VehicleService vehicleService,
                          CustomerService customerService,
                          RentalService rentalService,
                          PaymentService paymentService,
                          MaintenanceRecordService maintenanceService,
                          Clock clock) {
        this.categoryService = categoryService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
        this.rentalService = rentalService;
        this.paymentService = paymentService;
        this.maintenanceService = maintenanceService;
        this.clock = clock;
    }

    /**
     * Seeds the demonstration dataset when the database is empty; otherwise leaves
     * every record untouched. The whole operation runs in one transaction so a
     * failure midway leaves no partial dataset.
     */
    @Transactional
    public void seed() {
        if (!isDatabaseEmpty()) {
            log.info("Demo data initialization skipped because the database is not empty.");
            return;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate licenceExpiry = today.plusYears(5);

        // --- Categories -------------------------------------------------------
        Long economy = category("Economy",
                "Small, fuel-efficient cars for everyday city driving.");
        Long compact = category("Compact",
                "Comfortable mid-size cars for longer trips.");
        Long suv = category("SUV",
                "Spacious sport utility vehicles for families and rougher roads.");
        Long premium = category("Premium",
                "High-end vehicles with premium comfort and features.");

        // --- Vehicles (all start AVAILABLE) -----------------------------------
        Long v1 = vehicle("DEMO-001", "Nimbus", "City", 2021, "White", "35.00", 42000L, economy);
        Long v2 = vehicle("DEMO-002", "Nimbus", "City", 2020, "Blue", "32.00", 51000L, economy);
        Long v3 = vehicle("DEMO-003", "Corsair", "Cruise", 2022, "Grey", "45.00", 28000L, compact);
        Long v4 = vehicle("DEMO-004", "Corsair", "Cruise", 2021, "Black", "48.00", 33000L, compact);
        Long v5 = vehicle("DEMO-005", "Summit", "Trail", 2022, "Green", "70.00", 22000L, suv);
        Long v6 = vehicle("DEMO-006", "Summit", "Trail", 2023, "Silver", "75.00", 15000L, suv);
        Long v7 = vehicle("DEMO-007", "Regenta", "Prestige", 2023, "Black", "120.00", 8000L, premium);
        Long v8 = vehicle("DEMO-008", "Regenta", "Prestige", 2022, "White", "110.00", 12000L, premium);

        // --- Customers (obviously fictional) ----------------------------------
        Long alex = customer("Alex", "Demo", "alex.demo@example.invalid",
                "+0000000001", "1 Example Street, Demo City", "DEMO-LIC-001", licenceExpiry);
        Long blair = customer("Blair", "Sample", "blair.sample@example.invalid",
                "+0000000002", "2 Example Avenue, Demo City", "DEMO-LIC-002", licenceExpiry);
        Long casey = customer("Casey", "Fictional", "casey.fictional@example.invalid",
                "+0000000003", "3 Example Road, Demo City", "DEMO-LIC-003", licenceExpiry);
        Long dana = customer("Dana", "Placeholder", "dana.placeholder@example.invalid",
                "+0000000004", "4 Example Lane, Demo City", "DEMO-LIC-004", licenceExpiry);
        Long erin = customer("Erin", "Testcase", "erin.testcase@example.invalid",
                "+0000000005", "5 Example Boulevard, Demo City", "DEMO-LIC-005", licenceExpiry);
        // Finn is intentionally left free of any rental so it stays fully disposable.
        customer("Finn", "Spare", "finn.spare@example.invalid",
                "+0000000006", "6 Example Close, Demo City", "DEMO-LIC-006", licenceExpiry);

        // --- Rentals ----------------------------------------------------------
        // Completed history on vehicles that return to AVAILABLE afterwards.
        Long completedRental = completedRental(v3, alex, today.minusDays(20), today.minusDays(13));
        Long refundedRental = completedRental(v7, erin, today.minusDays(30), today.minusDays(25));
        Long failedRental = completedRental(v1, casey, today.minusDays(45), today.minusDays(40));

        // Active rental keeps its vehicle RENTED for the current status demonstration.
        Long activeRental = rentalService.create(new RentalRequestDto(
                today.minusDays(2), today.plusDays(5), v5, blair)).id();
        rentalService.start(activeRental);

        // Disposable planned rental (future period) and a cancelled rental.
        rentalService.create(new RentalRequestDto(today.plusDays(3), today.plusDays(8), v4, casey));
        Long cancelledRental = rentalService.create(new RentalRequestDto(
                today.plusDays(10), today.plusDays(14), v6, dana)).id();
        rentalService.cancel(cancelledRental);

        // --- Payments (only on payable ACTIVE/COMPLETED rentals) --------------
        Long paidPayment = paymentService.create(
                new PaymentRequestDto(completedRental, PaymentMethod.CARD)).id();
        paymentService.markPaid(paidPayment);

        Long refundedPayment = paymentService.create(
                new PaymentRequestDto(refundedRental, PaymentMethod.BANK_TRANSFER)).id();
        paymentService.markPaid(refundedPayment);
        paymentService.refund(refundedPayment);

        Long failedPayment = paymentService.create(
                new PaymentRequestDto(failedRental, PaymentMethod.CARD)).id();
        paymentService.markFailed(failedPayment);

        // Pending payment on the active rental.
        paymentService.create(new PaymentRequestDto(activeRental, PaymentMethod.CASH));

        // --- Maintenance ------------------------------------------------------
        // In-progress maintenance keeps its vehicle in MAINTENANCE.
        Long inProgressMaintenance = maintenanceService.create(new MaintenanceRecordRequestDto(
                v8, "Scheduled inspection and service.",
                today.minusDays(1), today.plusDays(2), new BigDecimal("150.00"))).id();
        maintenanceService.start(inProgressMaintenance);

        // Completed maintenance history; the vehicle returns to AVAILABLE.
        Long completedMaintenance = maintenanceService.create(new MaintenanceRecordRequestDto(
                v2, "Tyre replacement.",
                today.minusDays(10), today.minusDays(8), new BigDecimal("200.00"))).id();
        maintenanceService.start(completedMaintenance);
        maintenanceService.complete(completedMaintenance);

        // Disposable planned maintenance (future period) on an available vehicle.
        maintenanceService.create(new MaintenanceRecordRequestDto(
                v6, "Planned brake check.",
                today.plusDays(20), today.plusDays(22), new BigDecimal("90.00")));

        log.info("Demo data initialized successfully: {} categories, {} vehicles, {} customers, "
                        + "{} rentals, {} payments, {} maintenance records.",
                categoryService.findAll().size(),
                vehicleService.findAll().size(),
                customerService.findAll().size(),
                rentalService.findAll().size(),
                paymentService.findAll().size(),
                maintenanceService.findAll().size());
    }

    /**
     * Returns {@code true} only when every domain is empty. A single existing
     * record in any domain makes the seeder skip, protecting a non-empty
     * development database if the profile is enabled by accident.
     */
    private boolean isDatabaseEmpty() {
        return categoryService.findAll().isEmpty()
                && vehicleService.findAll().isEmpty()
                && customerService.findAll().isEmpty()
                && rentalService.findAll().isEmpty()
                && paymentService.findAll().isEmpty()
                && maintenanceService.findAll().isEmpty();
    }

    private Long category(String name, String description) {
        return categoryService.create(new VehicleCategoryRequestDto(name, description)).id();
    }

    private Long vehicle(String registration, String brand, String model, int manufacturingYear,
                         String color, String dailyPrice, Long mileage, Long categoryId) {
        return vehicleService.create(new VehicleRequestDto(
                registration, brand, model, manufacturingYear, color,
                new BigDecimal(dailyPrice), mileage, categoryId)).id();
    }

    private Long customer(String firstName, String lastName, String email, String phone,
                          String address, String drivingLicenseNumber, LocalDate expiry) {
        return customerService.create(new CustomerRequestDto(
                firstName, lastName, email, phone, address, drivingLicenseNumber, expiry)).id();
    }

    /** Creates a rental and drives it through start and complete to COMPLETED. */
    private Long completedRental(Long vehicleId, Long customerId, LocalDate start, LocalDate end) {
        Long rentalId = rentalService.create(new RentalRequestDto(start, end, vehicleId, customerId)).id();
        rentalService.start(rentalId);
        rentalService.complete(rentalId);
        return rentalId;
    }
}
