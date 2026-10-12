package com.bgsistemas.api.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void shouldReturnOnlyPublishedClients() {
        Client client = new Client(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);
        when(clientRepository.findByPublishedTrue()).thenReturn(List.of(client));

        List<Client> result = clientService.getPublishedClients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublished()).isTrue();
    }
}