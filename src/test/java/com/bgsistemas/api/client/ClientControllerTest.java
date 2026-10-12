package com.bgsistemas.api.client;
import com.bgsistemas.api.client.dtos.ClientDTO;
import com.bgsistemas.api.client.mappers.ClientMapper;

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

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private ClientMapper clientMapper;

    @Test
    void shouldReturnPublishedClients() throws Exception {
        Client client = new Client(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);
        ClientDTO dto = new ClientDTO(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5);

        when(clientService.getPublishedClients()).thenReturn(List.of(client));
        when(clientMapper.toDTO(client)).thenReturn(dto);

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bar La Plaza"))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].published").doesNotExist());
    }
}