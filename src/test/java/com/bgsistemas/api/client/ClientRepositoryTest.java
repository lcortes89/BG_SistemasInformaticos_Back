package com.bgsistemas.api.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
        clientRepository.save(new Client(null, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true));
        clientRepository.save(new Client(null, "Café Central", "Cafeteria", "Pending review", "central.png", 4, false));

        List<Client> result = clientRepository.findByPublishedTrue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bar La Plaza");
    }

    @Test
    void shouldReturnPublishedClientsPageByPage() {
        for (int i = 1; i <= 3; i++) {
            clientRepository.save(new Client(null, "Client " + i, "Bar", "Testimonial", "logo.png", 5, true));
        }
        clientRepository.save(new Client(null, "Draft", "Bar", "Not published", "logo.png", 3, false));

        Page<Client> firstPage = clientRepository.findByPublishedTrue(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}
