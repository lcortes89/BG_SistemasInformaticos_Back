package com.bgsistemas.api.service.mappers;

import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.dtos.ServiceRequestDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-309: the "active" state of a product in the admin JSON and in the form
class ServiceActiveMapperTest {

    private final ServiceMapper mapper = new ServiceMapper();

    private ServiceRequestDTO request(Boolean active) {
        return new ServiceRequestDTO("BDP One", null, null, false, 5L, null, null, null, null, active);
    }

    @Test
    void aNewServiceIsActiveByDefault() {
        assertThat(new Service().isActive()).isTrue();
    }

    @Test
    void whenTheFormDoesNotSendTheStateTheServiceIsActive() {
        assertThat(mapper.toEntity(request(null)).isActive()).isTrue();
    }

    @Test
    void theFormCanSaveAnInactiveService() {
        assertThat(mapper.toEntity(request(false)).isActive()).isFalse();
        assertThat(mapper.toEntity(request(true)).isActive()).isTrue();
    }

    @Test
    void theAdminJsonHasTheState() {
        Service service = new Service(1L, "BDP One", null, null);
        service.setActive(false);

        assertThat(mapper.toAdminDTO(service).active()).isFalse();
    }
}
