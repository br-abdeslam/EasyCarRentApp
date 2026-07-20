package be.condorcet.easycarrent.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit tests for the {@link Payment} entity contract. No persistence is
 * involved, so no repository is required at this step.
 */
class PaymentTest {

    private Rental sampleRental() {
        return new Rental(null, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                new BigDecimal("200.00"));
    }

    @Test
    void newPaymentDefaultsToPendingWithAssignedFields() {
        Rental rental = sampleRental();
        Payment payment = new Payment(rental, PaymentMethod.CARD, new BigDecimal("200.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getAmount()).isEqualByComparingTo("200.00");
        assertThat(payment.getRental()).isSameAs(rental);
    }

    @Test
    void newPaymentHasNullPaidAt() {
        Payment payment = new Payment(sampleRental(), PaymentMethod.CASH, new BigDecimal("50.00"));

        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void transientPaymentsWithNullIdsAreNotEqual() {
        Payment a = new Payment(sampleRental(), PaymentMethod.CARD, new BigDecimal("100.00"));
        Payment b = new Payment(sampleRental(), PaymentMethod.CARD, new BigDecimal("100.00"));

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isEqualTo(a);
    }

    @Test
    void paymentsWithSameNonNullIdAreEqual() {
        Payment a = new Payment(sampleRental(), PaymentMethod.CARD, new BigDecimal("100.00"));
        Payment b = new Payment(sampleRental(), PaymentMethod.CASH, new BigDecimal("999.00"));
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 1L);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void differentTypeIsNotEqual() {
        Payment payment = new Payment(sampleRental(), PaymentMethod.CARD, new BigDecimal("100.00"));

        assertThat(payment).isNotEqualTo("not a payment");
    }

    @Test
    void toStringExcludesRentalAndFinancialData() {
        Payment payment = new Payment(sampleRental(), PaymentMethod.CARD, new BigDecimal("200.00"));
        ReflectionTestUtils.setField(payment, "id", 7L);

        String text = payment.toString();

        assertThat(text).contains("7").contains("PENDING");
        assertThat(text).doesNotContain("Rental").doesNotContain("200").doesNotContain("CARD");
    }

    // ------------------------------------------------------------------ lifecycle

    private Payment pendingPayment() {
        return new Payment(sampleRental(), PaymentMethod.CARD, new BigDecimal("200.00"));
    }

    @Test
    void markPaidSetsPaidStatusAndAssignsPaidAt() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 8, 2, 9, 15);
        Payment payment = pendingPayment();

        payment.markPaid(paidAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void markPaidRejectsNullTimestampWithoutChangingStatus() {
        Payment payment = pendingPayment();

        assertThatThrownBy(() -> payment.markPaid(null)).isInstanceOf(NullPointerException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void markFailedSetsFailedStatusAndLeavesPaidAtNull() {
        Payment payment = pendingPayment();

        payment.markFailed();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void retrySetsPendingStatusAndLeavesPaidAtNull() {
        Payment payment = pendingPayment();
        payment.markFailed();

        payment.retry();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void refundSetsRefundedStatusAndPreservesPaidAt() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 8, 2, 9, 15);
        Payment payment = pendingPayment();
        payment.markPaid(paidAt);

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void lifecycleMethodsPreserveAmountMethodAndRental() {
        Rental rental = sampleRental();
        Payment payment = new Payment(rental, PaymentMethod.CARD, new BigDecimal("200.00"));

        payment.markPaid(LocalDateTime.of(2026, 8, 2, 9, 15));
        payment.refund();

        assertThat(payment.getAmount()).isEqualByComparingTo("200.00");
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getRental()).isSameAs(rental);
    }
}
