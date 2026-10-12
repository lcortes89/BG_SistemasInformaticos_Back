package com.bgsistemas.api.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void shouldSaveAndRetrieveClient() {
        Client client = new Client(null, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);

        Client saved = clientRepository.save(client);

        assertThat(saved.getId()).isNotNull();
        assertThat(clientRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldFindOnlyPublishedClients() {
        Client published = new Client(null, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);
        Client draft = new Client(null, "Café Central", "Cafeteria", "Pending review", "central.png", 4, false);
        clientRepository.save(published);
        clientRepository.save(draft);

        List<Client> result = clientRepository.findByPublishedTrue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bar La Plaza");
    }
}