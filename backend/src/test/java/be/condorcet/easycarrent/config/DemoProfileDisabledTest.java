package be.condorcet.easycarrent.config;

import static org.assertj.core.api.Assertions.assertThat;

import be.condorcet.easycarrent.service.CustomerService;
import be.condorcet.easycarrent.service.MaintenanceRecordService;
import be.condorcet.easycarrent.service.PaymentService;
import be.condorcet.easycarrent.service.RentalService;
import be.condorcet.easycarrent.service.VehicleCategoryService;
import be.condorcet.easycarrent.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Verifies that the demonstration initializer is inactive under the default
 * profile: no demo bean is registered and no data is inserted at startup.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-disabled-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class DemoProfileDisabledTest {

    @Autowired private ApplicationContext context;

    @Autowired private VehicleCategoryService categoryService;
    @Autowired private VehicleService vehicleService;
    @Autowired private CustomerService customerService;
    @Autowired private RentalService rentalService;
    @Autowired private PaymentService paymentService;
    @Autowired private MaintenanceRecordService maintenanceService;

    @Test
    void demoBeansAreNotRegisteredUnderTheDefaultProfile() {
        assertThat(context.getBeanNamesForType(DemoDataInitializer.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DemoDataSeeder.class)).isEmpty();
    }

    @Test
    void noDemoDataIsInsertedUnderTheDefaultProfile() {
        assertThat(categoryService.findAll()).isEmpty();
        assertThat(vehicleService.findAll()).isEmpty();
        assertThat(customerService.findAll()).isEmpty();
        assertThat(rentalService.findAll()).isEmpty();
        assertThat(paymentService.findAll()).isEmpty();
        assertThat(maintenanceService.findAll()).isEmpty();
    }
}
