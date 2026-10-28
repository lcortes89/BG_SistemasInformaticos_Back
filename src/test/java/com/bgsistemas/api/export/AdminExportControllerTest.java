package com.bgsistemas.api.export;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandService;
import com.bgsistemas.api.client.ClientService;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceService;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-255: only the admin downloads the lists, as PDF or Excel
@Import({SecurityConfig.class, ExportService.class, PdfExporter.class, ExcelExporter.class})
@WebMvcTest(AdminExportController.class)
class AdminExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private ServiceService serviceService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void shouldReturn401WithoutLogin() throws Exception {
        mockMvc.perform(get("/api/admin/clients/export").param("format", "pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDownloadTheClientsAsExcel() throws Exception {
        when(clientService.getClientsPage(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/clients/export").param("format", "xlsx").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition",
                        containsString("clientes-" + LocalDate.now() + ".xlsx")));
    }

    @Test
    void shouldExportEveryRecordAndNotOnlyOnePage() throws Exception {
        when(clientService.getClientsPage(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/clients/export").param("format", "pdf").with(user(admin)))
                .andExpect(status().isOk());

        verify(clientService).getClientsPage(eq(null), eq(null), org.mockito.ArgumentMatchers.argThat(Pageable::isUnpaged));
    }

    @Test
    void shouldDownloadTheBrandsAsPdf() throws Exception {
        Brand bdp = new Brand(1L, "BDP", "TPV", "Software", null, true);
        when(brandService.getBrandsPage(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bdp)));
        when(brandService.countServices(1L)).thenReturn(2L);

        mockMvc.perform(get("/api/admin/brands/export").param("format", "pdf").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("marcas-")));
    }

    @Test
    void shouldExportOnlyTheProductsOfTheChosenBrand() throws Exception {
        when(serviceService.getServicesPage(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Service(2L, "BDP One", "TPV", null))));

        mockMvc.perform(get("/api/admin/services/export").param("format", "xlsx").param("brandId", "1").with(user(admin)))
                .andExpect(status().isOk());

        verify(serviceService).getServicesPage(eq(1L), eq(null), eq(null), any(Pageable.class));
    }

    // BGSI-302: the export downloads what the table shows, with the same filters
    @Test
    void shouldExportWithTheSameSearchAndActiveFilterAsTheTable() throws Exception {
        when(clientService.getClientsPage(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(brandService.getBrandsPage(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(serviceService.getServicesPage(any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/clients/export").param("format", "pdf")
                .param("q", "plaza").param("active", "true").with(user(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/brands/export").param("format", "pdf")
                .param("q", "bdp").param("active", "false").with(user(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/services/export").param("format", "xlsx")
                .param("q", "one").param("active", "true").with(user(admin))).andExpect(status().isOk());

        verify(clientService).getClientsPage(eq("plaza"), eq(true), any(Pageable.class));
        verify(brandService).getBrandsPage(eq("bdp"), eq(false), any(Pageable.class));
        verify(serviceService).getServicesPage(eq(null), eq("one"), eq(true), any(Pageable.class));
    }

    @Test
    void shouldReturn400ForAnUnknownFormat() throws Exception {
        mockMvc.perform(get("/api/admin/clients/export").param("format", "docx").with(user(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Format must be pdf or xlsx"));
    }
}
