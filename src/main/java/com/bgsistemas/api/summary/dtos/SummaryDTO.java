package com.bgsistemas.api.summary.dtos;

import com.bgsistemas.api.contact.dtos.ContactResponseDTO;

import java.util.List;

// Everything the "Resumen" page of the dashboard shows, in one call
public record SummaryDTO(
        long newContacts,
        long totalContacts,
        long activeBrands,
        long totalBrands,
        long featuredServices,
        long totalServices,
        long publishedClients,
        long totalClients,
        List<ContactResponseDTO> latestContacts
) {
}
