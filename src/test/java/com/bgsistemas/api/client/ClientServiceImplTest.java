package com.bgsistemas.api.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
    void shouldReturnPublishedClientsPage() {
        Client client = new Client(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);
        PageRequest pageRequest = PageRequest.of(0, 6);
        when(clientRepository.findByPublishedTrue(pageRequest))
                .thenReturn(new PageImpl<>(List.of(client), pageRequest, 1));

        Page<Client> result = clientService.getPublishedClients(pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }
}
