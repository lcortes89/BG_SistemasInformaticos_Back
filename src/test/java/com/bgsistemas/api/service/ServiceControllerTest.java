package com.bgsistemas.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceController.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private ServiceMapper serviceMapper;

    @Test
    void shouldReturnListOfServices() throws Exception {
        Service service = new Service(1L, "Web Design", "Custom website design", "design.jpg");
        ServiceDTO dto = new ServiceDTO(1L, "Web Design", "Custom website design", "design.jpg");

        when(serviceService.getAllServices()).thenReturn(List.of(service));
        when(serviceMapper.toDTO(service)).thenReturn(dto);

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Web Design"));
    }

    // NEW: ?featured=true returns only featured services
    @Test
    void shouldReturnOnlyFeaturedServicesWhenFilterIsTrue() throws Exception {
        Service bdpOne = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        ServiceDTO dto = new ServiceDTO(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");

        when(serviceService.getFeaturedServices()).thenReturn(List.of(bdpOne));
        when(serviceMapper.toDTO(bdpOne)).thenReturn(dto);

        mockMvc.perform(get("/api/services").param("featured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("BDP One"));
    }
}