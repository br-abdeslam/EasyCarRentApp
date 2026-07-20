package be.condorcet.easycarrent.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import be.condorcet.easycarrent.dto.PaymentRequestDto;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.entity.Payment;
import be.condorcet.easycarrent.entity.PaymentMethod;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit tests for {@link PaymentMapper} and the Payment DTO contracts. No
 * Spring context or persistence is involved.
 */
class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    private Rental rentalWith(Long id, RentalStatus status) {
        Rental rental = new Rental(null, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                new BigDecimal("100.00"), status);
        ReflectionTestUtils.setField(rental, "id", id);
        return rental;
    }

    private Payment paymentWith(Long id, Rental rental, PaymentMethod method, String amount,
                                LocalDateTime createdAt) {
        Payment payment = new Payment(rental, method, new BigDecimal(amount));
        ReflectionTestUtils.setField(payment, "id", id);
        ReflectionTestUtils.setField(payment, "createdAt", createdAt);
        return payment;
    }

    // ------------------------------------------------------------------ toEntity

    @Test
    void toEntityUsesResolvedRentalMethodAndBackendAmount() {
        Rental rental = rentalWith(42L, RentalStatus.PLANNED);
        PaymentRequestDto request = new PaymentRequestDto(42L, PaymentMethod.CARD);

        Payment payment = mapper.toEntity(request, rental, new BigDecimal("250.00"));

        assertThat(payment.getRental()).isSameAs(rental);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    void toEntityDefaultsStatusToPendingWithNullPaidAt() {
        Payment payment = mapper.toEntity(
                new PaymentRequestDto(42L, PaymentMethod.CASH),
                rentalWith(42L, RentalStatus.PLANNED),
                new BigDecimal("100.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void toEntityDoesNotModifyRental() {
        Rental rental = rentalWith(42L, RentalStatus.ACTIVE);

        mapper.toEntity(new PaymentRequestDto(42L, PaymentMethod.CARD), rental, new BigDecimal("100.00"));

        assertThat(rental.getId()).isEqualTo(42L);
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    // ------------------------------------------------------------------ toResponse

    @Test
    void toResponseMapsAllPaymentAndRentalFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 10, 30);
        Rental rental = rentalWith(42L, RentalStatus.ACTIVE);
        Payment payment = paymentWith(7L, rental, PaymentMethod.CARD, "250.00", createdAt);

        PaymentResponseDto response = mapper.toResponse(payment);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.rentalId()).isEqualTo(42L);
        assertThat(response.rentalStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponseMapsNullPaidAt() {
        Payment payment = paymentWith(7L, rentalWith(42L, RentalStatus.PLANNED),
                PaymentMethod.CARD, "250.00", LocalDateTime.of(2026, 8, 1, 10, 30));

        assertThat(mapper.toResponse(payment).paidAt()).isNull();
    }

    // ------------------------------------------------------------------ DTO contracts

    @Test
    void requestDtoContainsExactlyRentalIdAndPaymentMethod() {
        assertThat(PaymentRequestDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("rentalId", "paymentMethod");
    }

    @Test
    void requestDtoHasNoAmountStatusOrTimestamps() {
        assertThat(PaymentRequestDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("id", "amount", "status", "createdAt", "paidAt");
    }

    @Test
    void requestDtoFieldsAreNotNullValidated() throws NoSuchFieldException {
        assertThat(PaymentRequestDto.class.getDeclaredField("rentalId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(PaymentRequestDto.class.getDeclaredField("paymentMethod").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void responseDtoContainsExactlyEightExpectedFields() {
        assertThat(PaymentResponseDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("id", "rentalId", "rentalStatus", "amount",
                        "paymentMethod", "status", "createdAt", "paidAt");
    }

    @Test
    void dtosExposeNoJpaEntityType() {
        for (RecordComponent rc : PaymentRequestDto.class.getRecordComponents()) {
            assertThat(rc.getType().isAnnotationPresent(Entity.class))
                    .as("request component %s must not be a JPA entity", rc.getName())
                    .isFalse();
        }
        for (RecordComponent rc : PaymentResponseDto.class.getRecordComponents()) {
            assertThat(rc.getType().isAnnotationPresent(Entity.class))
                    .as("response component %s must not be a JPA entity", rc.getName())
                    .isFalse();
        }
    }
}
