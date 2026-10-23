package com.bgsistemas.api.client;

import com.bgsistemas.api.client.exceptions.ClientNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private final PageRequest pageRequest = PageRequest.of(0, 6);

    private Client laPlaza() {
        return new Client(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);
    }

    @Test
    void shouldReturnPublishedClientsPage() {
        when(clientRepository.findByPublishedTrue(pageRequest))
                .thenReturn(new PageImpl<>(List.of(laPlaza()), pageRequest, 1));

        Page<Client> result = clientService.getPublishedClients(pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }

    @Test
    void shouldReturnAllClientsPageForTheAdmin() {
        Client draft = new Client(2L, "Café Central", "Cafeteria", "Pending", null, 4, false);
        when(clientRepository.findAll(pageRequest))
                .thenReturn(new PageImpl<>(List.of(laPlaza(), draft), pageRequest, 2));

        Page<Client> result = clientService.getClientsPage(pageRequest);

        assertThat(result.getContent()).extracting(Client::isPublished).containsExactly(true, false);
    }

    @Test
    void shouldCreateClientAsNewRow() {
        Client newClient = new Client(50L, "Hotel Mar", "Hotel", "Very good", null, 4, false);
        when(clientRepository.save(newClient)).thenReturn(newClient);

        clientService.createClient(newClient);

        assertThat(newClient.getId()).isNull();
        verify(clientRepository).save(newClient);
    }

    @Test
    void shouldUpdateAllFieldsOfTheClient() {
        Client existing = laPlaza();
        Client changes = new Client(null, "Bar La Plaza Nueva", "Restaurant", "Even better", null, 4, false);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        Client result = clientService.updateClient(1L, changes);

        assertThat(result.getName()).isEqualTo("Bar La Plaza Nueva");
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getLogoUrl()).isNull();
        assertThat(result.isPublished()).isFalse();
    }

    @Test
    void shouldThrowWhenClientDoesNotExist() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(99L, laPlaza()))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldDeleteClient() {
        Client client = laPlaza();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        clientService.deleteClient(1L);

        verify(clientRepository).delete(client);
    }

    // BGSI-309: active = published on the website
    @Test
    void shouldPublishClient() {
        Client client = laPlaza();
        client.setPublished(false);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        Client result = clientService.setClientPublished(1L, true);

        assertThat(result.isPublished()).isTrue();
        verify(clientRepository).save(client);
    }

    @Test
    void shouldThrowWhenChangingTheStateOfAnUnknownClient() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.setClientPublished(99L, false))
                .isInstanceOf(ClientNotFoundException.class);
    }
}
