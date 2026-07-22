package be.condorcet.easycarrent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.exception.InvalidRequestException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.service.MaintenanceRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MaintenanceRecordController.class)
@Import(SecurityConfig.class)
class MaintenanceRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceRecordService maintenanceRecordService;

    private static final String VALID_BODY = """
            {"vehicleId":7,"description":"Full brake service","startDate":"2026-06-10","endDate":"2026-06-20","cost":120.00}
            """;

    private MaintenanceRecordResponseDto sample(Long id, MaintenanceStatus status) {
        return new MaintenanceRecordResponseDto(id, 7L, "Full brake service",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20), new BigDecimal("120.00"), status);
    }

    private MaintenanceRecordResponseDto planned() {
        return sample(100L, MaintenanceStatus.PLANNED);
    }

    // -------------------------------------------------------------- Reads

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsOkWithDtoFields() throws Exception {
        when(maintenanceRecordService.findAll()).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].vehicleId").value(7))
                .andExpect(jsonPath("$[0].description").value("Full brake service"))
                .andExpect(jsonPath("$[0].startDate").value("2026-06-10"))
                .andExpect(jsonPath("$[0].endDate").value("2026-06-20"))
                .andExpect(jsonPath("$[0].cost").value(120.00))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listEmptyReturnsOkWithEmptyArray() throws Exception {
        when(maintenanceRecordService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance-records"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsOk() throws Exception {
        when(maintenanceRecordService.findById(100L)).thenReturn(planned());

        mockMvc.perform(get("/api/maintenance-records/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMissingReturnsNotFoundError() throws Exception {
        when(maintenanceRecordService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Maintenance record not found with id: 99"));

        mockMvc.perform(get("/api/maintenance-records/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/maintenance-records/99"))
                .andExpect(jsonPath("$.message").value("Maintenance record not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByVehicleReturnsOk() throws Exception {
        when(maintenanceRecordService.findByVehicleId(7L)).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records/vehicle/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByVehicleWithoutRecordsReturnsEmptyList() throws Exception {
        when(maintenanceRecordService.findByVehicleId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance-records/vehicle/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByVehicleMissingVehicleReturnsNotFound() throws Exception {
        when(maintenanceRecordService.findByVehicleId(7L))
                .thenThrow(new ResourceNotFoundException("Vehicle not found with id: 7"));

        mockMvc.perform(get("/api/maintenance-records/vehicle/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByVehicleDelegatesAndIsNotRoutedToFindById() throws Exception {
        when(maintenanceRecordService.findByVehicleId(7L)).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records/vehicle/7")).andExpect(status().isOk());

        verify(maintenanceRecordService).findByVehicleId(7L);
        verify(maintenanceRecordService, never()).findById(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByStatusReturnsOk() throws Exception {
        when(maintenanceRecordService.findByStatus(MaintenanceStatus.PLANNED)).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records/status/PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByStatusReturnsOnlyRequestedStatus() throws Exception {
        when(maintenanceRecordService.findByStatus(MaintenanceStatus.IN_PROGRESS))
                .thenReturn(List.of(sample(101L, MaintenanceStatus.IN_PROGRESS)));

        mockMvc.perform(get("/api/maintenance-records/status/IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByStatusInvalidValueReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/maintenance-records/status/BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(maintenanceRecordService, never()).findByStatus(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByStatusDelegatesAndIsNotRoutedToFindById() throws Exception {
        when(maintenanceRecordService.findByStatus(MaintenanceStatus.PLANNED)).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records/status/PLANNED")).andExpect(status().isOk());

        verify(maintenanceRecordService).findByStatus(MaintenanceStatus.PLANNED);
        verify(maintenanceRecordService, never()).findById(anyLong());
    }

    @Test
    void listUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/maintenance-records"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Creation

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedWithLocationAndBody() throws Exception {
        when(maintenanceRecordService.create(any())).thenReturn(planned());

        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/api/maintenance-records/100")))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.vehicleId").value(7))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingVehicleIdReturnsValidationErrorAndDoesNotCallService() throws Exception {
        String body = """
                {"description":"Work","startDate":"2026-06-10","endDate":"2026-06-20","cost":120.00}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.vehicleId").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBlankDescriptionReturnsValidationError() throws Exception {
        String body = """
                {"vehicleId":7,"description":"   ","startDate":"2026-06-10","endDate":"2026-06-20","cost":120.00}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.description").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDescriptionTooLongReturnsValidationError() throws Exception {
        String longDescription = "x".repeat(501);
        String body = "{\"vehicleId\":7,\"description\":\"" + longDescription
                + "\",\"startDate\":\"2026-06-10\",\"endDate\":\"2026-06-20\",\"cost\":120.00}";
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.description").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingStartDateReturnsValidationError() throws Exception {
        String body = """
                {"vehicleId":7,"description":"Work","endDate":"2026-06-20","cost":120.00}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.startDate").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingEndDateReturnsValidationError() throws Exception {
        String body = """
                {"vehicleId":7,"description":"Work","startDate":"2026-06-10","cost":120.00}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.endDate").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingCostReturnsValidationError() throws Exception {
        String body = """
                {"vehicleId":7,"description":"Work","startDate":"2026-06-10","endDate":"2026-06-20"}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.cost").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNegativeCostReturnsValidationError() throws Exception {
        String body = """
                {"vehicleId":7,"description":"Work","startDate":"2026-06-10","endDate":"2026-06-20","cost":-1.00}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.cost").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCostExceedingPrecisionReturnsValidationError() throws Exception {
        String body = """
                {"vehicleId":7,"description":"Work","startDate":"2026-06-10","endDate":"2026-06-20","cost":10.999}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.cost").exists());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createInvalidDateFormatReturnsBadRequest() throws Exception {
        String body = """
                {"vehicleId":7,"description":"Work","startDate":"not-a-date","endDate":"2026-06-20","cost":120.00}
                """;
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEndBeforeStartReturnsBadRequest() throws Exception {
        // endDate < startDate is a service-level cross-field error (InvalidRequestException)
        // mapped by the GlobalExceptionHandler to 400. The controller does not compute it.
        when(maintenanceRecordService.create(any()))
                .thenThrow(new InvalidRequestException("Maintenance end date must be on or after start date"));

        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Maintenance end date must be on or after start date"))
                .andExpect(jsonPath("$.path").value("/api/maintenance-records"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMissingVehicleReturnsNotFound() throws Exception {
        when(maintenanceRecordService.create(any()))
                .thenThrow(new ResourceNotFoundException("Vehicle not found with id: 7"));

        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createInactiveVehicleReturnsConflict() throws Exception {
        when(maintenanceRecordService.create(any()))
                .thenThrow(new ResourceConflictException("Vehicle 7 is INACTIVE and cannot have maintenance scheduled"));

        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMaintenanceOverlapReturnsConflict() throws Exception {
        when(maintenanceRecordService.create(any()))
                .thenThrow(new ResourceConflictException("Vehicle 7 already has maintenance scheduled overlapping"));

        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRentalOverlapReturnsConflict() throws Exception {
        when(maintenanceRecordService.create(any()))
                .thenThrow(new ResourceConflictException("Vehicle 7 has a rental overlapping"));

        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Lifecycle: start

    @Test
    @WithMockUser(roles = "ADMIN")
    void startReturnsOkWithInProgress() throws Exception {
        when(maintenanceRecordService.start(100L)).thenReturn(sample(100L, MaintenanceStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/maintenance-records/100/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void startMissingReturnsNotFound() throws Exception {
        when(maintenanceRecordService.start(100L))
                .thenThrow(new ResourceNotFoundException("Maintenance record not found with id: 100"));

        mockMvc.perform(patch("/api/maintenance-records/100/start"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void startInvalidRecordTransitionReturnsConflict() throws Exception {
        when(maintenanceRecordService.start(100L))
                .thenThrow(new ResourceConflictException("Maintenance record 100 can only be started from PLANNED but is IN_PROGRESS"));

        mockMvc.perform(patch("/api/maintenance-records/100/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void startInvalidVehicleStatusReturnsConflict() throws Exception {
        when(maintenanceRecordService.start(100L))
                .thenThrow(new ResourceConflictException("Maintenance record 100 cannot be started because vehicle 7 is RENTED"));

        mockMvc.perform(patch("/api/maintenance-records/100/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void startUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/maintenance-records/100/start"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Lifecycle: complete

    @Test
    @WithMockUser(roles = "ADMIN")
    void completeReturnsOkWithCompleted() throws Exception {
        when(maintenanceRecordService.complete(100L)).thenReturn(sample(100L, MaintenanceStatus.COMPLETED));

        mockMvc.perform(patch("/api/maintenance-records/100/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void completeMissingReturnsNotFound() throws Exception {
        when(maintenanceRecordService.complete(100L))
                .thenThrow(new ResourceNotFoundException("Maintenance record not found with id: 100"));

        mockMvc.perform(patch("/api/maintenance-records/100/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void completeInvalidRecordTransitionReturnsConflict() throws Exception {
        when(maintenanceRecordService.complete(100L))
                .thenThrow(new ResourceConflictException("Maintenance record 100 can only be completed from IN_PROGRESS but is PLANNED"));

        mockMvc.perform(patch("/api/maintenance-records/100/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void completeInvalidVehicleStatusReturnsConflict() throws Exception {
        when(maintenanceRecordService.complete(100L))
                .thenThrow(new ResourceConflictException("Maintenance record 100 cannot be completed because vehicle 7 is AVAILABLE"));

        mockMvc.perform(patch("/api/maintenance-records/100/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void completeUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/maintenance-records/100/complete"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Delete

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturnsNoContentWithEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/maintenance-records/100"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteInProgressReturnsConflict() throws Exception {
        doThrow(new ResourceConflictException("Maintenance record 100 is IN_PROGRESS and can only be deleted while PLANNED"))
                .when(maintenanceRecordService).delete(100L);

        mockMvc.perform(delete("/api/maintenance-records/100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCompletedReturnsConflict() throws Exception {
        doThrow(new ResourceConflictException("Maintenance record 100 is COMPLETED and can only be deleted while PLANNED"))
                .when(maintenanceRecordService).delete(100L);

        mockMvc.perform(delete("/api/maintenance-records/100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMissingReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Maintenance record not found with id: 100"))
                .when(maintenanceRecordService).delete(100L);

        mockMvc.perform(delete("/api/maintenance-records/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/maintenance-records/100"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- Authorization (USER vs ADMIN)
    // Reads are open to USER and ADMIN; create, start, complete and delete are
    // ADMIN-only. The ADMIN success paths and the unauthenticated 401s are already
    // covered by the tests above.

    @Test
    @WithMockUser(roles = "USER")
    void listAsUserReturnsOk() throws Exception {
        when(maintenanceRecordService.findAll()).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByIdAsUserReturnsOk() throws Exception {
        when(maintenanceRecordService.findById(100L)).thenReturn(planned());

        mockMvc.perform(get("/api/maintenance-records/100")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByVehicleAsUserReturnsOk() throws Exception {
        when(maintenanceRecordService.findByVehicleId(7L)).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records/vehicle/7")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByStatusAsUserReturnsOk() throws Exception {
        when(maintenanceRecordService.findByStatus(MaintenanceStatus.PLANNED)).thenReturn(List.of(planned()));

        mockMvc.perform(get("/api/maintenance-records/status/PLANNED")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAsUserReturnsForbiddenAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/maintenance-records")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(maintenanceRecordService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void startAsUserReturnsForbiddenAndDoesNotCallService() throws Exception {
        mockMvc.perform(patch("/api/maintenance-records/100/start"))
                .andExpect(status().isForbidden());

        verify(maintenanceRecordService, never()).start(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    void completeAsUserReturnsForbiddenAndDoesNotCallService() throws Exception {
        mockMvc.perform(patch("/api/maintenance-records/100/complete"))
                .andExpect(status().isForbidden());

        verify(maintenanceRecordService, never()).complete(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteAsUserReturnsForbiddenAndDoesNotCallService() throws Exception {
        mockMvc.perform(delete("/api/maintenance-records/100"))
                .andExpect(status().isForbidden());

        verify(maintenanceRecordService, never()).delete(anyLong());
    }
}
