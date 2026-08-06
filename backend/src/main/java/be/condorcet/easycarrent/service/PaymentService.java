package be.condorcet.easycarrent.service;

import be.condorcet.easycarrent.dto.PaymentRequestDto;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.entity.Payment;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.PaymentMapper;
import be.condorcet.easycarrent.repository.PaymentRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for payments: reads, creation, lifecycle transitions and deletion.
 *
 * <p>A payment can only be created for an ACTIVE or COMPLETED rental, and at
 * most one payment may exist per rental. The amount is derived from the rental
 * total; the client never supplies it. The lifecycle transitions (pay, fail,
 * retry, refund) and deletion enforce the payment status rules here: pay and fail
 * require PENDING, retry requires FAILED, refund requires PAID, and a PAID or
 * REFUNDED payment cannot be deleted.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    /** Rental statuses for which a payment may be created. */
    private static final Set<RentalStatus> PAYABLE_RENTAL_STATUSES =
            Set.of(RentalStatus.ACTIVE, RentalStatus.COMPLETED);

    private static final int MONETARY_SCALE = 2;

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          RentalRepository rentalRepository,
                          PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.rentalRepository = rentalRepository;
        this.paymentMapper = paymentMapper;
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    public List<PaymentResponseDto> findAll() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    public PaymentResponseDto findById(Long id) {
        return paymentMapper.toResponse(getPayment(id));
    }

    /**
     * Returns the payment for a rental. The rental is checked first: a missing
     * rental and a rental without a payment produce distinct not-found errors.
     */
    public PaymentResponseDto findByRentalId(Long rentalId) {
        if (!rentalRepository.existsById(rentalId)) {
            throw new ResourceNotFoundException("Rental not found with id: " + rentalId);
        }
        Payment payment = paymentRepository.findByRental_Id(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for rental id: " + rentalId));
        return paymentMapper.toResponse(payment);
    }

    // ---------------------------------------------------------------------
    // Creation
    // ---------------------------------------------------------------------

    @Transactional
    public PaymentResponseDto create(PaymentRequestDto request) {
        validateRequest(request);
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental not found with id: " + request.rentalId()));
        validateRentalIsPayable(rental);
        rejectDuplicatePayment(rental);

        BigDecimal amount = deriveAmount(rental);
        Payment payment = paymentMapper.toEntity(request, rental, amount);
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    // ---------------------------------------------------------------------
    // Lifecycle transitions
    // ---------------------------------------------------------------------

    @Transactional
    public PaymentResponseDto markPaid(Long id) {
        Payment payment = getPayment(id);
        requireStatus(payment, PaymentStatus.PENDING, "marked as paid");
        payment.markPaid(LocalDateTime.now());
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponseDto markFailed(Long id) {
        Payment payment = getPayment(id);
        requireStatus(payment, PaymentStatus.PENDING, "marked as failed");
        payment.markFailed();
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponseDto retry(Long id) {
        Payment payment = getPayment(id);
        requireStatus(payment, PaymentStatus.FAILED, "retried");
        payment.retry();
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponseDto refund(Long id) {
        Payment payment = getPayment(id);
        requireStatus(payment, PaymentStatus.PAID, "refunded");
        payment.refund();
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = getPayment(id);
        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.PAID || status == PaymentStatus.REFUNDED) {
            throw new ResourceConflictException(
                    "A payment that is " + status + " cannot be deleted");
        }
        paymentRepository.delete(payment);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void validateRequest(PaymentRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request must not be null");
        }
        if (request.rentalId() == null) {
            throw new IllegalArgumentException("rentalId must not be null");
        }
        if (request.paymentMethod() == null) {
            throw new IllegalArgumentException("paymentMethod must not be null");
        }
    }

    private void validateRentalIsPayable(Rental rental) {
        if (!PAYABLE_RENTAL_STATUSES.contains(rental.getStatus())) {
            throw new ResourceConflictException(
                    "A payment can only be created for an ACTIVE or COMPLETED rental but rental "
                            + rental.getId() + " is " + rental.getStatus());
        }
    }

    private void rejectDuplicatePayment(Rental rental) {
        if (paymentRepository.existsByRental_Id(rental.getId())) {
            throw new ResourceConflictException(
                    "A payment already exists for rental " + rental.getId());
        }
    }

    private BigDecimal deriveAmount(Rental rental) {
        BigDecimal totalPrice = rental.getTotalPrice();
        if (totalPrice == null) {
            throw new IllegalStateException("Rental " + rental.getId() + " has no total price");
        }
        return totalPrice.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }

    private void requireStatus(Payment payment, PaymentStatus required, String action) {
        if (payment.getStatus() != required) {
            throw new ResourceConflictException(
                    "A payment can only be " + action + " from " + required
                            + " but payment " + payment.getId() + " is " + payment.getStatus());
        }
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}
