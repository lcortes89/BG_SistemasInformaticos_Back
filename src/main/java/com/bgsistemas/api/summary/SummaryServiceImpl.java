package com.bgsistemas.api.summary;

import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.contact.ContactRepository;
import com.bgsistemas.api.contact.ContactStatus;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import com.bgsistemas.api.service.ServiceRepository;
import com.bgsistemas.api.summary.dtos.SummaryDTO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SummaryServiceImpl implements SummaryService {

    private final ContactRepository contactRepository;
    private final BrandRepository brandRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final ContactMapper contactMapper;

    public SummaryServiceImpl(ContactRepository contactRepository, BrandRepository brandRepository,
                              ServiceRepository serviceRepository, ClientRepository clientRepository,
                              ContactMapper contactMapper) {
        this.contactRepository = contactRepository;
        this.brandRepository = brandRepository;
        this.serviceRepository = serviceRepository;
        this.clientRepository = clientRepository;
        this.contactMapper = contactMapper;
    }

    // Every number comes from a COUNT query: the rows are never loaded just to count them
    @Override
    @Transactional(readOnly = true) // BGSI-326: only reads
    public SummaryDTO getSummary() {
        return new SummaryDTO(
                contactRepository.countByStatus(ContactStatus.NEW),
                contactRepository.count(),
                brandRepository.countByActiveTrue(),
                brandRepository.count(),
                serviceRepository.countByFeaturedTrue(),
                serviceRepository.count(),
                clientRepository.countByPublishedTrue(),
                clientRepository.count(),
                contactRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(contactMapper::toResponse)
                        .toList()
        );
    }
}
