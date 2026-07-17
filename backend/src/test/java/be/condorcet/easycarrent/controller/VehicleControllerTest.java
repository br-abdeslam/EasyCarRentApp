package be.condorcet.easycarrent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.condorcet.easycarrent.config.SecurityConfig;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.InvalidRentalPeriodException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.service.RentalService;
import be.condorcet.easycarrent.service.VehicleService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleController.class)
@Import(SecurityConfig.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private RentalService rentalService;

    private static final String VALID_BODY = """
            {"registrationNumber":"1-ABC-001","brand":"Toyota","model":"Corolla",
             "manufacturingYear":2022,"color":"Blue","dailyPrice":49.99,
             "mileage":15000,"categoryId":1}
            """;

    private VehicleResponseDto sampleResponse() {
        return new VehicleResponseDto(10L, "1-ABC-001", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, VehicleStatus.AVAILABLE, 1L, "SUV");
    }

    @Test
    @WithMockUser(roles = "USER")
    void listReturnsOk() throws Exception {
        when(vehicleService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("1-ABC-001"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getReturnsOk() throws Exception {
        when(vehicleService.findById(10L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/vehicles/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("SUV"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listByStatusReturnsOk() throws Exception {
        when(vehicleService.findByStatus(VehicleStatus.AVAILABLE)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/vehicles/status/AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void invalidStatusPathReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/vehicles/status/NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsCreated() throws Exception {
        when(vehicleService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithBlankRequiredFieldsReturnsValidationError() throws Exception {
        String body = """
                {"registrationNumber":"","brand":"","model":"","manufacturingYear":2022,
                 "color":"Blue","dailyPrice":49.99,"mileage":15000,"categoryId":1}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.registrationNumber").exists())
                .andExpect(jsonPath("$.validationErrors.brand").exists())
                .andExpect(jsonPath("$.validationErrors.model").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithNonPositiveDailyPriceReturnsValidationError() throws Exception {
        String body = """
                {"registrationNumber":"1-ABC-001","brand":"Toyota","model":"Corolla",
                 "manufacturingYear":2022,"color":"Blue","dailyPrice":0,
                 "mileage":15000,"categoryId":1}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.dailyPrice").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithNegativeMileageReturnsValidationError() throws Exception {
        String body = """
                {"registrationNumber":"1-ABC-001","brand":"Toyota","model":"Corolla",
                 "manufacturingYear":2022,"color":"Blue","dailyPrice":49.99,
                 "mileage":-5,"categoryId":1}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.mileage").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithFutureManufacturingYearReturnsValidationError() throws Exception {
        String body = """
                {"registrationNumber":"1-ABC-001","brand":"Toyota","model":"Corolla",
                 "manufacturingYear":3000,"color":"Blue","dailyPrice":49.99,
                 "mileage":15000,"categoryId":1}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.manufacturingYear").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithMissingCategoryReturnsNotFound() throws Exception {
        when(vehicleService.create(any())).thenThrow(new ResourceNotFoundException("Category not found with id 99"));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDuplicateRegistrationReturnsConflict() throws Exception {
        when(vehicleService.create(any())).thenThrow(new DuplicateResourceException("duplicate"));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAsUserReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------- Availability

    @Test
    @WithMockUser(roles = "USER")
    void availableAsUserReturnsOk() throws Exception {
        when(rentalService.findAvailableVehicles(any(), any())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("1-ABC-001"))
                .andExpect(jsonPath("$[0].categoryName").value("SUV"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void availableAsAdminReturnsOk() throws Exception {
        when(rentalService.findAvailableVehicles(any(), any())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-20"))
                .andExpect(status().isOk());
    }

    @Test
    void availableUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void availableWithMissingStartDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/vehicles/available")
                        .param("endDate", "2026-06-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void availableWithMissingEndDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "2026-06-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void availableWithInvalidDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2026-06-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void availableWithInvalidPeriodReturnsBadRequest() throws Exception {
        when(rentalService.findAvailableVehicles(any(), any()))
                .thenThrow(new InvalidRentalPeriodException("endDate must be strictly after startDate"));

        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "2026-06-20")
                        .param("endDate", "2026-06-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void availablePathIsNotRoutedToIdEndpoint() throws Exception {
        // If '/available' were captured by '/{id}', findById would run and the
        // literal would fail Long conversion. A 200 list proves the availability
        // mapping wins, and findById is never consulted.
        when(rentalService.findAvailableVehicles(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/vehicles/available")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-20"))
                .andExpect(status().isOk());
    }
}
