package be.condorcet.easycarrent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.condorcet.easycarrent.dto.PaymentRequestDto;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.entity.Payment;
import be.condorcet.easycarrent.entity.PaymentMethod;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.PaymentMapper;
import be.condorcet.easycarrent.repository.PaymentRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RentalRepository rentalRepository;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        // real mapper, consistent with the other service tests
        service = new PaymentService(paymentRepository, rentalRepository, new PaymentMapper());
    }

    // ------------------------------------------------------------------ fixtures

    private Rental rental(Long id, RentalStatus status, String totalPrice) {
        Rental rental = new Rental(null, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                totalPrice == null ? null : new BigDecimal(totalPrice), status);
        ReflectionTestUtils.setField(rental, "id", id);
        return rental;
    }

    private Payment payment(Long id, Rental rental, PaymentMethod method, String amount) {
        Payment payment = new Payment(rental, method, new BigDecimal(amount));
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }

    private PaymentRequestDto request(Long rentalId, PaymentMethod method) {
        return new PaymentRequestDto(rentalId, method);
    }

    private void stubSaveAssigningId(Long id) {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", id);
            return p;
        });
    }

    // ==================================================================== Reads

    @Test
    void findAllReturnsMappedPayments() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment(1L, rental, PaymentMethod.CARD, "100.00"),
                payment(2L, rental, PaymentMethod.CASH, "100.00")));

        List<PaymentResponseDto> result = service.findAll();

        assertThat(result).extracting(PaymentResponseDto::id).containsExactly(1L, 2L);
    }

    @Test
    void findAllReturnsEmptyListWhenNone() {
        when(paymentRepository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findByIdReturnsPayment() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(1L, rental, PaymentMethod.CARD, "100.00")));

        PaymentResponseDto result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.rentalId()).isEqualTo(42L);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByRentalIdReturnsPayment() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(rentalRepository.existsById(42L)).thenReturn(true);
        when(paymentRepository.findByRental_Id(42L)).thenReturn(Optional.of(payment(1L, rental, PaymentMethod.CARD, "100.00")));

        PaymentResponseDto result = service.findByRentalId(42L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.rentalId()).isEqualTo(42L);
    }

    @Test
    void findByRentalIdThrowsWhenRentalMissingAndDoesNotSearchPayment() {
        when(rentalRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByRentalId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rental");
        verify(paymentRepository, never()).findByRental_Id(any());
    }

    @Test
    void findByRentalIdThrowsWhenRentalExistsButNoPayment() {
        when(rentalRepository.existsById(42L)).thenReturn(true);
        when(paymentRepository.findByRental_Id(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRentalId(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment");
    }

    @Test
    void findByRentalIdChecksRentalBeforePayment() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(rentalRepository.existsById(42L)).thenReturn(true);
        when(paymentRepository.findByRental_Id(42L)).thenReturn(Optional.of(payment(1L, rental, PaymentMethod.CARD, "100.00")));

        service.findByRentalId(42L);

        InOrder order = inOrder(rentalRepository, paymentRepository);
        order.verify(rentalRepository).existsById(42L);
        order.verify(paymentRepository).findByRental_Id(42L);
    }

    // ==================================================================== Create success

    @Test
    void createForActiveRentalPersistsPendingPaymentFromRentalTotal() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "199.99");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(false);
        stubSaveAssigningId(500L);

        PaymentResponseDto result = service.create(request(42L, PaymentMethod.CARD));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getRental()).isSameAs(rental);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getPaidAt()).isNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("199.99");
        assertThat(result.id()).isEqualTo(500L);
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.rentalId()).isEqualTo(42L);
    }

    @Test
    void createForCompletedRentalIsAllowed() {
        Rental rental = rental(42L, RentalStatus.COMPLETED, "100.00");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(false);
        stubSaveAssigningId(500L);

        PaymentResponseDto result = service.create(request(42L, PaymentMethod.CASH));

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void createNormalizesAmountToScaleTwo() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "200.0"); // scale 1
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(false);
        stubSaveAssigningId(500L);

        service.create(request(42L, PaymentMethod.CARD));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount().scale()).isEqualTo(2);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void createDoesNotModifyRental() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(false);
        stubSaveAssigningId(500L);

        service.create(request(42L, PaymentMethod.CARD));

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(rental.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void createDuplicateCheckUsesResolvedRentalId() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(false);
        stubSaveAssigningId(500L);

        service.create(request(42L, PaymentMethod.CARD));

        verify(paymentRepository).existsByRental_Id(42L);
    }

    // ==================================================================== Create rejection

    @Test
    void createRejectsPlannedRentalWithoutDuplicateCheckOrSave() {
        Rental rental = rental(42L, RentalStatus.PLANNED, "100.00");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));

        assertThatThrownBy(() -> service.create(request(42L, PaymentMethod.CARD)))
                .isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).existsByRental_Id(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsCancelledRental() {
        Rental rental = rental(42L, RentalStatus.CANCELLED, "100.00");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));

        assertThatThrownBy(() -> service.create(request(42L, PaymentMethod.CARD)))
                .isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingRentalWithoutDuplicateCheckOrSave() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(99L, PaymentMethod.CARD)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(paymentRepository, never()).existsByRental_Id(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicatePaymentWithoutSave() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "100.00");
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(42L, PaymentMethod.CARD)))
                .isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsNullRequest() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsNullRentalId() {
        assertThatThrownBy(() -> service.create(request(null, PaymentMethod.CARD)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rentalRepository, never()).findById(any());
    }

    @Test
    void createRejectsNullPaymentMethod() {
        assertThatThrownBy(() -> service.create(request(42L, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rentalRepository, never()).findById(any());
    }

    @Test
    void createFailsSafelyWhenRentalTotalPriceIsNull() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, null);
        when(rentalRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRental_Id(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(42L, PaymentMethod.CARD)))
                .isInstanceOf(IllegalStateException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void requestDtoExposesNoAmountOrStatusSoClientCannotControlThem() {
        assertThat(PaymentRequestDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("amount", "status")
                .containsExactly("rentalId", "paymentMethod");
    }

    // ==================================================================== Lifecycle helpers

    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 8, 2, 9, 15);

    /** Builds a payment referencing the given rental and transitions it to the target status. */
    private Payment paymentInStatus(Long id, Rental rental, PaymentStatus status) {
        Payment payment = payment(id, rental, PaymentMethod.CARD, "100.00"); // PENDING
        switch (status) {
            case PENDING -> { }
            case PAID -> payment.markPaid(PAID_AT);
            case FAILED -> payment.markFailed();
            case REFUNDED -> {
                payment.markPaid(PAID_AT);
                payment.refund();
            }
        }
        return payment;
    }

    private Payment paymentInStatus(Long id, PaymentStatus status) {
        return paymentInStatus(id, rental(42L, RentalStatus.ACTIVE, "100.00"), status);
    }

    private void stubSaveReturnsArgument() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================================================================== Mark paid

    @Test
    void markPaidTransitionsPendingToPaidAndPopulatesPaidAt() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PENDING)));
        stubSaveReturnsArgument();

        PaymentResponseDto result = service.markPaid(1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.paidAt()).isNotNull();
    }

    @Test
    void markPaidRejectsPaid() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PAID)));

        assertThatThrownBy(() -> service.markPaid(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("PENDING");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markPaidRejectsFailed() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.FAILED)));

        assertThatThrownBy(() -> service.markPaid(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markPaidRejectsRefunded() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> service.markPaid(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markPaidThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================================================================== Mark failed

    @Test
    void markFailedTransitionsPendingToFailedLeavingPaidAtNull() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PENDING)));
        stubSaveReturnsArgument();

        PaymentResponseDto result = service.markFailed(1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.paidAt()).isNull();
    }

    @Test
    void markFailedRejectsPaid() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PAID)));

        assertThatThrownBy(() -> service.markFailed(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markFailedRejectsFailed() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.FAILED)));

        assertThatThrownBy(() -> service.markFailed(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markFailedRejectsRefunded() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> service.markFailed(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markFailedThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markFailed(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================================================================== Retry

    @Test
    void retryTransitionsFailedToPendingLeavingPaidAtNull() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.FAILED)));
        stubSaveReturnsArgument();

        PaymentResponseDto result = service.retry(1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.paidAt()).isNull();
    }

    @Test
    void retryRejectsPending() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("FAILED");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void retryRejectsPaid() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PAID)));

        assertThatThrownBy(() -> service.retry(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void retryRejectsRefunded() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> service.retry(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void retryThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retry(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================================================================== Refund

    @Test
    void refundTransitionsPaidToRefundedPreservingFieldsAndRental() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "199.99");
        Payment payment = paymentInStatus(1L, rental, PaymentStatus.PAID);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 8, 0);
        ReflectionTestUtils.setField(payment, "createdAt", createdAt);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        stubSaveReturnsArgument();

        PaymentResponseDto result = service.refund(1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.paidAt()).isEqualTo(PAID_AT);
        assertThat(result.amount()).isEqualByComparingTo("100.00");
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(rental.getTotalPrice()).isEqualByComparingTo("199.99");
    }

    @Test
    void refundRejectsPending() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.refund(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("PAID");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundRejectsFailed() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.FAILED)));

        assertThatThrownBy(() -> service.refund(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundRejectsRefunded() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> service.refund(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================================================================== Delete

    @Test
    void deletePendingSucceeds() {
        Payment payment = paymentInStatus(1L, PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        service.delete(1L);

        verify(paymentRepository).delete(payment);
    }

    @Test
    void deleteFailedSucceeds() {
        Payment payment = paymentInStatus(1L, PaymentStatus.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        service.delete(1L);

        verify(paymentRepository).delete(payment);
    }

    @Test
    void deletePaidRejectedWithoutDeleteCall() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.PAID)));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("PAID");
        verify(paymentRepository, never()).delete(any());
    }

    @Test
    void deleteRefundedRejectedWithoutDeleteCall() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentInStatus(1L, PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceConflictException.class);
        verify(paymentRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(paymentRepository, never()).delete(any());
    }

    @Test
    void deleteDoesNotModifyRental() {
        Rental rental = rental(42L, RentalStatus.ACTIVE, "199.99");
        Payment payment = paymentInStatus(1L, rental, PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        service.delete(1L);

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(rental.getTotalPrice()).isEqualByComparingTo("199.99");
        verify(rentalRepository, never()).delete(any());
    }
}
