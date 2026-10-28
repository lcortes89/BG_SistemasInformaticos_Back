package com.bgsistemas.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    // Public website: only published clients
    Page<Client> getPublishedClients(Pageable pageable);

    // Admin panel
    // BGSI-302: search = name or type of business, active = published. null = no filter
    Page<Client> getClientsPage(String search, Boolean active, Pageable pageable);

    Client getClientById(Long id);

    Client createClient(Client client);

    Client updateClient(Long id, Client changes);

    void deleteClient(Long id);

    // BGSI-309: active = published on the website
    Client setClientPublished(Long id, boolean published);
}
