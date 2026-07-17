package be.condorcet.easycarrent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A rental agreement linking one {@link Customer} to one {@link Vehicle} for an
 * inclusive date period.
 *
 * <p>Both relationships are required and lazily loaded through the
 * {@code customer_id} and {@code vehicle_id} foreign keys. Deleting a referenced
 * customer or vehicle is intentionally not cascaded: no {@code CascadeType.REMOVE}
 * is configured, so the foreign keys protect existing rentals.
 *
 * <p>The rental business rules (end date after start date, licence validity,
 * price calculation, overlap rejection) are enforced later by the service layer;
 * this entity only models persistence.
 */
@Entity
@Table(name = "rentals", indexes = {
        @Index(name = "idx_rental_vehicle_dates", columnList = "vehicle_id, start_date, end_date"),
        @Index(name = "idx_rental_customer", columnList = "customer_id"),
        @Index(name = "idx_rental_status", columnList = "status")
})
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalStatus status = RentalStatus.PLANNED;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    protected Rental() {
        // Required no-argument constructor for JPA.
    }

    /**
     * Creates a rental that defaults to {@link RentalStatus#PLANNED}.
     */
    public Rental(Vehicle vehicle,
                  Customer customer,
                  LocalDate startDate,
                  LocalDate endDate,
                  BigDecimal totalPrice) {
        this(vehicle, customer, startDate, endDate, totalPrice, RentalStatus.PLANNED);
    }

    public Rental(Vehicle vehicle,
                  Customer customer,
                  LocalDate startDate,
                  LocalDate endDate,
                  BigDecimal totalPrice,
                  RentalStatus status) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = status != null ? status : RentalStatus.PLANNED;
    }

    /**
     * Initializes {@link #createdAt} the first time the rental is persisted.
     * The column is non-updatable, so it is never changed afterwards.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rental that)) {
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
        // Intentionally excludes customer personal data and relationships.
        return "Rental{id=" + id + ", status=" + status + "}";
    }
}
