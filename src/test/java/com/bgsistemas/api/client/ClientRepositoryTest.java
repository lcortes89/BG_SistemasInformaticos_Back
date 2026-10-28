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

    // BGSI-302: admin list with search box (name or type of business) + active (= published)

    @Test
    void shouldSearchClientsByNameOrBusinessTypeIgnoringCase() {
        clientRepository.save(new Client(null, "Bar La Plaza", "Bar", "Great", "logo.png", 5, true));
        clientRepository.save(new Client(null, "Hotel Mar", "Hotel", "Great", "logo.png", 5, true));
        clientRepository.save(new Client(null, "Café Central", "Cafeteria", "Great", "logo.png", 4, false));

        assertThat(names(clientRepository.search("%plaza%", null, PageRequest.of(0, 10)))).containsExactly("Bar La Plaza");
        assertThat(names(clientRepository.search("%hotel%", null, PageRequest.of(0, 10)))).containsExactly("Hotel Mar");
        assertThat(clientRepository.search(null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void shouldFilterClientsByActive() {
        clientRepository.save(new Client(null, "Bar La Plaza", "Bar", "Great", "logo.png", 5, true));
        clientRepository.save(new Client(null, "Bar Draft", "Bar", "Draft", "logo.png", 3, false));

        assertThat(names(clientRepository.search("%bar%", true, PageRequest.of(0, 10)))).containsExactly("Bar La Plaza");
        assertThat(names(clientRepository.search("%bar%", false, PageRequest.of(0, 10)))).containsExactly("Bar Draft");
    }

    private List<String> names(Page<Client> page) {
        return page.getContent().stream().map(Client::getName).toList();
    }
}
