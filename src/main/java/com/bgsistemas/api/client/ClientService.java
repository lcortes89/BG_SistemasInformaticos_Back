package com.bgsistemas.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    // Public website: only published clients
    Page<Client> getPublishedClients(Pageable pageable);

    // Admin panel
    Page<Client> getClientsPage(Pageable pageable);

    Client getClientById(Long id);

    Client createClient(Client client);

    Client updateClient(Long id, Client changes);

    void deleteClient(Long id);
}
