package be.condorcet.easycarrent.repository;

import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    List<Vehicle> findAllByStatus(VehicleStatus status);

    boolean existsByCategory_Id(Long categoryId);
}
