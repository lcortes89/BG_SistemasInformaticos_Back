package com.bgsistemas.api.service.mappers;

import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.dtos.AdminServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-205: the full product sheet travels from the form to the entity and back to the JSON
class ServiceSheetMapperTest {

    private static final String OFFICIAL_URL = "https://www.bdp-software.com/bdp-one";

    private final ServiceMapper mapper = new ServiceMapper();

    private Service bdpOne() {
        Service service = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        service.setLongDescription("TPV for Windows and Android.");
        service.setFeatures(List.of("Orders at the table", "Kitchen printers"));
        service.setSectors(List.of("Bars", "Restaurants"));
        service.setOfficialUrl(OFFICIAL_URL);
        return service;
    }

    @Test
    void shouldIncludeTheSheetInThePublicDTO() {
        ServiceDTO dto = mapper.toDTO(bdpOne());

        assertThat(dto.getLongDescription()).isEqualTo("TPV for Windows and Android.");
        assertThat(dto.getFeatures()).containsExactly("Orders at the table", "Kitchen printers");
        assertThat(dto.getSectors()).containsExactly("Bars", "Restaurants");
        assertThat(dto.getOfficialUrl()).isEqualTo(OFFICIAL_URL);
    }

    @Test
    void shouldIncludeTheSheetInTheAdminDTO() {
        AdminServiceDTO dto = mapper.toAdminDTO(bdpOne());

        assertThat(dto.longDescription()).isEqualTo("TPV for Windows and Android.");
        assertThat(dto.features()).hasSize(2);
        assertThat(dto.sectors()).containsExactly("Bars", "Restaurants");
        assertThat(dto.officialUrl()).isEqualTo(OFFICIAL_URL);
    }

    @Test
    void shouldCleanTheListsOfTheForm() {
        ServiceRequestDTO request = new ServiceRequestDTO("BDP One", null, null, false, 5L, "  ",
                Arrays.asList("  Orders  ", "", null, "Line\nbreak"), null, "   ", null);

        Service service = mapper.toEntity(request);

        assertThat(service.getLongDescription()).isNull();
        assertThat(service.getFeatures()).containsExactly("Orders", "Line break");
        assertThat(service.getSectors()).isEmpty();
        assertThat(service.getOfficialUrl()).isNull();
    }

    @Test
    void shouldTrimTheOfficialUrl() {
        ServiceRequestDTO request = new ServiceRequestDTO("BDP One", null, null, false, 5L, null,
                null, null, "  " + OFFICIAL_URL + "  ", null);

        Service service = mapper.toEntity(request);

        assertThat(service.getOfficialUrl()).isEqualTo(OFFICIAL_URL);
    }
}
