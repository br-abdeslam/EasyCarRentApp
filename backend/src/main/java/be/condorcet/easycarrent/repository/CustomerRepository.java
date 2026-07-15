package be.condorcet.easycarrent.repository;

import be.condorcet.easycarrent.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Customer> findByDrivingLicenseNumberIgnoreCase(String drivingLicenseNumber);

    boolean existsByDrivingLicenseNumberIgnoreCase(String drivingLicenseNumber);
}
