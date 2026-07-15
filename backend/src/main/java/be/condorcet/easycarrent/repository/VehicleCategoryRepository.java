package be.condorcet.easycarrent.repository;

import be.condorcet.easycarrent.entity.VehicleCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, Long> {

    Optional<VehicleCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
