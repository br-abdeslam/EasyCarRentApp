package be.condorcet.easycarrent.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import be.condorcet.easycarrent.entity.VehicleStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private VehicleCategory persistCategory(String name) {
        return categoryRepository.saveAndFlush(new VehicleCategory(name, null));
    }

    private Vehicle newVehicle(String registration, VehicleStatus status, VehicleCategory category) {
        return new Vehicle(
                registration,
                "Toyota",
                "Corolla",
                2022,
                "Blue",
                new BigDecimal("49.99"),
                15000L,
                status,
                category);
    }

    @Test
    void persistsVehicleWithCategory() {
        VehicleCategory category = persistCategory("Compact");

        Vehicle saved = vehicleRepository.save(newVehicle("1-ABC-001", VehicleStatus.AVAILABLE, category));

        Optional<Vehicle> found = vehicleRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRegistrationNumber()).isEqualTo("1-ABC-001");
        assertThat(found.get().getDailyPrice()).isEqualByComparingTo("49.99");
        assertThat(found.get().getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void defaultsStatusToAvailableWhenNotSupplied() {
        VehicleCategory category = persistCategory("Default");

        Vehicle saved = vehicleRepository.saveAndFlush(new Vehicle(
                "1-DEF-002", "Ford", "Focus", 2021, "Red",
                new BigDecimal("39.50"), 20000L, category));

        assertThat(saved.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void findsVehicleByRegistrationNumberCaseInsensitively() {
        VehicleCategory category = persistCategory("Lookup");
        vehicleRepository.saveAndFlush(newVehicle("1-XyZ-003", VehicleStatus.AVAILABLE, category));

        assertThat(vehicleRepository.findByRegistrationNumberIgnoreCase("1-xyz-003")).isPresent();
        assertThat(vehicleRepository.findByRegistrationNumberIgnoreCase("1-XYZ-003")).isPresent();
        assertThat(vehicleRepository.existsByRegistrationNumberIgnoreCase("1-xYz-003")).isTrue();
    }

    @Test
    void rejectsDuplicateRegistrationNumber() {
        VehicleCategory category = persistCategory("Unique");
        vehicleRepository.saveAndFlush(newVehicle("1-DUP-004", VehicleStatus.AVAILABLE, category));

        assertThatThrownBy(() ->
                vehicleRepository.saveAndFlush(newVehicle("1-DUP-004", VehicleStatus.RENTED, category)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsAndQueriesByStatus() {
        VehicleCategory category = persistCategory("Status");
        vehicleRepository.saveAndFlush(newVehicle("1-AVA-005", VehicleStatus.AVAILABLE, category));
        vehicleRepository.saveAndFlush(newVehicle("1-MNT-006", VehicleStatus.MAINTENANCE, category));

        List<Vehicle> maintenance = vehicleRepository.findAllByStatus(VehicleStatus.MAINTENANCE);

        assertThat(maintenance).hasSize(1);
        assertThat(maintenance.get(0).getRegistrationNumber()).isEqualTo("1-MNT-006");
        assertThat(maintenance.get(0).getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void requiresCategory() {
        Vehicle orphan = newVehicle("1-NUL-007", VehicleStatus.AVAILABLE, null);

        assertThatThrownBy(() -> vehicleRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void doesNotCascadeDeleteFromCategoryToVehicle() {
        VehicleCategory category = persistCategory("Referenced");
        vehicleRepository.saveAndFlush(newVehicle("1-REF-008", VehicleStatus.AVAILABLE, category));

        // Detach everything so the delete below is issued as a plain
        // DELETE on vehicle_categories rather than being reordered inside the
        // current persistence context.
        entityManager.flush();
        entityManager.clear();

        // No CascadeType.REMOVE is configured, so the foreign key must block the
        // deletion of a category still referenced by a vehicle rather than
        // silently deleting that vehicle.
        // Flush through the repository so the persistence exception is
        // translated into Spring's DataAccessException hierarchy.
        assertThatThrownBy(() -> {
            categoryRepository.deleteById(category.getId());
            categoryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
