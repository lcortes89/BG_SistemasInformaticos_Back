package com.bgsistemas.api.client;

import com.bgsistemas.api.globals.BulkResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    // BGSI-333: bulk actions (the missing ids are reported, not an error)
    BulkResult deleteClients(List<Long> ids);

    BulkResult setClientsPublished(List<Long> ids, boolean published);
}
