package be.condorcet.easycarrent.repository;

import be.condorcet.easycarrent.entity.Payment;
import be.condorcet.easycarrent.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRental_Id(Long rentalId);

    boolean existsByRental_Id(Long rentalId);

    List<Payment> findAllByStatus(PaymentStatus status);
}
