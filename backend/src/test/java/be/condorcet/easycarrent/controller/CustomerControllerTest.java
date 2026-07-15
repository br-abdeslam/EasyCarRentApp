package be.condorcet.easycarrent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.condorcet.easycarrent.config.SecurityConfig;
import be.condorcet.easycarrent.dto.CustomerResponseDto;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.service.CustomerService;
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

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static final String VALID_BODY = """
            {"firstName":"John","lastName":"Doe","email":"john.doe@example.com",
             "phone":"+32 470 12 34 56","address":"Rue de la Loi 16, Brussels",
             "drivingLicenseNumber":"BE1234567","drivingLicenseExpiryDate":"2999-12-31"}
            """;

    private CustomerResponseDto sample() {
        return new CustomerResponseDto(10L, "John", "Doe", "john.doe@example.com",
                "+32 470 12 34 56", "Rue de la Loi 16, Brussels", "BE1234567",
                LocalDate.of(2999, 12, 31));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listReturnsOk() throws Exception {
        when(customerService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getReturnsOk() throws Exception {
        when(customerService.findById(10L)).thenReturn(sample());

        mockMvc.perform(get("/api/customers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.drivingLicenseNumber").value("BE1234567"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMissingReturnsNotFoundError() throws Exception {
        when(customerService.findById(99L)).thenThrow(new ResourceNotFoundException("Customer not found with id 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/customers/99"))
                .andExpect(jsonPath("$.message").value("Customer not found with id 99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsCreatedWithLocation() throws Exception {
        when(customerService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/customers/10")))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithBlankFirstNameReturnsValidationError() throws Exception {
        String body = """
                {"firstName":"  ","lastName":"Doe","email":"john.doe@example.com",
                 "phone":"+32 470 12 34 56","address":"Rue de la Loi 16",
                 "drivingLicenseNumber":"BE1234567","drivingLicenseExpiryDate":"2999-12-31"}
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.firstName").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithInvalidEmailReturnsValidationError() throws Exception {
        String body = """
                {"firstName":"John","lastName":"Doe","email":"not-an-email",
                 "phone":"+32 470 12 34 56","address":"Rue de la Loi 16",
                 "drivingLicenseNumber":"BE1234567","drivingLicenseExpiryDate":"2999-12-31"}
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithInvalidPhoneReturnsValidationError() throws Exception {
        String body = """
                {"firstName":"John","lastName":"Doe","email":"john.doe@example.com",
                 "phone":"not-a-phone","address":"Rue de la Loi 16",
                 "drivingLicenseNumber":"BE1234567","drivingLicenseExpiryDate":"2999-12-31"}
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.phone").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithExpiredLicenceReturnsValidationError() throws Exception {
        String body = """
                {"firstName":"John","lastName":"Doe","email":"john.doe@example.com",
                 "phone":"+32 470 12 34 56","address":"Rue de la Loi 16",
                 "drivingLicenseNumber":"BE1234567","drivingLicenseExpiryDate":"2000-01-01"}
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.drivingLicenseExpiryDate").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDuplicateEmailReturnsConflict() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateResourceException("A customer with email 'john.doe@example.com' already exists"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDuplicateLicenceReturnsConflict() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateResourceException("A customer with driving licence number 'BE1234567' already exists"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminReturnsOk() throws Exception {
        when(customerService.update(eq(10L), any())).thenReturn(sample());

        mockMvc.perform(put("/api/customers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateWithInvalidDtoReturnsBadRequest() throws Exception {
        String body = """
                {"firstName":"","lastName":"","email":"bad","phone":"xx","address":"",
                 "drivingLicenseNumber":"","drivingLicenseExpiryDate":"2000-01-01"}
                """;

        mockMvc.perform(put("/api/customers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/customers/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listAsUserReturnsOk() throws Exception {
        when(customerService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk());
    }

    @Test
    void createUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAsUserReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateAsUserReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/customers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteAsUserReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/customers/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/customers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/customers/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithMalformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
