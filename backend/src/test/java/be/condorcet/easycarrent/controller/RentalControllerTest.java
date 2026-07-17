package be.condorcet.easycarrent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.condorcet.easycarrent.config.SecurityConfig;
import be.condorcet.easycarrent.dto.RentalResponseDto;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.exception.InvalidRentalPeriodException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.service.RentalService;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(RentalController.class)
@Import(SecurityConfig.class)
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RentalService rentalService;

    private static final String VALID_BODY = """
            {"startDate":"2026-06-10","endDate":"2026-06-20","vehicleId":1,"customerId":2}
            """;

    private RentalResponseDto sample(RentalStatus status) {
        return new RentalResponseDto(100L, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20),
                status, new BigDecimal("500.00"), LocalDateTime.of(2026, 1, 1, 12, 0),
                1L, "1-ABC-001", "Toyota", "Corolla", 2L, "John", "Doe");
    }

    // -------------------------------------------------------------- Reads

    @Test
    @WithMockUser(roles = "USER")
    void listReturnsOk() throws Exception {
        when(rentalService.findAll()).thenReturn(List.of(sample(RentalStatus.PLANNED)));

        mockMvc.perform(get("/api/rentals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].customerFirstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getReturnsOk() throws Exception {
        when(rentalService.findById(100L)).thenReturn(sample(RentalStatus.PLANNED));

        mockMvc.perform(get("/api/rentals/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.totalPrice").value(500.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMissingReturnsNotFoundError() throws Exception {
        when(rentalService.findById(99L)).thenThrow(new ResourceNotFoundException("Rental not found with id 99"));

        mockMvc.perform(get("/api/rentals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/rentals/99"))
                .andExpect(jsonPath("$.message").value("Rental not found with id 99"));
    }

    @Test
    void listUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/rentals"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Create

    @Test
    @WithMockUser(roles = "USER")
    void createAsUserReturnsCreatedWithLocation() throws Exception {
        when(rentalService.create(any())).thenReturn(sample(RentalStatus.PLANNED));

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/rentals/100")))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsCreated() throws Exception {
        when(rentalService.create(any())).thenReturn(sample(RentalStatus.PLANNED));

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithMissingStartDateReturnsValidationError() throws Exception {
        String body = """
                {"endDate":"2026-06-20","vehicleId":1,"customerId":2}
                """;
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.startDate").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithMissingVehicleIdReturnsValidationError() throws Exception {
        String body = """
                {"startDate":"2026-06-10","endDate":"2026-06-20","customerId":2}
                """;
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.vehicleId").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithMissingCustomerIdReturnsValidationError() throws Exception {
        String body = """
                {"startDate":"2026-06-10","endDate":"2026-06-20","vehicleId":1}
                """;
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.customerId").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithInvalidPeriodReturnsBadRequest() throws Exception {
        when(rentalService.create(any()))
                .thenThrow(new InvalidRentalPeriodException("endDate must be strictly after startDate"));

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithOverlapReturnsConflict() throws Exception {
        when(rentalService.create(any()))
                .thenThrow(new ResourceConflictException("overlapping rental"));

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithInvalidLicenceReturnsConflict() throws Exception {
        when(rentalService.create(any()))
                .thenThrow(new ResourceConflictException("licence expired"));

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createWithInvalidVehicleStateReturnsConflict() throws Exception {
        when(rentalService.create(any()))
                .thenThrow(new ResourceConflictException("vehicle in maintenance"));

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Update

    @Test
    @WithMockUser(roles = "USER")
    void updateAsUserReturnsOk() throws Exception {
        when(rentalService.update(eq(100L), any())).thenReturn(sample(RentalStatus.PLANNED));

        mockMvc.perform(put("/api/rentals/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateWithInvalidDtoReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/rentals/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateMissingReturnsNotFound() throws Exception {
        when(rentalService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Rental not found with id 99"));

        mockMvc.perform(put("/api/rentals/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateConflictReturnsConflict() throws Exception {
        when(rentalService.update(eq(100L), any()))
                .thenThrow(new ResourceConflictException("Only a PLANNED rental can be updated"));

        mockMvc.perform(put("/api/rentals/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/rentals/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Delete

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/rentals/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteAsUserReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/rentals/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/rentals/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteActiveRentalReturnsConflict() throws Exception {
        doThrow(new ResourceConflictException("An active rental cannot be deleted"))
                .when(rentalService).delete(100L);

        mockMvc.perform(delete("/api/rentals/100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // -------------------------------------------------------------- Lifecycle

    @Test
    @WithMockUser(roles = "USER")
    void startAsUserReturnsOk() throws Exception {
        when(rentalService.start(100L)).thenReturn(sample(RentalStatus.ACTIVE));

        mockMvc.perform(patch("/api/rentals/100/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void completeAsUserReturnsOk() throws Exception {
        when(rentalService.complete(100L)).thenReturn(sample(RentalStatus.COMPLETED));

        mockMvc.perform(patch("/api/rentals/100/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void cancelAsUserReturnsOk() throws Exception {
        when(rentalService.cancel(100L)).thenReturn(sample(RentalStatus.CANCELLED));

        mockMvc.perform(patch("/api/rentals/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void invalidTransitionReturnsConflict() throws Exception {
        when(rentalService.start(100L))
                .thenThrow(new ResourceConflictException("A rental can only be started from PLANNED"));

        mockMvc.perform(patch("/api/rentals/100/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void lifecycleUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/rentals/100/start"))
                .andExpect(status().isUnauthorized());
    }
}
