package com.bgsistemas.api.globals;

import com.bgsistemas.api.auth.AuthController;
import com.bgsistemas.api.auth.AuthService;
import com.bgsistemas.api.brand.BrandController;
import com.bgsistemas.api.brand.BrandService;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.brand.mappers.BrandMapper;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.contact.ContactController;
import com.bgsistemas.api.contact.ContactService;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import com.bgsistemas.api.service.ServiceService;
import com.bgsistemas.api.service.mappers.ServiceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(controllers = {ContactController.class, BrandController.class, AuthController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ContactService contactService;
    @MockitoBean private ContactMapper contactMapper;
    @MockitoBean private BrandService brandService;
    @MockitoBean private BrandMapper brandMapper;
    @MockitoBean private ServiceService serviceService;
    @MockitoBean private ServiceMapper serviceMapper;
    @MockitoBean private AuthService authService;

    @Test
    void shouldReturn400WithFieldDetailsWhenValidationFails() throws Exception {
        String json = """
                { "name": "", "email": "not-an-email", "message": "Hi", "gdprConsent": true }
                """;

        mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/contacts"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content("{ bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn404WhenBrandDoesNotExist() throws Exception {
        when(brandService.getBrandById(99L)).thenThrow(new BrandNotFoundException(99L));

        mockMvc.perform(get("/api/brands/99/services"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Brand with id 99 not found"));
    }

    @Test
    void shouldReturn401AsJsonWhenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"new@mail.com\", \"password\": \"password123\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("You must log in to access this resource"));
    }

    @Test
    void shouldReturn403AsJsonWhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(user("visitor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"new@mail.com\", \"password\": \"password123\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }
}
