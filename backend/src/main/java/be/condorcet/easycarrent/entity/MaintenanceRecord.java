package be.condorcet.easycarrent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A maintenance operation performed on exactly one {@link Vehicle} over an
 * inclusive date period.
 *
 * <p>The relationship to the vehicle is required and lazily loaded through the
 * {@code vehicle_id} foreign key. Many maintenance records may reference the same
 * vehicle. Deleting a maintenance record is intentionally not cascaded to the
 * vehicle: no {@code CascadeType.REMOVE} or {@code CascadeType.ALL} is configured,
 * so the foreign key protects the referenced vehicle.
 *
 * <p>The maintenance business rules (end date after start date, non-negative
 * cost, status handling) are enforced later by the service layer; this entity
 * only models persistence and defaults the status to
 * {@link MaintenanceStatus#PLANNED}.
 */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceStatus status = MaintenanceStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    protected MaintenanceRecord() {
        // Required no-argument constructor for JPA.
    }

    /**
     * Creates a maintenance record that defaults to
     * {@link MaintenanceStatus#PLANNED}.
     */
    public MaintenanceRecord(Vehicle vehicle,
                             String description,
                             LocalDate startDate,
                             LocalDate endDate,
                             BigDecimal cost) {
        this.vehicle = vehicle;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cost = cost;
        this.status = MaintenanceStatus.PLANNED;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    // ---------------------------------------------------------------------
    // Controlled lifecycle transitions. Whether a transition is allowed from
    // the current status, and the matching vehicle-status change, are decided
    // by the service layer; these methods only apply the direct status change
    // and never touch id, vehicle, description, dates or cost.
    // ---------------------------------------------------------------------

    /** Marks the maintenance as {@link MaintenanceStatus#IN_PROGRESS}. */
    public void start() {
        this.status = MaintenanceStatus.IN_PROGRESS;
    }

    /** Marks the maintenance as {@link MaintenanceStatus#COMPLETED}. */
    public void complete() {
        this.status = MaintenanceStatus.COMPLETED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MaintenanceRecord that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        // Intentionally excludes the vehicle relationship, the cost and the
        // full description.
        return "MaintenanceRecord{id=" + id + ", status=" + status + "}";
    }
}
