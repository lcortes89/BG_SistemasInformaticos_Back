package com.bgsistemas.api.summary;

import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.contact.ContactRepository;
import com.bgsistemas.api.contact.ContactStatus;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import com.bgsistemas.api.service.ServiceRepository;
import com.bgsistemas.api.summary.dtos.SummaryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ClientRepository clientRepository;

    // The real mapper: it has no dependencies
    private final ContactMapper contactMapper = new ContactMapper();

    private SummaryServiceImpl summaryService() {
        return new SummaryServiceImpl(contactRepository, brandRepository, serviceRepository,
                clientRepository, contactMapper);
    }

    @Test
    void shouldGatherAllTheCounters() {
        when(contactRepository.countByStatus(ContactStatus.NEW)).thenReturn(3L);
        when(contactRepository.count()).thenReturn(12L);
        when(brandRepository.countByActiveTrue()).thenReturn(4L);
        when(brandRepository.count()).thenReturn(5L);
        when(serviceRepository.countByFeaturedTrue()).thenReturn(3L);
        when(serviceRepository.count()).thenReturn(10L);
        when(clientRepository.countByPublishedTrue()).thenReturn(2L);
        when(clientRepository.count()).thenReturn(3L);
        when(contactRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(
                new Contact("Ana López", "ana@mail.com", null, "BDP One", "Demo please", true)));

        SummaryDTO summary = summaryService().getSummary();

        assertThat(summary.newContacts()).isEqualTo(3);
        assertThat(summary.totalContacts()).isEqualTo(12);
        assertThat(summary.activeBrands()).isEqualTo(4);
        assertThat(summary.totalBrands()).isEqualTo(5);
        assertThat(summary.featuredServices()).isEqualTo(3);
        assertThat(summary.totalServices()).isEqualTo(10);
        assertThat(summary.publishedClients()).isEqualTo(2);
        assertThat(summary.totalClients()).isEqualTo(3);
        assertThat(summary.latestContacts()).hasSize(1);
        assertThat(summary.latestContacts().get(0).name()).isEqualTo("Ana López");
    }

    // BGSI-141: an empty database gives zeros and an empty list, never null
    @Test
    void shouldReturnZerosAndEmptyListWhenThereIsNoData() {
        when(contactRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());

        SummaryDTO summary = summaryService().getSummary();

        assertThat(summary.newContacts()).isZero();
        assertThat(summary.totalBrands()).isZero();
        assertThat(summary.totalClients()).isZero();
        assertThat(summary.latestContacts()).isNotNull().isEmpty();
    }
}
