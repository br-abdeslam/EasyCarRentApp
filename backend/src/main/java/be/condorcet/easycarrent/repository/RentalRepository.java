package be.condorcet.easycarrent.repository;

import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findAllByCustomer_Id(Long customerId);

    List<Rental> findAllByVehicle_Id(Long vehicleId);

    List<Rental> findAllByStatus(RentalStatus status);

    boolean existsByCustomer_Id(Long customerId);

    boolean existsByVehicle_Id(Long vehicleId);

    /**
     * Counts rentals of one vehicle whose inclusive date range overlaps the
     * requested period and whose status is one of the supplied blocking
     * statuses. Two ranges overlap when
     * {@code existing.startDate <= requestedEnd AND existing.endDate >= requestedStart}.
     *
     * <p>The caller (service layer) decides which statuses block, typically
     * {@code PLANNED} and {@code ACTIVE}. The repository makes no business decision.
     */
    @Query("""
            SELECT COUNT(r) FROM Rental r
            WHERE r.vehicle.id = :vehicleId
              AND r.status IN :blockingStatuses
              AND r.startDate <= :requestedEnd
              AND r.endDate >= :requestedStart
            """)
    long countOverlappingRentals(@Param("vehicleId") Long vehicleId,
                                 @Param("requestedStart") LocalDate requestedStart,
                                 @Param("requestedEnd") LocalDate requestedEnd,
                                 @Param("blockingStatuses") Collection<RentalStatus> blockingStatuses);

    /**
     * Same overlap semantics as {@link #countOverlappingRentals}, but excludes the
     * rental identified by {@code excludedRentalId}. Used when updating an existing
     * rental so that it does not conflict with itself.
     */
    @Query("""
            SELECT COUNT(r) FROM Rental r
            WHERE r.vehicle.id = :vehicleId
              AND r.id <> :excludedRentalId
              AND r.status IN :blockingStatuses
              AND r.startDate <= :requestedEnd
              AND r.endDate >= :requestedStart
            """)
    long countOverlappingRentalsExcludingRentalId(@Param("vehicleId") Long vehicleId,
                                                  @Param("excludedRentalId") Long excludedRentalId,
                                                  @Param("requestedStart") LocalDate requestedStart,
                                                  @Param("requestedEnd") LocalDate requestedEnd,
                                                  @Param("blockingStatuses") Collection<RentalStatus> blockingStatuses);
}
