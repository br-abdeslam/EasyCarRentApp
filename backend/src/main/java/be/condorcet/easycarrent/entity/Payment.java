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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A payment settling exactly one {@link Rental}. A rental can have at most one
 * payment, enforced by the unique {@code rental_id} foreign key.
 *
 * <p>The amount is provided by the service from the rental total; the status is
 * backend-controlled and starts at {@link PaymentStatus#PENDING}. No card, bank
 * or external-provider data is stored. Deleting a rental is intentionally not
 * cascaded from here: no {@code CascadeType.REMOVE} or {@code CascadeType.ALL}
 * is configured.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    protected Payment() {
        // Required no-argument constructor for JPA.
    }

    /**
     * Creates a {@link PaymentStatus#PENDING} payment for a rental using the
     * backend-provided amount. {@code paidAt} stays null until the payment is
     * settled by a later lifecycle step.
     */
    public Payment(Rental rental, PaymentMethod paymentMethod, BigDecimal amount) {
        this.rental = rental;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    /**
     * Initializes {@link #createdAt} the first time the payment is persisted.
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

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Rental getRental() {
        return rental;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Payment that)) {
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
        // Intentionally excludes the rental relationship and financial details.
        return "Payment{id=" + id + ", status=" + status + "}";
    }
}
