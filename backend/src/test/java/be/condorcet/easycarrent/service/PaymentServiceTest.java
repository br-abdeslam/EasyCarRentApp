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
}
