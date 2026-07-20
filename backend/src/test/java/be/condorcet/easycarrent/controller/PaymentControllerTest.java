package be.condorcet.easycarrent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.condorcet.easycarrent.config.SecurityConfig;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.entity.PaymentMethod;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static final String VALID_BODY = """
            {"rentalId":42,"paymentMethod":"CARD"}
            """;

    private PaymentResponseDto sample(PaymentStatus status, LocalDateTime paidAt) {
        return new PaymentResponseDto(100L, 42L, RentalStatus.ACTIVE, new BigDecimal("199.99"),
                PaymentMethod.CARD, status, LocalDateTime.of(2026, 8, 1, 10, 0), paidAt);
    }

    private PaymentResponseDto pending() {
        return sample(PaymentStatus.PENDING, null);
    }

    // -------------------------------------------------------------- Reads

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsOkWithDtoFields() throws Exception {
        when(paymentService.findAll()).thenReturn(List.of(pending()));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].rentalId").value(42))
                .andExpect(jsonPath("$[0].amount").value(199.99))
                .andExpect(jsonPath("$[0].paymentMethod").value("CARD"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsOk() throws Exception {
        when(paymentService.findById(100L)).thenReturn(pending());

        mockMvc.perform(get("/api/payments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByRentalIdReturnsOk() throws Exception {
        when(paymentService.findByRentalId(42L)).thenReturn(pending());

        mockMvc.perform(get("/api/payments/rental/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalId").value(42));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByRentalIdDelegatesAndIsNotRoutedToFindById() throws Exception {
        when(paymentService.findByRentalId(42L)).thenReturn(pending());

        mockMvc.perform(get("/api/payments/rental/42"))
                .andExpect(status().isOk());

        verify(paymentService).findByRentalId(42L);
        verify(paymentService, never()).findById(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMissingReturnsNotFoundError() throws Exception {
        when(paymentService.findById(99L)).thenThrow(new ResourceNotFoundException("Payment not found with id: 99"));

        mockMvc.perform(get("/api/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/payments/99"))
                .andExpect(jsonPath("$.message").value("Payment not found with id: 99"));
    }

    @Test
    void listUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Creation

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedWithLocationAndBody() throws Exception {
        when(paymentService.create(any())).thenReturn(pending());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/payments/100")))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingRentalIdReturnsValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.rentalId").exists());

        verify(paymentService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingPaymentMethodReturnsValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rentalId\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.paymentMethod").exists());

        verify(paymentService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingRentalReturnsNotFound() throws Exception {
        when(paymentService.create(any())).thenThrow(new ResourceNotFoundException("Rental not found with id: 42"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createInvalidRentalStatusReturnsConflict() throws Exception {
        when(paymentService.create(any()))
                .thenThrow(new ResourceConflictException("A payment can only be created for an ACTIVE or COMPLETED rental"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDuplicateReturnsConflict() throws Exception {
        when(paymentService.create(any()))
                .thenThrow(new ResourceConflictException("A payment already exists for rental 42"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Lifecycle

    @Test
    @WithMockUser(roles = "ADMIN")
    void payReturnsOk() throws Exception {
        when(paymentService.markPaid(100L)).thenReturn(sample(PaymentStatus.PAID, LocalDateTime.of(2026, 8, 2, 9, 0)));

        mockMvc.perform(patch("/api/payments/100/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void failReturnsOk() throws Exception {
        when(paymentService.markFailed(100L)).thenReturn(sample(PaymentStatus.FAILED, null));

        mockMvc.perform(patch("/api/payments/100/fail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void retryReturnsOk() throws Exception {
        when(paymentService.retry(100L)).thenReturn(pending());

        mockMvc.perform(patch("/api/payments/100/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refundReturnsOk() throws Exception {
        when(paymentService.refund(100L)).thenReturn(sample(PaymentStatus.REFUNDED, LocalDateTime.of(2026, 8, 2, 9, 0)));

        mockMvc.perform(patch("/api/payments/100/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidTransitionReturnsConflict() throws Exception {
        when(paymentService.markPaid(100L))
                .thenThrow(new ResourceConflictException("A payment can only be marked as paid from PENDING"));

        mockMvc.perform(patch("/api/payments/100/pay"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void lifecycleMissingPaymentReturnsNotFound() throws Exception {
        when(paymentService.markPaid(100L)).thenThrow(new ResourceNotFoundException("Payment not found with id: 100"));

        mockMvc.perform(patch("/api/payments/100/pay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void lifecycleUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/payments/100/pay"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Delete

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturnsNoContentWithEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/payments/100"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePaidOrRefundedReturnsConflict() throws Exception {
        doThrow(new ResourceConflictException("A payment that is PAID cannot be deleted"))
                .when(paymentService).delete(100L);

        mockMvc.perform(delete("/api/payments/100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMissingReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Payment not found with id: 100"))
                .when(paymentService).delete(100L);

        mockMvc.perform(delete("/api/payments/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/payments/100"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Authorization (USER vs ADMIN)
    // Reads, create and the normal lifecycle (pay/fail/retry) are open to USER and
    // ADMIN; refund and delete are ADMIN-only. ADMIN success paths are already
    // covered by the tests above.

    @Test
    @WithMockUser(roles = "USER")
    void listAsUserReturnsOk() throws Exception {
        when(paymentService.findAll()).thenReturn(List.of(pending()));

        mockMvc.perform(get("/api/payments")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByIdAsUserReturnsOk() throws Exception {
        when(paymentService.findById(100L)).thenReturn(pending());

        mockMvc.perform(get("/api/payments/100")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByRentalIdAsUserReturnsOk() throws Exception {
        when(paymentService.findByRentalId(42L)).thenReturn(pending());

        mockMvc.perform(get("/api/payments/rental/42")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAsUserReturnsCreated() throws Exception {
        when(paymentService.create(any())).thenReturn(pending());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void payAsUserReturnsOk() throws Exception {
        when(paymentService.markPaid(100L)).thenReturn(sample(PaymentStatus.PAID, LocalDateTime.of(2026, 8, 2, 9, 0)));

        mockMvc.perform(patch("/api/payments/100/pay")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void failAsUserReturnsOk() throws Exception {
        when(paymentService.markFailed(100L)).thenReturn(sample(PaymentStatus.FAILED, null));

        mockMvc.perform(patch("/api/payments/100/fail")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void retryAsUserReturnsOk() throws Exception {
        when(paymentService.retry(100L)).thenReturn(pending());

        mockMvc.perform(patch("/api/payments/100/retry")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void refundAsUserReturnsForbiddenAndDoesNotCallService() throws Exception {
        mockMvc.perform(patch("/api/payments/100/refund"))
                .andExpect(status().isForbidden());

        verify(paymentService, never()).refund(any());
    }

    @Test
    void refundUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/payments/100/refund"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteAsUserReturnsForbiddenAndDoesNotCallService() throws Exception {
        mockMvc.perform(delete("/api/payments/100"))
                .andExpect(status().isForbidden());

        verify(paymentService, never()).delete(any());
    }
}
