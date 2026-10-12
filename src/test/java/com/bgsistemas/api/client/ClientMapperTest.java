package com.bgsistemas.api.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {

    @Test
    void shouldMapClientToClientDTO() {
        Client client = new Client(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, true);

        ClientDTO dto = new ClientMapper().toDTO(client);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Bar La Plaza");
        assertThat(dto.getBusinessType()).isEqualTo("Bar");
        assertThat(dto.getTestimonial()).isEqualTo("Great service");
        assertThat(dto.getLogoUrl()).isEqualTo("laplaza.png");
        assertThat(dto.getRating()).isEqualTo(5);
    }
}